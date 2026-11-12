package com.seaclient.menu.optimize;

import com.seaclient.menu.hud.SeaClientPvpTracker;
import com.seaclient.menu.screen.SeaClientIcons;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ParticleStatus;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Core Optimization & Hypixel Bedwars Assistant Engine for Sea Client.
 * Manages all Bedwars PvP helpers, HUD modules, expandable sub-settings,
 * keybinds, live Minecraft option synchronization, and render/particle/sound hooks.
 */
public final class SeaClientOptimizer {

    public enum Category {
        HOME("Home", SeaClientIcons.HOME),
        BEDWARS("Bedwars", SeaClientIcons.SWORD),
        HUD("HUD", SeaClientIcons.SLIDERS),
        RENDER("Render", SeaClientIcons.EYE),
        PARTICLES("Particles", SeaClientIcons.BURST),
        WORLD("World", SeaClientIcons.GLOBE),
        SYSTEM("System", SeaClientIcons.CPU);

        public final String displayName;
        public final Identifier icon;

        Category(String displayName, Identifier icon) {
            this.displayName = displayName;
            this.icon = icon;
        }
    }

    public static final class SubOption {
        public final String id;
        public final String label;
        public final String[] values;
        public int index;

        public SubOption(String id, String label, int defaultIndex, String... values) {
            this.id = id;
            this.label = label;
            this.values = values;
            this.index = Math.clamp(defaultIndex, 0, Math.max(0, values.length - 1));
        }

        public String currentValue() {
            return values[index];
        }

        public void prev() {
            index = (index - 1 + values.length) % values.length;
            SeaClientOptimizer.getInstance().onSettingChanged();
        }

        public void next() {
            index = (index + 1) % values.length;
            SeaClientOptimizer.getInstance().onSettingChanged();
        }
    }

    public static final class OptModule {
        public final String id;
        public final Category category;
        public final String title;
        public final String description;
        public final Identifier icon;
        public final int badgeColor;
        public boolean enabled;
        public boolean expanded;
        public int keyCode = -1;
        public String keyName = "None";
        public final List<SubOption> subOptions = new ArrayList<>();

        public OptModule(
                String id,
                Category category,
                String title,
                String description,
                Identifier icon,
                int badgeColor,
                boolean defaultEnabled,
                boolean defaultExpanded
        ) {
            this.id = id;
            this.category = category;
            this.title = title;
            this.description = description;
            this.icon = icon;
            this.badgeColor = badgeColor;
            this.enabled = defaultEnabled;
            this.expanded = defaultExpanded;
        }

        public OptModule addSub(String id, String label, int defaultIdx, String... values) {
            this.subOptions.add(new SubOption(id, label, defaultIdx, values));
            return this;
        }

        public SubOption getSub(String subId) {
            for (SubOption s : subOptions) {
                if (s.id.equals(subId)) {
                    return s;
                }
            }
            return null;
        }

        public String getSubValue(String subId, String fallback) {
            SubOption s = getSub(subId);
            return s != null ? s.currentValue() : fallback;
        }
    }

    private static final SeaClientOptimizer INSTANCE = new SeaClientOptimizer();

    public static SeaClientOptimizer getInstance() {
        return INSTANCE;
    }

    private final List<OptModule> modules = new ArrayList<>();
    private final Path configPath;

    // Bedwars & PvP cached flags
    public volatile boolean bwResourcesEnabled = true;
    public volatile boolean bwResourcesHideZero = false;
    public volatile boolean bwShowShopHint = true;

    public volatile boolean bwUtilitiesEnabled = true;
    public volatile int bwLowBlockThreshold = 16;

    public volatile boolean bwBridgeAssistEnabled = true;
    public volatile boolean bwBridgeOnlyWhenHoldingBlocks = true;

    public volatile boolean targetHudEnabled = true;
    public volatile long targetHudTimeoutMs = 4000L;
    public volatile boolean targetHudShowStatus = true;

    public volatile boolean comboReachEnabled = true;
    public volatile boolean showReach = true;
    public volatile boolean showCombo = true;

    public volatile boolean toggleSprintEnabled = true;
    public volatile boolean showSprintHud = true;

    public volatile boolean potionHudEnabled = true;
    public volatile int potionLowWarnSec = 8;

    public volatile boolean autoGgEnabled = true;
    public volatile String autoGgMessage = "gg";
    public volatile boolean showBwTimer = true;

    public volatile boolean fullbrightEnabled = true;
    public volatile float fullbrightLevel = 15.0f;

    public volatile boolean customCrosshairEnabled = true;
    public volatile String crosshairStyle = "Cross + Dot";
    public volatile boolean crosshairHighlightTarget = true;

    // HUD cached flags
    public volatile boolean hudStatsEnabled = true;
    public volatile boolean hudShowFps = true;
    public volatile boolean hudShowPing = true;
    public volatile boolean hudShowCoords = true;
    public volatile boolean hudShowRam = false;

    public volatile boolean hudMusicEnabled = true;
    public volatile boolean hudMusicHideWhenIdle = false;

    public volatile boolean hudKeystrokesEnabled = true;
    public volatile boolean hudShowCps = true;

    public volatile boolean hudArmorEnabled = true;

    // Optimization cached flags
    public volatile boolean cleanViewEnabled = true;
    public volatile boolean hideFireOverlay = true;
    public volatile boolean hideTotemPopOverlay = false;
    public volatile boolean hideUnderwaterOverlay = true;
    public volatile boolean noHurtCam = true;

    public volatile boolean entityCullingEnabled = true;
    public volatile double maxEntityDistSq = 48.0 * 48.0;
    public volatile boolean skipArmorStands = false;
    public volatile boolean skipItemFrames = false;
    public volatile boolean skipPaintings = false;

    public volatile boolean muteExplosionsEnabled = true;
    public volatile float explosionVolumeMultiplier = 0.0f;
    public volatile boolean hideExplosionParticles = true;
    public volatile float totemVolumeMultiplier = 1.0f;

    public volatile boolean particleLimiterEnabled = true;
    public volatile boolean hideSmokeParticles = true;
    public volatile boolean hideBlockBreakParticles = false;
    public volatile boolean hideRainSplashParticles = true;
    public volatile boolean hidePortalParticles = true;

    public volatile boolean ambientSoundFilterEnabled = false;
    public volatile float weatherSoundMultiplier = 0.25f;
    public volatile float pistonSoundMultiplier = 0.5f;

    public volatile boolean dynamicFpsEnabled = true;
    public volatile int unfocusedFpsLimit = 30;
    public volatile int focusedFpsLimit = 260;

    public volatile boolean memoryOptEnabled = true;
    public volatile long autoGcIntervalMs = 300_000L;
    public volatile int ramThresholdPercent = 85;
    private long lastGcTimeMs = System.currentTimeMillis();
    private boolean lastWindowFocused = true;

    // Module references
    private final OptModule modBwResources;
    private final OptModule modBwUtilities;
    private final OptModule modBridgeAssist;
    private final OptModule modTargetHud;
    private final OptModule modComboReach;
    private final OptModule modToggleSprint;
    private final OptModule modPotionHud;
    private final OptModule modAutoGg;
    private final OptModule modFullbright;
    private final OptModule modCrosshair;

    private final OptModule modHudStats;
    private final OptModule modHudMusic;
    private final OptModule modHudKeystrokes;
    private final OptModule modHudArmor;

    private final OptModule modCleanView;
    private final OptModule modEntityCulling;
    private final OptModule modFastSky;
    private final OptModule modMuteExplosions;
    private final OptModule modParticleLimiter;
    private final OptModule modChunkBoost;
    private final OptModule modAmbientSound;
    private final OptModule modDynamicFps;
    private final OptModule modMemoryGc;
    private final OptModule modLowLatency;

    private SeaClientOptimizer() {
        this.configPath = FabricLoader.getInstance().getConfigDir().resolve("seaclient-optimize.properties");

        // ================= BEDWARS & HYPIXEL PVP MODULES =================
        modBwResources = new OptModule(
                "bw_resources", Category.BEDWARS,
                "Bedwars Resource Counter",
                "Live Iron, Gold, Diamond & Emerald counts + shop affordability",
                SeaClientIcons.SPARKLES, 0xFF4ADE80, true, true
        )
                .addSub("zero_mode", "Zero count items", 0, "Show all", "Hide zero")
                .addSub("shop_hint", "Shop afford hint", 0, "On", "Off");

        modBwUtilities = new OptModule(
                "bw_utilities", Category.BEDWARS,
                "Bedwars Blocks & Utilities",
                "Track Wool/Blocks, Golden Apples, Fireballs, TNT & Ender Pearls",
                SeaClientIcons.SWORD, 0xFFF59E0B, true, false
        )
                .addSub("low_blocks", "Low block warning", 0, "16 blocks", "32 blocks", "8 blocks", "Off");

        modBridgeAssist = new OptModule(
                "bw_bridge", Category.BEDWARS,
                "Speedbridge Angle Assist",
                "Show exact pitch/yaw angle & sweet spot indicator (75°-79.5°) for bridging",
                SeaClientIcons.BOLT, 0xFF38BDF8, true, false
        )
                .addSub("display_when", "Show indicator", 0, "Holding blocks", "Always on");

        modTargetHud = new OptModule(
                "bw_target_hud", Category.BEDWARS,
                "Target Info HUD",
                "Show enemy 2D head, HP bar, distance & WINNING/LOSING status",
                SeaClientIcons.USER, 0xFFF45B69, true, false
        )
                .addSub("timeout", "Keep visible after hit", 1, "2 Seconds", "4 Seconds", "6 Seconds")
                .addSub("win_status", "Show WINNING/LOSING", 0, "On", "Off");

        modComboReach = new OptModule(
                "bw_combo_reach", Category.BEDWARS,
                "Combo & Reach Counter",
                "Display real-time hit distance (blocks) & consecutive hit combo",
                SeaClientIcons.BURST, 0xFF8B7CFF, true, false
        )
                .addSub("show_reach", "Show hit reach", 0, "On", "Off")
                .addSub("show_combo", "Show combo counter", 0, "On", "Off");

        modToggleSprint = new OptModule(
                "bw_toggle_sprint", Category.BEDWARS,
                "Toggle Sprint",
                "Automatically sprint when moving forward without holding sprint key",
                SeaClientIcons.BOLT, 0xFF6EA8FF, true, false
        )
                .addSub("sprint_hud", "Show [Sprinting] badge", 0, "On", "Off");

        modPotionHud = new OptModule(
                "bw_potion_hud", Category.BEDWARS,
                "Potion & Invis Timer HUD",
                "Track Speed II, Jump Boost V & Invisibility duration in MM:SS",
                SeaClientIcons.EYE, 0xFFEC4899, true, false
        )
                .addSub("low_warn", "Flashing low alert", 1, "5 Seconds", "8 Seconds", "Off");

        modAutoGg = new OptModule(
                "bw_autogg", Category.BEDWARS,
                "Hypixel AutoGG & BW Timer",
                "Auto-send gg at match end + Diamond/Emerald upgrade countdown",
                SeaClientIcons.GLOBE, 0xFF67B7B2, true, false
        )
                .addSub("gg_msg", "AutoGG message", 0, "gg", "gf", "Good game!", "Off")
                .addSub("bw_timer", "Generator upgrade timer", 0, "On", "Off");

        // ================= HUD MODULES =================
        modHudStats = new OptModule(
                "hud_stats", Category.HUD,
                "Stats & Coords HUD",
                "Display live FPS, ping, XYZ coordinates & RAM usage on screen",
                SeaClientIcons.SPARKLES, 0xFF8EAFFF, true, true
        )
                .addSub("show_fps", "Show FPS", 0, "On", "Off")
                .addSub("show_ping", "Show ping (ms)", 0, "On", "Off")
                .addSub("show_coords", "Show XYZ coordinates", 0, "On", "Off")
                .addSub("show_ram", "Show RAM usage", 1, "On", "Off");

        modCrosshair = new OptModule(
                "hud_crosshair", Category.HUD,
                "Custom PvP Crosshair",
                "Sharp PvP crosshair that highlights red when aimed at an enemy",
                SeaClientIcons.SWORD, 0xFFF45B69, true, false
        )
                .addSub("style", "Crosshair style", 0, "Cross + Dot", "Precision Cross", "Dot Only", "Target Box", "Vanilla")
                .addSub("target_color", "Turn red on target", 0, "On", "Off");

        modHudMusic = new OptModule(
                "hud_music", Category.HUD,
                "Music Player HUD",
                "Show currently playing song, album art & progress bar in-game",
                SeaClientIcons.PLAY, 0xFF8B7CFF, true, false
        )
                .addSub("hide_idle", "When no music playing", 0, "Show idle", "Hide widget");

        modHudKeystrokes = new OptModule(
                "hud_keystrokes", Category.HUD,
                "Keystrokes & CPS",
                "Sharp WASD, Spacebar & LMB/RMB real-time CPS counter",
                SeaClientIcons.SLIDERS, 0xFF58C4DC, true, false
        )
                .addSub("show_cps", "Show LMB/RMB CPS", 0, "On", "Off");

        modHudArmor = new OptModule(
                "hud_armor", Category.HUD,
                "Armor Status HUD",
                "Show equipped armor pieces & durability bars next to hotbar",
                SeaClientIcons.SWORD, 0xFF4ADE80, true, false
        );

        // ================= RENDER / PARTICLES / WORLD / SYSTEM =================
        modFullbright = new OptModule(
                "fullbright", Category.RENDER,
                "Fullbright (Max Gamma)",
                "See clearly in dark caves, roofs & void maps without Night Vision",
                SeaClientIcons.SPARKLES, 0xFFFBBF24, true, false
        )
                .addSub("level", "Brightness level", 0, "100% (Max)", "75%", "50%");

        modCleanView = new OptModule(
                "clean_view", Category.RENDER,
                "Clean View",
                "Hide smoke, explosions, fog, fire & totem pop overlays",
                SeaClientIcons.REALMS, 0xFF6BA6FF, true, false
        )
                .addSub("fire_overlay", "Fire overlay", 0, "Hidden", "Visible")
                .addSub("totem_overlay", "Totem pop overlay", 1, "Hidden", "Visible")
                .addSub("water_overlay", "Underwater overlay", 0, "Hidden", "Visible")
                .addSub("hurt_cam", "Hurt camera shake", 0, "Off", "On");

        modMuteExplosions = new OptModule(
                "mute_explosions", Category.PARTICLES,
                "Mute Explosions",
                "Silence crystal, anchor, TNT & fireball explosion sounds",
                SeaClientIcons.VOLUME_MUTE, 0xFF8B7CFF, true, false
        )
                .addSub("exp_volume", "Explosion sound volume", 0, "Off", "25%", "50%", "75%", "100%")
                .addSub("exp_particles", "Explosion particles", 0, "Hidden", "Visible")
                .addSub("totem_volume", "Totem sound volume", 3, "Off", "25%", "50%", "100%");

        modParticleLimiter = new OptModule(
                "particle_limiter", Category.PARTICLES,
                "Particle Optimizer",
                "Filter laggy particles: smoke, campfire, rain, block dust...",
                SeaClientIcons.BURST, 0xFFF45B69, true, true
        )
                .addSub("preset", "Particle mode", 1, "Minimal", "Decreased", "All")
                .addSub("smoke", "Smoke & campfire", 0, "Off", "On")
                .addSub("block_dust", "Block break dust", 1, "Off", "On")
                .addSub("rain_splash", "Weather splash", 0, "Off", "On")
                .addSub("portal_fx", "Portal & spell effects", 0, "Off", "On");

        modEntityCulling = new OptModule(
                "entity_culling", Category.RENDER,
                "Entity Culling",
                "Skip rendering distant entities, armor stands & item frames",
                SeaClientIcons.EYE, 0xFF58C4DC, true, false
        )
                .addSub("max_dist", "Max entity distance", 2, "24m", "32m", "48m", "64m", "96m", "128m")
                .addSub("skip_armor_stand", "Cull armor stands", 1, "On", "Off")
                .addSub("skip_item_frame", "Cull item frames", 1, "On", "Off")
                .addSub("skip_painting", "Cull paintings", 1, "On", "Off");

        modFastSky = new OptModule(
                "fast_sky", Category.RENDER,
                "Fast Sky & Foliage",
                "Optimize clouds, leaves cutout & biome color blending",
                SeaClientIcons.SPARKLES, 0xFF4ADE80, true, false
        )
                .addSub("clouds", "Cloud rendering", 0, "Off", "Fast", "Fancy")
                .addSub("fast_leaves", "Fast leaves", 0, "On", "Off")
                .addSub("biome_blend", "Biome blend radius", 1, "Off", "Fast (3x3)", "Normal (5x5)");

        modChunkBoost = new OptModule(
                "chunk_boost", Category.WORLD,
                "World & Chunk Boost",
                "Optimize simulation distance, entity shadows & weather",
                SeaClientIcons.GLOBE, 0xFF6EA8FF, true, false
        )
                .addSub("sim_dist", "Simulation distance", 0, "5 chunks", "8 chunks", "12 chunks")
                .addSub("entity_shadows", "Entity shadows", 0, "Off", "On");

        modAmbientSound = new OptModule(
                "ambient_sound", Category.WORLD,
                "Lag Sound Limiter",
                "Reduce heavy rain, piston & redstone contraption audio load",
                SeaClientIcons.VOLUME_MUTE, 0xFFF59E0B, false, false
        )
                .addSub("weather_vol", "Weather sound volume", 1, "0%", "25%", "50%", "100%")
                .addSub("piston_vol", "Piston sound volume", 1, "25%", "50%", "100%");

        modDynamicFps = new OptModule(
                "dynamic_fps", Category.SYSTEM,
                "Dynamic FPS Saver",
                "Throttle framerate when unfocused & unlock max active FPS",
                SeaClientIcons.BOLT, 0xFF8EAFFF, true, false
        )
                .addSub("unfocused_fps", "Unfocused FPS", 1, "15 FPS", "30 FPS", "60 FPS")
                .addSub("focused_fps", "Max focused FPS", 0, "Unlimited", "240 FPS", "180 FPS", "120 FPS")
                .addSub("vsync", "VSync", 0, "Off", "On");

        modMemoryGc = new OptModule(
                "memory_gc", Category.SYSTEM,
                "Memory & GC Cleaner",
                "Automatically clean unused heap memory to prevent lag spikes",
                SeaClientIcons.CPU, 0xFF67B7B2, true, false
        )
                .addSub("gc_interval", "Auto clean interval", 1, "3 Min", "5 Min", "10 Min", "Off")
                .addSub("ram_threshold", "RAM trigger threshold", 1, "75%", "85%", "90%");

        modLowLatency = new OptModule(
                "low_latency", Category.SYSTEM,
                "Low-Latency Camera",
                "Zero view bobbing, static FOV & zero screen distortion",
                SeaClientIcons.SLIDERS, 0xFFEC4899, true, false
        )
                .addSub("fov_effects", "Dynamic FOV effects", 0, "0%", "50%", "100%")
                .addSub("view_bobbing", "View bobbing", 0, "Off", "On")
                .addSub("distortion", "Screen distortion", 0, "0%", "50%", "100%");

        modules.add(modBwResources);
        modules.add(modBwUtilities);
        modules.add(modBridgeAssist);
        modules.add(modTargetHud);
        modules.add(modComboReach);
        modules.add(modToggleSprint);
        modules.add(modPotionHud);
        modules.add(modAutoGg);
        modules.add(modFullbright);
        modules.add(modCrosshair);

        modules.add(modHudStats);
        modules.add(modHudMusic);
        modules.add(modHudKeystrokes);
        modules.add(modHudArmor);

        modules.add(modMuteExplosions);
        modules.add(modCleanView);
        modules.add(modParticleLimiter);
        modules.add(modEntityCulling);
        modules.add(modFastSky);
        modules.add(modChunkBoost);
        modules.add(modAmbientSound);
        modules.add(modDynamicFps);
        modules.add(modMemoryGc);
        modules.add(modLowLatency);

        loadConfig();
        refreshCachedFlags(false);
    }

    public List<OptModule> getAllModules() {
        return modules;
    }

    public List<OptModule> getModulesForCategory(Category cat) {
        if (cat == Category.HOME) {
            return modules;
        }
        List<OptModule> filtered = new ArrayList<>();
        for (OptModule m : modules) {
            if (m.category == cat) {
                filtered.add(m);
            }
        }
        return filtered;
    }

    public void applyPreset(String preset) {
        switch (preset) {
            case "ULTRA_FPS" -> {
                for (OptModule m : modules) {
                    m.enabled = true;
                }
                setSub(modEntityCulling, "max_dist", 0);
                setSub(modEntityCulling, "skip_armor_stand", 0);
                setSub(modEntityCulling, "skip_item_frame", 0);
                setSub(modParticleLimiter, "preset", 0);
                setSub(modParticleLimiter, "smoke", 0);
                setSub(modParticleLimiter, "block_dust", 0);
                setSub(modFastSky, "clouds", 0);
                setSub(modFastSky, "biome_blend", 0);
            }
            case "COMPETITIVE" -> {
                modBwResources.enabled = true;
                modBwUtilities.enabled = true;
                modBridgeAssist.enabled = true;
                modTargetHud.enabled = true;
                modComboReach.enabled = true;
                modToggleSprint.enabled = true;
                modPotionHud.enabled = true;
                modAutoGg.enabled = true;
                modFullbright.enabled = true;
                modCrosshair.enabled = true;
                modCleanView.enabled = true;
                modMuteExplosions.enabled = true;
                modParticleLimiter.enabled = true;
                modEntityCulling.enabled = true;
                modFastSky.enabled = true;
                modDynamicFps.enabled = true;
                modMemoryGc.enabled = true;
                modLowLatency.enabled = true;
                setSub(modEntityCulling, "max_dist", 2);
                setSub(modParticleLimiter, "preset", 1);
            }
            case "BALANCED" -> {
                modBwResources.enabled = true;
                modBwUtilities.enabled = true;
                modTargetHud.enabled = true;
                modToggleSprint.enabled = true;
                modPotionHud.enabled = true;
                modFullbright.enabled = true;
                modCleanView.enabled = true;
                modMuteExplosions.enabled = false;
                modParticleLimiter.enabled = true;
                modEntityCulling.enabled = true;
                modFastSky.enabled = true;
                modChunkBoost.enabled = false;
                modAmbientSound.enabled = false;
                modDynamicFps.enabled = true;
                modMemoryGc.enabled = true;
                modLowLatency.enabled = false;
                setSub(modEntityCulling, "max_dist", 3);
                setSub(modParticleLimiter, "preset", 1);
                setSub(modFastSky, "clouds", 1);
            }
        }
        onSettingChanged();
    }

    private static void setSub(OptModule mod, String id, int idx) {
        SubOption s = mod.getSub(id);
        if (s != null) {
            s.index = Math.clamp(idx, 0, s.values.length - 1);
        }
    }

    public void cleanMemoryNow() {
        System.gc();
        this.lastGcTimeMs = System.currentTimeMillis();
    }

    public void onClientTick(Minecraft mc) {
        if (mc == null) {
            return;
        }
        SeaClientPvpTracker.onClientTick(mc);

        // 1. Dynamic FPS when window focus changes
        if (dynamicFpsEnabled && mc.options != null) {
            boolean focused = mc.isWindowActive();
            if (focused != lastWindowFocused) {
                lastWindowFocused = focused;
                int targetFps = focused ? focusedFpsLimit : unfocusedFpsLimit;
                mc.options.framerateLimit().set(targetFps);
            }
        }

        // 2. Smart periodic RAM / GC check
        if (memoryOptEnabled && autoGcIntervalMs > 0) {
            long now = System.currentTimeMillis();
            if (now - lastGcTimeMs >= autoGcIntervalMs) {
                Runtime rt = Runtime.getRuntime();
                long total = rt.totalMemory();
                long used = total - rt.freeMemory();
                int pct = total > 0 ? (int) ((used * 100L) / total) : 0;
                if (pct >= ramThresholdPercent) {
                    System.gc();
                }
                lastGcTimeMs = now;
            }
        }
    }

    public void handleKeyPress(int keyCode) {
        if (keyCode <= 0) {
            return;
        }
        boolean changed = false;
        for (OptModule m : modules) {
            if (m.keyCode == keyCode) {
                m.enabled = !m.enabled;
                changed = true;
            }
        }
        if (changed) {
            onSettingChanged();
        }
    }

    public void onSettingChanged() {
        refreshCachedFlags(true);
        saveConfig();
    }

    public void refreshCachedFlags(boolean applyToMinecraftOptions) {
        // Bedwars & PvP flags
        bwResourcesEnabled = modBwResources.enabled;
        bwResourcesHideZero = "Hide zero".equals(modBwResources.getSubValue("zero_mode", "Show all"));
        bwShowShopHint = "On".equals(modBwResources.getSubValue("shop_hint", "On"));

        bwUtilitiesEnabled = modBwUtilities.enabled;
        String lb = modBwUtilities.getSubValue("low_blocks", "16 blocks");
        if (lb.startsWith("32")) bwLowBlockThreshold = 32;
        else if (lb.startsWith("8")) bwLowBlockThreshold = 8;
        else if (lb.startsWith("Off")) bwLowBlockThreshold = 0;
        else bwLowBlockThreshold = 16;

        bwBridgeAssistEnabled = modBridgeAssist.enabled;
        bwBridgeOnlyWhenHoldingBlocks = "Holding blocks".equals(modBridgeAssist.getSubValue("display_when", "Holding blocks"));

        targetHudEnabled = modTargetHud.enabled;
        String to = modTargetHud.getSubValue("timeout", "4 Seconds");
        if (to.startsWith("2")) targetHudTimeoutMs = 2000L;
        else if (to.startsWith("6")) targetHudTimeoutMs = 6000L;
        else targetHudTimeoutMs = 4000L;
        targetHudShowStatus = "On".equals(modTargetHud.getSubValue("win_status", "On"));

        comboReachEnabled = modComboReach.enabled;
        showReach = "On".equals(modComboReach.getSubValue("show_reach", "On"));
        showCombo = "On".equals(modComboReach.getSubValue("show_combo", "On"));

        toggleSprintEnabled = modToggleSprint.enabled;
        showSprintHud = "On".equals(modToggleSprint.getSubValue("sprint_hud", "On"));

        potionHudEnabled = modPotionHud.enabled;
        String lw = modPotionHud.getSubValue("low_warn", "8 Seconds");
        if (lw.startsWith("5")) potionLowWarnSec = 5;
        else if (lw.startsWith("Off")) potionLowWarnSec = 0;
        else potionLowWarnSec = 8;

        autoGgEnabled = modAutoGg.enabled;
        autoGgMessage = modAutoGg.getSubValue("gg_msg", "gg");
        showBwTimer = "On".equals(modAutoGg.getSubValue("bw_timer", "On"));

        fullbrightEnabled = modFullbright.enabled;
        String fb = modFullbright.getSubValue("level", "100% (Max)");
        if (fb.startsWith("75")) fullbrightLevel = 8.0f;
        else if (fb.startsWith("50")) fullbrightLevel = 4.0f;
        else fullbrightLevel = 15.0f;

        customCrosshairEnabled = modCrosshair.enabled;
        crosshairStyle = modCrosshair.getSubValue("style", "Cross + Dot");
        crosshairHighlightTarget = "On".equals(modCrosshair.getSubValue("target_color", "On"));

        // HUD flags
        hudStatsEnabled = modHudStats.enabled;
        hudShowFps = "On".equals(modHudStats.getSubValue("show_fps", "On"));
        hudShowPing = "On".equals(modHudStats.getSubValue("show_ping", "On"));
        hudShowCoords = "On".equals(modHudStats.getSubValue("show_coords", "On"));
        hudShowRam = "On".equals(modHudStats.getSubValue("show_ram", "Off"));

        hudMusicEnabled = modHudMusic.enabled;
        hudMusicHideWhenIdle = "Hide widget".equals(modHudMusic.getSubValue("hide_idle", "Show idle"));

        hudKeystrokesEnabled = modHudKeystrokes.enabled;
        hudShowCps = "On".equals(modHudKeystrokes.getSubValue("show_cps", "On"));

        hudArmorEnabled = modHudArmor.enabled;

        // 1. Clean View
        cleanViewEnabled = modCleanView.enabled;
        hideFireOverlay = cleanViewEnabled && "Hidden".equals(modCleanView.getSubValue("fire_overlay", "Hidden"));
        hideTotemPopOverlay = cleanViewEnabled && "Hidden".equals(modCleanView.getSubValue("totem_overlay", "Visible"));
        hideUnderwaterOverlay = cleanViewEnabled && "Hidden".equals(modCleanView.getSubValue("water_overlay", "Hidden"));
        noHurtCam = cleanViewEnabled && "Off".equals(modCleanView.getSubValue("hurt_cam", "Off"));

        // 2. Mute Explosions
        muteExplosionsEnabled = modMuteExplosions.enabled;
        explosionVolumeMultiplier = parsePercent(modMuteExplosions.getSubValue("exp_volume", "Off"), 0.0f);
        hideExplosionParticles = muteExplosionsEnabled && "Hidden".equals(modMuteExplosions.getSubValue("exp_particles", "Hidden"));
        totemVolumeMultiplier = parsePercent(modMuteExplosions.getSubValue("totem_volume", "100%"), 1.0f);

        // 3. Particle Limiter
        particleLimiterEnabled = modParticleLimiter.enabled;
        hideSmokeParticles = particleLimiterEnabled && "Off".equals(modParticleLimiter.getSubValue("smoke", "Off"));
        hideBlockBreakParticles = particleLimiterEnabled && "Off".equals(modParticleLimiter.getSubValue("block_dust", "On"));
        hideRainSplashParticles = particleLimiterEnabled && "Off".equals(modParticleLimiter.getSubValue("rain_splash", "Off"));
        hidePortalParticles = particleLimiterEnabled && "Off".equals(modParticleLimiter.getSubValue("portal_fx", "Off"));

        // 4. Entity Culling
        entityCullingEnabled = modEntityCulling.enabled;
        double distMeters = parseMeters(modEntityCulling.getSubValue("max_dist", "48m"), 48.0);
        maxEntityDistSq = distMeters * distMeters;
        skipArmorStands = entityCullingEnabled && "On".equals(modEntityCulling.getSubValue("skip_armor_stand", "Off"));
        skipItemFrames = entityCullingEnabled && "On".equals(modEntityCulling.getSubValue("skip_item_frame", "Off"));
        skipPaintings = entityCullingEnabled && "On".equals(modEntityCulling.getSubValue("skip_painting", "Off"));

        // 5. Ambient Sound Filter
        ambientSoundFilterEnabled = modAmbientSound.enabled;
        weatherSoundMultiplier = parsePercent(modAmbientSound.getSubValue("weather_vol", "25%"), 0.25f);
        pistonSoundMultiplier = parsePercent(modAmbientSound.getSubValue("piston_vol", "50%"), 0.5f);

        // 6. Dynamic FPS
        dynamicFpsEnabled = modDynamicFps.enabled;
        unfocusedFpsLimit = parseFps(modDynamicFps.getSubValue("unfocused_fps", "30 FPS"), 30);
        focusedFpsLimit = parseFps(modDynamicFps.getSubValue("focused_fps", "Unlimited"), 260);

        // 7. Memory GC
        memoryOptEnabled = modMemoryGc.enabled;
        String gcInt = modMemoryGc.getSubValue("gc_interval", "5 Min");
        if (gcInt.startsWith("3")) autoGcIntervalMs = 180_000L;
        else if (gcInt.startsWith("5")) autoGcIntervalMs = 300_000L;
        else if (gcInt.startsWith("10")) autoGcIntervalMs = 600_000L;
        else autoGcIntervalMs = 0L;
        ramThresholdPercent = (int) (parsePercent(modMemoryGc.getSubValue("ram_threshold", "85%"), 0.85f) * 100);

        if (applyToMinecraftOptions) {
            syncToMinecraftOptions();
        }
    }

    public void syncToMinecraftOptions() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.options == null) {
                return;
            }
            Options opts = mc.options;

            if (modFastSky.enabled) {
                String cloudMode = modFastSky.getSubValue("clouds", "Off");
                if ("Off".equals(cloudMode)) opts.cloudStatus().set(CloudStatus.OFF);
                else if ("Fast".equals(cloudMode)) opts.cloudStatus().set(CloudStatus.FAST);
                else opts.cloudStatus().set(CloudStatus.FANCY);

                boolean fastLeaves = "On".equals(modFastSky.getSubValue("fast_leaves", "On"));
                opts.cutoutLeaves().set(!fastLeaves);

                String blend = modFastSky.getSubValue("biome_blend", "Fast (3x3)");
                if (blend.startsWith("Off")) opts.biomeBlendRadius().set(0);
                else if (blend.startsWith("Fast")) opts.biomeBlendRadius().set(1);
                else opts.biomeBlendRadius().set(2);
            }

            if (modParticleLimiter.enabled) {
                String pMode = modParticleLimiter.getSubValue("preset", "Decreased");
                if ("Minimal".equals(pMode)) opts.particles().set(ParticleStatus.MINIMAL);
                else if ("Decreased".equals(pMode)) opts.particles().set(ParticleStatus.DECREASED);
                else opts.particles().set(ParticleStatus.ALL);
            }

            if (modChunkBoost.enabled) {
                String sim = modChunkBoost.getSubValue("sim_dist", "5 chunks");
                if (sim.startsWith("5")) opts.simulationDistance().set(5);
                else if (sim.startsWith("8")) opts.simulationDistance().set(8);
                else opts.simulationDistance().set(12);

                boolean shadows = "On".equals(modChunkBoost.getSubValue("entity_shadows", "Off"));
                opts.entityShadows().set(shadows);
            }

            if (modDynamicFps.enabled) {
                boolean vsync = "On".equals(modDynamicFps.getSubValue("vsync", "Off"));
                opts.enableVsync().set(vsync);
                opts.framerateLimit().set(focusedFpsLimit);
            }

            if (modLowLatency.enabled) {
                float fovScale = parsePercent(modLowLatency.getSubValue("fov_effects", "0%"), 0.0f);
                opts.fovEffectScale().set((double) fovScale);

                boolean bobbing = "On".equals(modLowLatency.getSubValue("view_bobbing", "Off"));
                opts.bobView().set(bobbing);

                float dist = parsePercent(modLowLatency.getSubValue("distortion", "0%"), 0.0f);
                opts.screenEffectScale().set((double) dist);
            }

            opts.save();
        } catch (Exception ignored) {
        }
    }

    private static float parsePercent(String val, float fallback) {
        if (val == null || "Off".equalsIgnoreCase(val) || "Muted".equalsIgnoreCase(val)) {
            return 0.0f;
        }
        try {
            return Float.parseFloat(val.replace("%", "").trim()) / 100.0f;
        } catch (Exception e) {
            return fallback;
        }
    }

    private static double parseMeters(String val, double fallback) {
        try {
            return Double.parseDouble(val.replace("m", "").trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static int parseFps(String val, int fallback) {
        if (val == null || val.startsWith("Unlimited")) {
            return 260;
        }
        try {
            return Integer.parseInt(val.replace("FPS", "").trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private void loadConfig() {
        if (!Files.exists(configPath)) {
            return;
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(configPath)) {
            props.load(in);
            for (OptModule m : modules) {
                String en = props.getProperty("mod." + m.id + ".enabled");
                if (en != null) {
                    m.enabled = Boolean.parseBoolean(en);
                }
                String exp = props.getProperty("mod." + m.id + ".expanded");
                if (exp != null) {
                    m.expanded = Boolean.parseBoolean(exp);
                }
                String kc = props.getProperty("mod." + m.id + ".keyCode");
                if (kc != null) {
                    m.keyCode = Integer.parseInt(kc);
                }
                String kn = props.getProperty("mod." + m.id + ".keyName");
                if (kn != null) {
                    m.keyName = kn;
                }
                for (SubOption s : m.subOptions) {
                    String si = props.getProperty("mod." + m.id + ".sub." + s.id);
                    if (si != null) {
                        s.index = Math.clamp(Integer.parseInt(si), 0, s.values.length - 1);
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    public void saveConfig() {
        Properties props = new Properties();
        for (OptModule m : modules) {
            props.setProperty("mod." + m.id + ".enabled", String.valueOf(m.enabled));
            props.setProperty("mod." + m.id + ".expanded", String.valueOf(m.expanded));
            props.setProperty("mod." + m.id + ".keyCode", String.valueOf(m.keyCode));
            props.setProperty("mod." + m.id + ".keyName", m.keyName);
            for (SubOption s : m.subOptions) {
                props.setProperty("mod." + m.id + ".sub." + s.id, String.valueOf(s.index));
            }
        }
        try {
            Files.createDirectories(configPath.getParent());
            try (OutputStream out = Files.newOutputStream(configPath)) {
                props.store(out, "Sea Client Optimization & Bedwars Settings");
            }
        } catch (Exception ignored) {
        }
    }
}
