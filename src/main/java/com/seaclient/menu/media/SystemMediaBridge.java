package com.seaclient.menu.media;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Base64;

/**
 * Low-CPU Windows GlobalSystemMediaTransportControlsSessionManager (GSMTC) bridge.
 * - Pre-compiles WinRT reflection delegates once at startup (zero per-tick reflection)
 * - Polls every 2.5s and automatically terminates when parent JVM exits (no orphan processes)
 * - Uses LastUpdatedTime + monotonic client-side interpolation so the progress bar
 *   moves 100% smoothly without bouncing or jumping backward.
 */
public final class SystemMediaBridge {

    public static final class MediaSnapshot {
        public final String title;
        public final String artist;
        public final boolean playing;
        public final double positionSeconds;
        public final double durationSeconds;
        public final byte[] thumbnailPngBytes;
        public final int thumbnailVersion;

        public MediaSnapshot(
                String title,
                String artist,
                boolean playing,
                double positionSeconds,
                double durationSeconds,
                byte[] thumbnailPngBytes,
                int thumbnailVersion
        ) {
            this.title = title;
            this.artist = artist;
            this.playing = playing;
            this.positionSeconds = positionSeconds;
            this.durationSeconds = durationSeconds;
            this.thumbnailPngBytes = thumbnailPngBytes;
            this.thumbnailVersion = thumbnailVersion;
        }

        public float progressFraction() {
            if (durationSeconds <= 0.5) {
                return 0.0f;
            }
            double ratio = positionSeconds / durationSeconds;
            if (ratio < 0.0) return 0.0f;
            if (ratio > 1.0) return 1.0f;
            return (float) ratio;
        }
    }

    private static final SystemMediaBridge INSTANCE = new SystemMediaBridge();

    public static SystemMediaBridge getInstance() {
        return INSTANCE;
    }

    private volatile String title = "No Media Playing";
    private volatile String artist = "System Audio";
    private volatile boolean playing = false;
    private volatile double basePositionSec = 0.0;
    private volatile double durationSec = 0.0;
    private volatile long lastPositionUpdateNanos = System.nanoTime();
    private volatile byte[] thumbnailPngBytes = null;
    private volatile int thumbnailVersion = 0;
    private volatile boolean started = false;
    private volatile Path commandFilePath;
    private volatile Process activeProcess;

    private SystemMediaBridge() {
    }

    public synchronized void ensureStarted() {
        if (started) {
            return;
        }
        started = true;
        String os = System.getProperty("os.name", "").toLowerCase();
        if (!os.contains("win")) {
            return;
        }
        try {
            commandFilePath = Files.createTempFile("seaclient-media-cmd-", ".txt");
            Files.deleteIfExists(commandFilePath);
            commandFilePath.toFile().deleteOnExit();
        } catch (Exception ignored) {
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Process proc = this.activeProcess;
            if (proc != null) {
                proc.destroyForcibly();
            }
            try {
                if (commandFilePath != null) {
                    Files.deleteIfExists(commandFilePath);
                }
            } catch (Exception ignored) {
            }
        }, "SeaClient-MediaBridge-Shutdown"));

        Thread worker = new Thread(this::runWorkerLoop, "SeaClient-MediaBridge");
        worker.setDaemon(true);
        worker.setPriority(Thread.MIN_PRIORITY + 1);
        worker.start();
    }

    private double getCurrentInterpolatedPosition() {
        double pos = basePositionSec;
        if (playing && durationSec > 0.5) {
            double elapsed = (System.nanoTime() - lastPositionUpdateNanos) / 1_000_000_000.0;
            pos = Math.min(durationSec, basePositionSec + Math.max(0.0, elapsed));
        }
        return pos;
    }

    public MediaSnapshot getSnapshot() {
        ensureStarted();
        return new MediaSnapshot(
                title,
                artist,
                playing,
                getCurrentInterpolatedPosition(),
                durationSec,
                thumbnailPngBytes,
                thumbnailVersion
        );
    }

    public void previousTrack() {
        this.basePositionSec = 0.0;
        this.lastPositionUpdateNanos = System.nanoTime();
        sendCommand("PREV");
    }

    public void togglePlayPause() {
        this.basePositionSec = getCurrentInterpolatedPosition();
        this.playing = !this.playing;
        this.lastPositionUpdateNanos = System.nanoTime();
        sendCommand("PLAY_PAUSE");
    }

    public void nextTrack() {
        this.basePositionSec = 0.0;
        this.lastPositionUpdateNanos = System.nanoTime();
        sendCommand("NEXT");
    }

    private void sendCommand(String cmd) {
        try {
            if (commandFilePath != null) {
                Files.writeString(
                        commandFilePath,
                        cmd,
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE
                );
            }
        } catch (Exception ignored) {
        }
    }

    private void runWorkerLoop() {
        String cmdPathStr = (commandFilePath != null)
                ? commandFilePath.toAbsolutePath().toString().replace("'", "''")
                : "";
        long parentPid = ProcessHandle.current().pid();
        String psScript = buildPowerShellScript(cmdPathStr, parentPid);
        String encodedCommand = Base64.getEncoder().encodeToString(psScript.getBytes(StandardCharsets.UTF_16LE));

        while (true) {
            Process process = null;
            try {
                ProcessBuilder pb = new ProcessBuilder(
                        "powershell.exe",
                        "-NoProfile",
                        "-NoLogo",
                        "-NonInteractive",
                        "-ExecutionPolicy", "Bypass",
                        "-EncodedCommand", encodedCommand
                );
                pb.redirectErrorStream(true);
                process = pb.start();
                this.activeProcess = process;

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        parseMediaLine(line);
                    }
                }
            } catch (Exception ignored) {
            } finally {
                this.activeProcess = null;
                if (process != null) {
                    process.destroyForcibly();
                }
            }

            try {
                Thread.sleep(4000L);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private void parseMediaLine(String line) {
        int idx = line.indexOf("SEAMEDIA\t");
        if (idx < 0) {
            return;
        }
        String cleanLine = line.substring(idx);
        String[] parts = cleanLine.split("\t", -1);
        if (parts.length < 7) {
            return;
        }
        String rawTitle = parts[1].trim();
        String rawArtist = parts[2].trim();
        boolean isPlaying = "1".equals(parts[3]);
        double reportedPos = parseDoubleSafe(parts[4]);
        double reportedDur = parseDoubleSafe(parts[5]);
        String thumbB64 = parts[6].trim();

        if (rawTitle.isEmpty()) {
            this.title = "No Media Playing";
            this.artist = "Play music on PC";
            this.playing = false;
            this.basePositionSec = 0.0;
            this.durationSec = 0.0;
            this.lastPositionUpdateNanos = System.nanoTime();
            return;
        }

        String resolvedArtist = rawArtist.isEmpty() ? "Unknown Artist" : rawArtist;
        boolean sameTrack = rawTitle.equals(this.title) && resolvedArtist.equals(this.artist);
        double currentInterp = getCurrentInterpolatedPosition();

        this.title = rawTitle;
        this.artist = resolvedArtist;
        if (reportedDur > 0.5) {
            this.durationSec = reportedDur;
        }

        if (!sameTrack) {
            // New song started: snap position cleanly
            this.basePositionSec = Math.max(0.0, reportedPos);
            this.lastPositionUpdateNanos = System.nanoTime();
        } else if (this.playing != isPlaying) {
            // Play/pause state changed: keep current interpolated position unless reportedPos is far off
            if (Math.abs(reportedPos - currentInterp) > 4.0) {
                this.basePositionSec = Math.max(0.0, reportedPos);
            } else {
                this.basePositionSec = currentInterp;
            }
            this.lastPositionUpdateNanos = System.nanoTime();
        } else if (isPlaying) {
            // Same song still playing: NEVER jump backward due to stale GSMTC timeline snapshots!
            // Only resync if user explicitly scrubbed/seeked (> 4.5s difference).
            double diff = reportedPos - currentInterp;
            if (Math.abs(diff) > 4.5) {
                this.basePositionSec = Math.clamp(reportedPos, 0.0, Math.max(1.0, this.durationSec));
                this.lastPositionUpdateNanos = System.nanoTime();
            } else if (diff > 0.8 && diff <= 4.5) {
                // Gently advance if slightly behind real clock (never goes backward)
                this.basePositionSec = currentInterp + diff * 0.35;
                this.lastPositionUpdateNanos = System.nanoTime();
            }
        } else {
            // Paused on same track
            if (Math.abs(reportedPos - currentInterp) > 3.0) {
                this.basePositionSec = Math.max(0.0, reportedPos);
                this.lastPositionUpdateNanos = System.nanoTime();
            }
        }

        this.playing = isPlaying;

        if (!thumbB64.isEmpty() && !"KEEP".equals(thumbB64) && !"NONE".equals(thumbB64)) {
            try {
                byte[] decoded = Base64.getDecoder().decode(thumbB64);
                if (decoded.length > 0) {
                    this.thumbnailPngBytes = decoded;
                    this.thumbnailVersion++;
                }
            } catch (Exception ignored) {
            }
        } else if ("NONE".equals(thumbB64)) {
            if (this.thumbnailPngBytes != null) {
                this.thumbnailPngBytes = null;
                this.thumbnailVersion++;
            }
        }
    }

    private static double parseDoubleSafe(String s) {
        try {
            return Double.parseDouble(s.replace(',', '.'));
        } catch (Exception e) {
            return 0.0;
        }
    }

    private static String buildPowerShellScript(String cmdFilePath, long parentPid) {
        return """
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
Add-Type -AssemblyName System.Runtime.WindowsRuntime, System.Drawing

$code = @"
using System;
using System.Runtime.InteropServices;
public static class MediaKeys {
    [DllImport("user32.dll")]
    public static extern void keybd_event(byte bVk, byte bScan, uint dwFlags, UIntPtr dwExtraInfo);
    public static void Send(byte vk) {
        keybd_event(vk, 0, 0, UIntPtr.Zero);
        keybd_event(vk, 0, 2, UIntPtr.Zero);
    }
}
"@
Add-Type -TypeDefinition $code -ErrorAction SilentlyContinue

[Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager,Windows.Media.Control,ContentType=WindowsRuntime] | Out-Null
[Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties,Windows.Media.Control,ContentType=WindowsRuntime] | Out-Null
[Windows.Storage.Streams.IRandomAccessStreamWithContentType,Windows.Storage.Streams,ContentType=WindowsRuntime] | Out-Null
[Windows.Storage.Streams.IInputStream,Windows.Storage.Streams,ContentType=WindowsRuntime] | Out-Null

$asTaskGeneric = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object {
    $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1'
})[0]

# Pre-construct generic MethodInfos ONCE so polling uses zero reflection CPU
$asTaskMgr = $asTaskGeneric.MakeGenericMethod([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])
$asTaskProps = $asTaskGeneric.MakeGenericMethod([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])
$asTaskStream = $asTaskGeneric.MakeGenericMethod([Windows.Storage.Streams.IRandomAccessStreamWithContentType])
$asTaskBool = $asTaskGeneric.MakeGenericMethod([bool])

$asStreamMethod = ([System.IO.WindowsRuntimeStreamExtensions].GetMethods() | Where-Object {
    $_.Name -eq 'AsStreamForRead' -and $_.GetParameters().Count -eq 1
})[0]

Function AwaitFast($WinRtTask, $Method) {
    if ($null -eq $WinRtTask) { return $null }
    $netTask = $Method.Invoke($null, @($WinRtTask))
    $netTask.Wait(1200) | Out-Null
    if ($netTask.IsCompleted) { return $netTask.Result }
    return $null
}

Function Get-BestSession($m) {
    if ($null -eq $m) { return $null }
    try {
        $all = $m.GetSessions()
        if ($null -ne $all) {
            foreach ($cand in $all) {
                $pInfo = $cand.GetPlaybackInfo()
                if ($null -ne $pInfo -and "$($pInfo.PlaybackStatus)" -eq "Playing") {
                    return $cand
                }
            }
        }
        $cur = $m.GetCurrentSession()
        if ($null -ne $cur) { return $cur }
        if ($null -ne $all -and $all.Count -gt 0) { return $all[0] }
    } catch {}
    return $null
}

$mgr = AwaitFast ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) $asTaskMgr
$lastKey = ""
$cachedTitle = ""
$cachedArtist = ""
$cmdFile = '%s'
$parentPid = %d
$tick = 6

while ($true) {
    try {
        if (-not [System.Diagnostics.Process]::GetProcessById($parentPid)) { exit }
    } catch {
        exit
    }

    try {
        if ($cmdFile -ne '' -and [System.IO.File]::Exists($cmdFile)) {
            $cmd = [System.IO.File]::ReadAllText($cmdFile).Trim()
            [System.IO.File]::Delete($cmdFile)
            $s = Get-BestSession $mgr
            if ($cmd -eq "PLAY_PAUSE") {
                if ($null -ne $s) { AwaitFast ($s.TryTogglePlayPauseAsync()) $asTaskBool | Out-Null }
                else { [MediaKeys]::Send(0xB3) }
            } elseif ($cmd -eq "NEXT") {
                if ($null -ne $s) { AwaitFast ($s.TrySkipNextAsync()) $asTaskBool | Out-Null }
                else { [MediaKeys]::Send(0xB0) }
                $lastKey = ""
            } elseif ($cmd -eq "PREV") {
                if ($null -ne $s) { AwaitFast ($s.TrySkipPreviousAsync()) $asTaskBool | Out-Null }
                else { [MediaKeys]::Send(0xB1) }
                $lastKey = ""
            }
            $tick = 6
        }

        if ($tick -ge 6) {
            $tick = 0
            $session = Get-BestSession $mgr
            if ($null -ne $session) {
                $props = AwaitFast ($session.TryGetMediaPropertiesAsync()) $asTaskProps
                $tl = $session.GetTimelineProperties()
                $pb = $session.GetPlaybackInfo()

                $title = if ($null -ne $props -and $null -ne $props.Title) { $props.Title.Replace("`t", " ").Replace("`r", "").Replace("`n", " ") } else { $cachedTitle }
                $artist = if ($null -ne $props -and $null -ne $props.Artist) { $props.Artist.Replace("`t", " ").Replace("`r", "").Replace("`n", " ") } else { $cachedArtist }
                $cachedTitle = $title
                $cachedArtist = $artist

                $isPlaying = if ($null -ne $pb -and "$($pb.PlaybackStatus)" -eq "Playing") { "1" } else { "0" }
                $pos = 0
                $dur = 0
                if ($null -ne $tl) {
                    $dur = [math]::Max(0, $tl.EndTime.TotalSeconds)
                    $pos = [math]::Max(0, $tl.Position.TotalSeconds)
                    if ($isPlaying -eq "1" -and $tl.LastUpdatedTime.Year -gt 2000) {
                        $since = ([DateTimeOffset]::UtcNow - $tl.LastUpdatedTime).TotalSeconds
                        if ($since -gt 0 -and $since -lt 600) {
                            $pos = if ($dur -gt 0) { [math]::Min($dur, $pos + $since) } else { $pos + $since }
                        }
                    }
                }

                $curKey = "$title|$artist"
                $thumbOut = "KEEP"
                if ($curKey -ne $lastKey) {
                    $lastKey = $curKey
                    $thumbOut = "NONE"
                    if ($null -ne $props -and $null -ne $props.Thumbnail) {
                        try {
                            $stream = AwaitFast ($props.Thumbnail.OpenReadAsync()) $asTaskStream
                            if ($null -ne $stream) {
                                $netStream = $asStreamMethod.Invoke($null, @($stream))
                                $bmp = New-Object System.Drawing.Bitmap($netStream)
                                $thumbBmp = New-Object System.Drawing.Bitmap(96, 96)
                                $g = [System.Drawing.Graphics]::FromImage($thumbBmp)
                                $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
                                $g.DrawImage($bmp, 0, 0, 96, 96)
                                $g.Dispose()
                                $msOut = New-Object System.IO.MemoryStream
                                $thumbBmp.Save($msOut, [System.Drawing.Imaging.ImageFormat]::Png)
                                $thumbOut = [Convert]::ToBase64String($msOut.ToArray())
                                $thumbBmp.Dispose()
                                $bmp.Dispose()
                                $netStream.Dispose()
                            }
                        } catch {
                            $thumbOut = "NONE"
                        }
                    }
                }
                $posStr = $pos.ToString([System.Globalization.CultureInfo]::InvariantCulture)
                $durStr = $dur.ToString([System.Globalization.CultureInfo]::InvariantCulture)
                [Console]::Out.WriteLine("SEAMEDIA`t$title`t$artist`t$isPlaying`t$posStr`t$durStr`t$thumbOut")
                [Console]::Out.Flush()
            } else {
                $lastKey = ""
                [Console]::Out.WriteLine("SEAMEDIA`t`t`t0`t0`t0`tNONE")
                [Console]::Out.Flush()
            }
        }
        $tick++
    } catch {
    }
    Start-Sleep -Milliseconds 400
}
""".formatted(cmdFilePath, parentPid);
    }
}
