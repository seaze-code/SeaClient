package com.seaclient.menu.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Tracks and persists ONLY the player's real most recently played Multiplayer Server
 * or Singleplayer World (never any hardcoded/fake server).
 */
public final class LastPlayedTracker {

    public static final class Entry {
        public String type; // "SERVER" or "WORLD"
        public String displayName;
        public String addressOrLevelId;
        public long timestampMs;
        public transient byte[] iconPngBytes;
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static volatile Entry cachedEntry = null;
    private static volatile int cachedIconVersion = 0;

    private LastPlayedTracker() {
    }

    private static File getConfigFile(Minecraft mc) {
        File configDir = new File(mc.gameDirectory, "config");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        return new File(configDir, "sea-client-recent.json");
    }

    public static Entry getRecentEntry(Minecraft mc) {
        return cachedEntry;
    }

    public static int getIconVersion() {
        return cachedIconVersion;
    }

    public static void refreshRealDataAsync(Minecraft mc) {
        Thread worker = new Thread(() -> {
            try {
                discoverRealData(mc);
            } catch (Exception ignored) {
            }
        }, "SeaClient-RecentTracker");
        worker.setDaemon(true);
        worker.start();
    }

    public static synchronized void recordServerJoin(Minecraft mc, String displayName, String ip, byte[] iconBytes) {
        if (ip == null || ip.isBlank()) {
            return;
        }
        Entry entry = new Entry();
        entry.type = "SERVER";
        entry.displayName = (displayName != null && !displayName.isBlank()) ? displayName : ip;
        entry.addressOrLevelId = ip;
        entry.timestampMs = System.currentTimeMillis();
        entry.iconPngBytes = iconBytes;
        cachedEntry = entry;
        cachedIconVersion++;
        save(mc, entry);
    }

    public static synchronized void recordWorldJoin(Minecraft mc, String levelName, String levelId) {
        if (levelId == null || levelId.isBlank()) {
            return;
        }
        Entry entry = new Entry();
        entry.type = "WORLD";
        entry.displayName = (levelName != null && !levelName.isBlank()) ? levelName : levelId;
        entry.addressOrLevelId = levelId;
        entry.timestampMs = System.currentTimeMillis();
        try {
            Path iconPath = mc.gameDirectory.toPath().resolve("saves").resolve(levelId).resolve("icon.png");
            if (Files.isRegularFile(iconPath)) {
                entry.iconPngBytes = Files.readAllBytes(iconPath);
            }
        } catch (Exception ignored) {
        }
        cachedEntry = entry;
        cachedIconVersion++;
        save(mc, entry);
    }

    public static String formatRelativeTime(long timestampMs) {
        if (timestampMs <= 0L) {
            return "Continue · recent";
        }
        long diffSec = Math.max(0L, (System.currentTimeMillis() - timestampMs) / 1000L);
        if (diffSec < 60L) {
            return "Continue · just now";
        }
        long mins = diffSec / 60L;
        if (mins < 60L) {
            return "Continue · " + mins + "m ago";
        }
        long hours = mins / 60L;
        if (hours < 24L) {
            return "Continue · " + hours + "h ago";
        }
        long days = hours / 24L;
        return "Continue · " + days + "d ago";
    }

    public static void connectOrOpen(Minecraft mc, Screen parentScreen) {
        Entry entry = cachedEntry;
        if (entry == null || entry.addressOrLevelId == null || entry.addressOrLevelId.isBlank()) {
            mc.gui.setScreen(new SelectWorldScreen(parentScreen));
            return;
        }
        if ("WORLD".equalsIgnoreCase(entry.type)) {
            try {
                entry.timestampMs = System.currentTimeMillis();
                save(mc, entry);
                mc.createWorldOpenFlows().openWorld(entry.addressOrLevelId, () -> mc.gui.setScreen(parentScreen));
                return;
            } catch (Exception e) {
                mc.gui.setScreen(new SelectWorldScreen(parentScreen));
                return;
            }
        }
        String ip = entry.addressOrLevelId;
        entry.timestampMs = System.currentTimeMillis();
        save(mc, entry);
        ServerAddress serverAddress = ServerAddress.parseString(ip);
        ServerData serverData = new ServerData(entry.displayName, ip, ServerData.Type.OTHER);
        ConnectScreen.startConnecting(parentScreen, mc, serverAddress, serverData, false, null);
    }

    private static synchronized void discoverRealData(Minecraft mc) {
        // 1. Check real Singleplayer worlds first to find the latest played world
        LevelSummary latestWorld = null;
        try {
            LevelStorageSource levelSource = mc.getLevelSource();
            LevelStorageSource.LevelCandidates candidates = levelSource.findLevelCandidates();
            List<LevelSummary> summaries = levelSource.loadLevelSummaries(candidates).join();
            if (summaries != null) {
                for (LevelSummary s : summaries) {
                    if (latestWorld == null || s.getLastPlayed() > latestWorld.getLastPlayed()) {
                        latestWorld = s;
                    }
                }
            }
        } catch (Exception ignored) {
        }

        // 2. Check saved recent entry from config file (only if it still exists in real worlds/servers)
        Entry savedEntry = null;
        File file = getConfigFile(mc);
        if (file.exists()) {
            try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
                Entry loaded = GSON.fromJson(reader, Entry.class);
                if (loaded != null && loaded.addressOrLevelId != null && !loaded.addressOrLevelId.isBlank()) {
                    if ("WORLD".equalsIgnoreCase(loaded.type)) {
                        Path worldFolder = mc.gameDirectory.toPath().resolve("saves").resolve(loaded.addressOrLevelId);
                        if (Files.isDirectory(worldFolder)) {
                            Path iconPath = worldFolder.resolve("icon.png");
                            if (Files.isRegularFile(iconPath)) {
                                loaded.iconPngBytes = Files.readAllBytes(iconPath);
                            }
                            savedEntry = loaded;
                        }
                    } else if ("SERVER".equalsIgnoreCase(loaded.type)) {
                        savedEntry = loaded;
                        attachServerIconIfFound(mc, savedEntry);
                    }
                }
            } catch (Exception ignored) {
            }
        }

        // If a world was played more recently than savedEntry, prefer the more recent world
        if (latestWorld != null && (savedEntry == null || latestWorld.getLastPlayed() > savedEntry.timestampMs)) {
            Entry worldEntry = new Entry();
            worldEntry.type = "WORLD";
            worldEntry.displayName = latestWorld.getLevelName();
            worldEntry.addressOrLevelId = latestWorld.getLevelId();
            worldEntry.timestampMs = latestWorld.getLastPlayed() > 0 ? latestWorld.getLastPlayed() : System.currentTimeMillis();
            try {
                Path iconPath = latestWorld.getIcon();
                if (iconPath != null && Files.isRegularFile(iconPath)) {
                    worldEntry.iconPngBytes = Files.readAllBytes(iconPath);
                }
            } catch (Exception ignored) {
            }
            cachedEntry = worldEntry;
            cachedIconVersion++;
            return;
        }

        if (savedEntry != null) {
            cachedEntry = savedEntry;
            cachedIconVersion++;
            return;
        }

        // 3. Check real ServerList (servers.dat) or mc.options.lastMpIp
        try {
            ServerList serverList = new ServerList(mc);
            serverList.load();
            if (serverList.size() > 0) {
                ServerData chosen = serverList.get(0);
                if (mc.options != null && mc.options.lastMpIp != null && !mc.options.lastMpIp.isBlank()) {
                    for (int i = 0; i < serverList.size(); i++) {
                        ServerData sd = serverList.get(i);
                        if (sd != null && mc.options.lastMpIp.equalsIgnoreCase(sd.ip)) {
                            chosen = sd;
                            break;
                        }
                    }
                }
                if (chosen != null && chosen.ip != null && !chosen.ip.isBlank()) {
                    Entry discovered = new Entry();
                    discovered.type = "SERVER";
                    discovered.displayName = (chosen.name != null && !chosen.name.isBlank() && !"Minecraft Server".equals(chosen.name))
                            ? chosen.name
                            : chosen.ip;
                    discovered.addressOrLevelId = chosen.ip;
                    discovered.timestampMs = System.currentTimeMillis();
                    discovered.iconPngBytes = chosen.getIconBytes();
                    cachedEntry = discovered;
                    cachedIconVersion++;
                    return;
                }
            } else if (mc.options != null && mc.options.lastMpIp != null && !mc.options.lastMpIp.isBlank()) {
                Entry discovered = new Entry();
                discovered.type = "SERVER";
                discovered.displayName = mc.options.lastMpIp;
                discovered.addressOrLevelId = mc.options.lastMpIp;
                discovered.timestampMs = System.currentTimeMillis();
                cachedEntry = discovered;
                cachedIconVersion++;
                return;
            }
        } catch (Exception ignored) {
        }

        // No real worlds or servers found yet
        cachedEntry = null;
        cachedIconVersion++;
    }

    private static void attachServerIconIfFound(Minecraft mc, Entry entry) {
        try {
            ServerList serverList = new ServerList(mc);
            serverList.load();
            for (int i = 0; i < serverList.size(); i++) {
                ServerData sd = serverList.get(i);
                if (sd != null && entry.addressOrLevelId.equalsIgnoreCase(sd.ip)) {
                    entry.iconPngBytes = sd.getIconBytes();
                    if (sd.name != null && !sd.name.isBlank() && !"Minecraft Server".equals(sd.name)) {
                        entry.displayName = sd.name;
                    }
                    break;
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void save(Minecraft mc, Entry entry) {
        try (FileWriter writer = new FileWriter(getConfigFile(mc), StandardCharsets.UTF_8)) {
            GSON.toJson(entry, writer);
        } catch (Exception ignored) {
        }
    }
}
