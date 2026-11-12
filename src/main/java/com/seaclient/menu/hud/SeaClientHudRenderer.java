package com.seaclient.menu.hud;

import com.mojang.blaze3d.platform.NativeImage;
import com.seaclient.menu.media.SystemMediaBridge;
import com.seaclient.menu.optimize.SeaClientOptimizer;
import com.seaclient.menu.screen.SeaClientIcons;
import com.seaclient.menu.skin.RealSkinResolver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

/**
 * Renders the custom sharp-rectangular Sea Client In-Game HUD & Hypixel Bedwars Overlays:
 * 1. Top-Left Client Stats, Coordinates, Bedwars Upgrade Timer, ToggleSprint & Combo/Reach
 * 2. Keystrokes & Real-time CPS Counter HUD
 * 3. Bedwars Resource Counter (Iron, Gold, Diamond, Emerald + Smart Shop Affordability Hint)
 * 4. Bedwars Combat & Blocks Counter (Wool/Blocks, Gapples, Fireballs, TNT, Pearls + Low Block Alert)
 * 5. Speedbridge Pitch/Yaw Sweet-Spot Indicator
 * 6. Target Info HUD (Enemy 2D Face, HP Bar, Distance, WINNING/LOSING combat badge)
 * 7. Top-Right Live Music Player HUD + Active Potion / Invisibility Timers HUD
 * 8. Custom Sharp PvP Crosshair
 */
public final class SeaClientHudRenderer {

    private static final Identifier HUD_ALBUM_ART_ID = Identifier.fromNamespaceAndPath("seaclient", "hud_album_art_dynamic");

    private static final int COL_BG = 0xDC090B12;
    private static final int COL_BG_ACTIVE = 0xE88EAFFF;
    private static final int COL_BORDER = 0xFF1F263A;
    private static final int COL_ACCENT = 0xFF8EAFFF;
    private static final int COL_WHITE = 0xFFFFFFFF;
    private static final int COL_MUTED = 0xFF8C96B2;
    private static final int COL_GREEN = 0xFF4ADE80;
    private static final int COL_RED = 0xFFF45B69;
    private static final int COL_AMBER = 0xFFFBBF24;

    private static final ItemStack ICON_IRON = new ItemStack(Items.IRON_INGOT);
    private static final ItemStack ICON_GOLD = new ItemStack(Items.GOLD_INGOT);
    private static final ItemStack ICON_DIAMOND = new ItemStack(Items.DIAMOND);
    private static final ItemStack ICON_EMERALD = new ItemStack(Items.EMERALD);

    private static final ItemStack ICON_WOOL = new ItemStack(Items.WOOL.white());
    private static final ItemStack ICON_GAPPLE = new ItemStack(Items.GOLDEN_APPLE);
    private static final ItemStack ICON_FIREBALL = new ItemStack(Items.FIRE_CHARGE);
    private static final ItemStack ICON_TNT = new ItemStack(Items.TNT);
    private static final ItemStack ICON_PEARL = new ItemStack(Items.ENDER_PEARL);

    private static final Deque<Long> leftClicks = new ArrayDeque<>();
    private static final Deque<Long> rightClicks = new ArrayDeque<>();
    private static boolean prevAttackDown = false;
    private static boolean prevUseDown = false;

    private static int lastUploadedThumbVersion = -1;
    private static boolean hasHudAlbumTexture = false;

    private SeaClientHudRenderer() {
    }

    public static void render(GuiGraphicsExtractor graphics, Minecraft mc) {
        if (mc == null || mc.player == null || mc.font == null) {
            return;
        }
        SeaClientIcons.ensureRegistered(mc);
        SeaClientOptimizer opt = SeaClientOptimizer.getInstance();

        int sw = graphics.guiWidth();
        int sh = graphics.guiHeight();
        Font font = mc.font;

        updateCpsTracking(mc);

        int leftY = 6;

        // 1. Stats, Coordinates, Sprint & Combo/Reach HUD (Top-Left)
        if (opt.hudStatsEnabled || opt.toggleSprintEnabled || opt.comboReachEnabled) {
            leftY = renderStatsAndPvpPills(graphics, mc, font, opt, 6, leftY);
        }

        // 2. Keystrokes & CPS HUD
        if (opt.hudKeystrokesEnabled) {
            leftY = renderKeystrokesHud(graphics, mc, font, opt, 6, leftY + 2);
        }

        // 3. Bedwars Resource Counter HUD (Iron, Gold, Diamond, Emerald + Shop Hints)
        if (opt.bwResourcesEnabled) {
            leftY = renderBedwarsResourcesHud(graphics, font, opt, 6, leftY + 4);
        }

        // 4. Bedwars Utilities & Block Counter HUD
        if (opt.bwUtilitiesEnabled) {
            leftY = renderBedwarsUtilitiesHud(graphics, font, opt, 6, leftY + 4);
        }

        // 5. Center Overlays: Low Block Alert + Speedbridge Angle Assist + Target Info HUD
        renderCenterBedwarsOverlays(graphics, mc, font, opt, sw, sh);

        // 6. Top-Right Stack: Music Player HUD + Active Potion / Invisibility Timers
        int rightY = 6;
        if (opt.hudMusicEnabled) {
            rightY = renderMusicHud(graphics, mc, font, opt, sw, rightY);
        }
        if (opt.potionHudEnabled) {
            renderPotionTimersHud(graphics, mc, font, opt, sw, rightY + 4);
        }

        // 7. Armor & Mainhand Durability HUD (Right of Hotbar)
        if (opt.hudArmorEnabled) {
            renderArmorHud(graphics, mc, font, sw, sh);
        }
    }

    public static boolean renderCustomCrosshair(GuiGraphicsExtractor graphics, Minecraft mc) {
        SeaClientOptimizer opt = SeaClientOptimizer.getInstance();
        if (!opt.customCrosshairEnabled || "Vanilla".equals(opt.crosshairStyle)) {
            return false;
        }
        int cx = graphics.guiWidth() / 2;
        int cy = graphics.guiHeight() / 2;

        boolean aimedAtEntity = opt.crosshairHighlightTarget && (mc.crosshairPickEntity instanceof LivingEntity);
        int mainCol = aimedAtEntity ? COL_RED : COL_WHITE;
        int outlineCol = 0xCC000000;

        String style = opt.crosshairStyle;
        if ("Dot Only".equals(style)) {
            graphics.fill(cx - 2, cy - 2, cx + 2, cy + 2, outlineCol);
            graphics.fill(cx - 1, cy - 1, cx + 1, cy + 1, mainCol);
        } else if ("Target Box".equals(style)) {
            graphics.outline(cx - 4, cy - 4, 9, 9, outlineCol);
            graphics.outline(cx - 3, cy - 3, 7, 7, mainCol);
            graphics.fill(cx, cy, cx + 1, cy + 1, aimedAtEntity ? COL_RED : COL_ACCENT);
        } else {
            boolean withDot = "Cross + Dot".equals(style);
            int gap = withDot ? 3 : 2;
            int len = 4;

            // Left arm
            graphics.fill(cx - gap - len - 1, cy - 1, cx - gap + 1, cy + 2, outlineCol);
            graphics.fill(cx - gap - len, cy, cx - gap, cy + 1, mainCol);
            // Right arm
            graphics.fill(cx + gap, cy - 1, cx + gap + len + 2, cy + 2, outlineCol);
            graphics.fill(cx + gap + 1, cy, cx + gap + len + 1, cy + 1, mainCol);
            // Top arm
            graphics.fill(cx - 1, cy - gap - len - 1, cx + 2, cy - gap + 1, outlineCol);
            graphics.fill(cx, cy - gap - len, cx + 1, cy - gap, mainCol);
            // Bottom arm
            graphics.fill(cx - 1, cy + gap, cx + 2, cy + gap + len + 2, outlineCol);
            graphics.fill(cx, cy + gap + 1, cx + 1, cy + gap + len + 1, mainCol);

            if (withDot) {
                graphics.fill(cx - 1, cy - 1, cx + 2, cy + 2, outlineCol);
                graphics.fill(cx, cy, cx + 1, cy + 1, aimedAtEntity ? COL_RED : COL_ACCENT);
            }
        }
        return true;
    }

    private static void updateCpsTracking(Minecraft mc) {
        long now = System.currentTimeMillis();
        boolean attackDown = mc.options.keyAttack.isDown();
        boolean useDown = mc.options.keyUse.isDown();

        if (attackDown && !prevAttackDown) {
            leftClicks.addLast(now);
        }
        if (useDown && !prevUseDown) {
            rightClicks.addLast(now);
        }
        prevAttackDown = attackDown;
        prevUseDown = useDown;

        while (!leftClicks.isEmpty() && now - leftClicks.peekFirst() > 1000L) {
            leftClicks.removeFirst();
        }
        while (!rightClicks.isEmpty() && now - rightClicks.peekFirst() > 1000L) {
            rightClicks.removeFirst();
        }
    }

    private static int renderStatsAndCoordsHud(
            GuiGraphicsExtractor graphics,
            Minecraft mc,
            Font font,
            SeaClientOptimizer opt,
            int x,
            int y
    ) {
        LocalPlayer player = mc.player;
        StringBuilder sb = new StringBuilder();
        sb.append(mc.getUser().getName());

        if (opt.hudShowFps) {
            sb.append("  │  ").append(mc.getFps()).append(" FPS");
        }
        if (opt.hudShowPing) {
            int ping = getPlayerPing(mc);
            sb.append("  │  ").append(ping).append(" ms");
        }
        if (opt.hudShowRam) {
            Runtime rt = Runtime.getRuntime();
            long usedMb = (rt.totalMemory() - rt.freeMemory()) / (1024L * 1024L);
            long maxMb = rt.maxMemory() / (1024L * 1024L);
            int pct = maxMb > 0 ? (int) ((usedMb * 100L) / maxMb) : 0;
            sb.append("  │  RAM ").append(pct).append("%");
        }
        if (opt.autoGgEnabled && opt.showBwTimer) {
            sb.append("  │  ").append(SeaClientPvpTracker.getBedwarsNextEventString());
        }

        String mainText = sb.toString();
        int textW = font.width(mainText);
        int boxH = 18;
        int boxW = 20 + textW + 8;

        graphics.fill(x, y, x + boxW, y + boxH, COL_BG);
        graphics.outline(x, y, boxW, boxH, COL_BORDER);
        graphics.fill(x + 2, y + 2, x + 16, y + 16, COL_ACCENT);
        graphics.blit(RenderPipelines.GUI_TEXTURED, SeaClientIcons.SPARKLES, x + 4, y + 4, 0.0f, 0.0f, 10, 10, 10, 10, COL_WHITE);
        graphics.text(font, mainText, x + 21, y + 5, COL_WHITE, false);

        int nextY = y + boxH + 3;

        if (opt.hudShowCoords && player != null) {
            int px = (int) Math.floor(player.getX());
            int py = (int) Math.floor(player.getY());
            int pz = (int) Math.floor(player.getZ());
            String dir = getCardinalDirection(player.getYRot());

            String coordLabel = "XYZ";
            String coordVal = px + ", " + py + ", " + pz + "  [" + dir + "]";
            int labelW = font.width(coordLabel);
            int valW = font.width(coordVal);
            int cBoxW = labelW + valW + 18;
            int cBoxH = 15;

            graphics.fill(x, nextY, x + cBoxW, nextY + cBoxH, COL_BG);
            graphics.outline(x, nextY, cBoxW, cBoxH, COL_BORDER);
            graphics.fill(x, nextY, x + 2, nextY + cBoxH, COL_ACCENT);
            graphics.text(font, coordLabel, x + 6, nextY + 4, COL_ACCENT, false);
            graphics.text(font, coordVal, x + 6 + labelW + 6, nextY + 4, COL_WHITE, false);

            nextY += cBoxH + 3;
        }

        return nextY;
    }

    private static int renderStatsAndPvpPills(
            GuiGraphicsExtractor graphics,
            Minecraft mc,
            Font font,
            SeaClientOptimizer opt,
            int x,
            int y
    ) {
        int curY = y;
        if (opt.hudStatsEnabled) {
            curY = renderStatsAndCoordsHud(graphics, mc, font, opt, x, curY);
        }

        // Second row of small PvP status pills: [Sprinting (Toggled)] + [Reach | Combo]
        int pillX = x;
        boolean drewAnyPill = false;

        if (opt.toggleSprintEnabled && opt.showSprintHud && mc.player != null) {
            boolean sprinting = mc.player.isSprinting();
            String spText = sprinting ? "Sprint [Active]" : "Sprint [Toggled]";
            int spW = font.width(spText) + 12;
            graphics.fill(pillX, curY, pillX + spW, curY + 15, COL_BG);
            graphics.outline(pillX, curY, spW, 15, sprinting ? COL_ACCENT : COL_BORDER);
            graphics.fill(pillX, curY, pillX + 2, curY + 15, sprinting ? COL_GREEN : COL_ACCENT);
            graphics.text(font, spText, pillX + 6, curY + 4, sprinting ? COL_WHITE : COL_MUTED, false);
            pillX += spW + 4;
            drewAnyPill = true;
        }

        if (opt.comboReachEnabled && (opt.showReach || opt.showCombo)) {
            double reach = SeaClientPvpTracker.getLastReachDistance();
            int combo = SeaClientPvpTracker.getComboCount();
            StringBuilder cb = new StringBuilder();
            if (opt.showReach) {
                cb.append(String.format(Locale.ROOT, "%.2f blocks", reach));
            }
            if (opt.showCombo) {
                if (!cb.isEmpty()) cb.append("  │  ");
                cb.append(combo).append(combo >= 3 ? " COMBO!" : " Combo");
            }
            String rcText = cb.toString();
            int rcW = font.width(rcText) + 12;
            boolean hotCombo = combo >= 3;
            graphics.fill(pillX, curY, pillX + rcW, curY + 15, COL_BG);
            graphics.outline(pillX, curY, rcW, 15, hotCombo ? COL_AMBER : COL_BORDER);
            graphics.fill(pillX, curY, pillX + 2, curY + 15, hotCombo ? COL_AMBER : COL_ACCENT);
            graphics.text(font, rcText, pillX + 6, curY + 4, hotCombo ? COL_AMBER : COL_WHITE, false);
            drewAnyPill = true;
        }

        if (drewAnyPill) {
            curY += 18;
        }
        return curY;
    }

    private static int renderBedwarsResourcesHud(
            GuiGraphicsExtractor graphics,
            Font font,
            SeaClientOptimizer opt,
            int x,
            int y
    ) {
        int[] counts = {
                SeaClientPvpTracker.ironCount,
                SeaClientPvpTracker.goldCount,
                SeaClientPvpTracker.diamondCount,
                SeaClientPvpTracker.emeraldCount
        };
        ItemStack[] icons = {ICON_IRON, ICON_GOLD, ICON_DIAMOND, ICON_EMERALD};
        int[] colors = {0xFFE2E8F0, 0xFFFBBF24, 0xFF38BDF8, 0xFF4ADE80};

        List<Integer> visibleIndices = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) {
            if (!opt.bwResourcesHideZero || counts[i] > 0) {
                visibleIndices.add(i);
            }
        }
        if (visibleIndices.isEmpty()) {
            return y;
        }

        String shopHint = opt.bwShowShopHint ? buildBedwarsShopHint(counts[0], counts[1], counts[2], counts[3]) : null;
        int slotW = 36;
        int rowW = Math.max(visibleIndices.size() * slotW + 8, shopHint != null ? font.width(shopHint) + 14 : 0);
        int boxH = (shopHint != null) ? 33 : 22;

        graphics.fill(x, y, x + rowW, y + boxH, COL_BG);
        graphics.outline(x, y, rowW, boxH, COL_BORDER);
        graphics.fill(x, y, x + 2, y + boxH, COL_GREEN);

        for (int idx = 0; idx < visibleIndices.size(); idx++) {
            int i = visibleIndices.get(idx);
            int sx = x + 5 + idx * slotW;
            int sy = y + 3;
            graphics.item(icons[i], sx, sy);
            String cStr = String.valueOf(counts[i]);
            graphics.text(font, cStr, sx + 17, sy + 5, counts[i] > 0 ? colors[i] : COL_MUTED, false);
        }

        if (shopHint != null) {
            graphics.fill(x + 4, y + 21, x + rowW - 4, y + 22, 0xFF171C2B);
            graphics.pose().pushMatrix();
            graphics.pose().translate(x + 6, y + 24);
            graphics.pose().scale(0.78f, 0.78f);
            graphics.text(font, shopHint, 0, 0, COL_ACCENT, false);
            graphics.pose().popMatrix();
        }

        return y + boxH;
    }

    private static String buildBedwarsShopHint(int iron, int gold, int dia, int em) {
        List<String> ready = new ArrayList<>();
        if (em >= 4) ready.add("Obsidian/DiaArmor (4E)");
        else if (em >= 2) ready.add("Invis+Jump (2E)");
        else if (em >= 1) ready.add("BridgeEgg/Pot (1E)");

        if (dia >= 4) ready.add("Sharpness (4D)");
        else if (dia >= 2) ready.add("Prot/Haste (2D)");

        if (gold >= 12) ready.add("IronArmor (12G)");
        else if (gold >= 7) ready.add("IronSword (7G)");
        else if (gold >= 4) ready.add("TNT (4G)");
        else if (gold >= 3) ready.add("Gapple (3G)");

        if (ready.isEmpty() && iron >= 40) {
            ready.add("Fireball (40I)");
        } else if (ready.isEmpty() && iron >= 16) {
            ready.add("Wool x64 (16I)");
        }

        if (ready.isEmpty()) {
            return "Bedwars Resources";
        }
        return "Ready: " + String.join(" · ", ready.subList(0, Math.min(2, ready.size())));
    }

    private static int renderBedwarsUtilitiesHud(
            GuiGraphicsExtractor graphics,
            Font font,
            SeaClientOptimizer opt,
            int x,
            int y
    ) {
        int[] counts = {
                SeaClientPvpTracker.blocksCount,
                SeaClientPvpTracker.gappleCount,
                SeaClientPvpTracker.fireballCount,
                SeaClientPvpTracker.tntCount,
                SeaClientPvpTracker.pearlCount
        };
        ItemStack[] icons = {ICON_WOOL, ICON_GAPPLE, ICON_FIREBALL, ICON_TNT, ICON_PEARL};

        int slotW = 35;
        int boxW = 5 * slotW + 8;
        int boxH = 22;

        boolean lowBlocks = opt.bwLowBlockThreshold > 0 && counts[0] <= opt.bwLowBlockThreshold;
        int borderCol = lowBlocks && SeaClientPvpTracker.holdingBlocks ? COL_RED : COL_BORDER;

        graphics.fill(x, y, x + boxW, y + boxH, COL_BG);
        graphics.outline(x, y, boxW, boxH, borderCol);
        graphics.fill(x, y, x + 2, y + boxH, lowBlocks ? COL_RED : COL_AMBER);

        for (int i = 0; i < 5; i++) {
            int sx = x + 5 + i * slotW;
            int sy = y + 3;
            graphics.item(icons[i], sx, sy);
            String cStr = String.valueOf(counts[i]);
            int col = (i == 0 && lowBlocks) ? COL_RED : (counts[i] > 0 ? COL_WHITE : COL_MUTED);
            graphics.text(font, cStr, sx + 17, sy + 5, col, false);
        }

        return y + boxH;
    }

    private static void renderCenterBedwarsOverlays(
            GuiGraphicsExtractor graphics,
            Minecraft mc,
            Font font,
            SeaClientOptimizer opt,
            int sw,
            int sh
    ) {
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }

        // 1. Speedbridge Angle Assist (below crosshair)
        if (opt.bwBridgeAssistEnabled && (!opt.bwBridgeOnlyWhenHoldingBlocks || SeaClientPvpTracker.holdingBlocks)) {
            float pitch = player.getXRot();
            float rawYaw = (player.getYRot() % 360.0f + 360.0f) % 360.0f;
            float yawMod45 = Math.min(rawYaw % 45.0f, 45.0f - (rawYaw % 45.0f));

            boolean sweetPitch = pitch >= 74.5f && pitch <= 79.8f;
            boolean sweetYaw = yawMod45 <= 3.5f;
            boolean optimal = sweetPitch && sweetYaw;

            String angleText = String.format(Locale.ROOT, "Pitch: %.1f°  │  %s",
                    pitch, optimal ? "BRIDGE SWEET SPOT" : (sweetPitch ? "Pitch OK" : "Aim 75°-79°"));
            int tw = font.width(angleText) + 12;
            int bx = (sw - tw) / 2;
            int by = sh / 2 + 12;

            graphics.fill(bx, by, bx + tw, by + 14, COL_BG);
            graphics.outline(bx, by, tw, 14, optimal ? COL_GREEN : COL_BORDER);
            graphics.fill(bx, by, bx + 2, by + 14, optimal ? COL_GREEN : COL_ACCENT);
            graphics.text(font, angleText, bx + 6, by + 3, optimal ? COL_GREEN : COL_WHITE, false);
        }

        // 2. Low Block Alert above hotbar when holding blocks
        if (opt.bwUtilitiesEnabled
                && opt.bwLowBlockThreshold > 0
                && SeaClientPvpTracker.holdingBlocks
                && SeaClientPvpTracker.blocksCount <= opt.bwLowBlockThreshold) {
            boolean flash = (System.currentTimeMillis() / 250L) % 2L == 0L;
            String warn = "LOW BLOCKS: " + SeaClientPvpTracker.blocksCount + " LEFT!";
            int ww = font.width(warn) + 14;
            int wx = (sw - ww) / 2;
            int wy = sh - 58;
            graphics.fill(wx, wy, wx + ww, wy + 15, flash ? 0xE85A141E : COL_BG);
            graphics.outline(wx, wy, ww, 15, COL_RED);
            graphics.text(font, warn, wx + 7, wy + 4, flash ? COL_WHITE : COL_RED, false);
        }

        // 3. Target Info HUD Card (when aiming at or fighting an entity)
        if (opt.targetHudEnabled) {
            LivingEntity target = SeaClientPvpTracker.getActiveTarget(mc);
            if (target != null && target != player) {
                renderTargetHudCard(graphics, mc, font, opt, player, target, sw / 2 + 16, sh / 2 + 28);
            }
        }
    }

    private static void renderTargetHudCard(
            GuiGraphicsExtractor graphics,
            Minecraft mc,
            Font font,
            SeaClientOptimizer opt,
            LocalPlayer self,
            LivingEntity target,
            int x,
            int y
    ) {
        int cardW = 142;
        int cardH = 38;

        float targetHp = target.getHealth() + target.getAbsorptionAmount();
        float targetMaxHp = Math.max(1.0f, target.getMaxHealth() + target.getAbsorptionAmount());
        float selfHp = self.getHealth() + self.getAbsorptionAmount();

        String statusText = "EVEN";
        int statusCol = COL_ACCENT;
        if (selfHp > targetHp + 1.0f) {
            statusText = "WINNING";
            statusCol = COL_GREEN;
        } else if (targetHp > selfHp + 1.0f) {
            statusText = "LOSING";
            statusCol = COL_RED;
        }

        graphics.fill(x, y, x + cardW, y + cardH, COL_BG);
        graphics.outline(x, y, cardW, cardH, statusCol);
        graphics.fill(x, y, x + 2, y + cardH, statusCol);

        // Left 24x24 Target Head or Icon
        int headX = x + 6;
        int headY = y + 7;
        Identifier skinTex = RealSkinResolver.resolve(mc).texture();
        if (target instanceof AbstractClientPlayer targetPlayer && mc.getConnection() != null) {
            PlayerInfo pInfo = mc.getConnection().getPlayerInfo(targetPlayer.getUUID());
            if (pInfo != null && pInfo.getSkin() != null && pInfo.getSkin().body() != null) {
                skinTex = pInfo.getSkin().body().texturePath();
            }
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, skinTex, headX, headY, 8.0f, 8.0f, 24, 24, 8, 8, 64, 64);
        graphics.blit(RenderPipelines.GUI_TEXTURED, skinTex, headX, headY, 40.0f, 8.0f, 24, 24, 8, 8, 64, 64);
        graphics.outline(headX, headY, 24, 24, COL_BORDER);

        int rx = headX + 28;
        int rw = cardW - 38;

        String name = font.plainSubstrByWidth(target.getName().getString(), rw - 36);
        graphics.text(font, name, rx, y + 6, COL_WHITE, false);

        if (opt.targetHudShowStatus) {
            graphics.pose().pushMatrix();
            graphics.pose().translate(x + cardW - 4, y + 7);
            graphics.pose().scale(0.72f, 0.72f);
            int stW = font.width(statusText);
            graphics.text(font, statusText, -stW, 0, statusCol, false);
            graphics.pose().popMatrix();
        }

        double dist = self.distanceTo(target);
        String hpInfo = String.format(Locale.ROOT, "%.1f HP  ·  %.1fm", targetHp, dist);
        graphics.pose().pushMatrix();
        graphics.pose().translate(rx, y + 17);
        graphics.pose().scale(0.8f, 0.8f);
        graphics.text(font, hpInfo, 0, 0, COL_MUTED, false);
        graphics.pose().popMatrix();

        // Sharp Health Bar
        int barY = y + 28;
        graphics.fill(rx, barY, rx + rw, barY + 4, 0xFF1E2436);
        float hpRatio = Math.clamp(targetHp / targetMaxHp, 0.0f, 1.0f);
        int fillW = Math.round(rw * hpRatio);
        if (fillW > 0) {
            graphics.fill(rx, barY, rx + fillW, barY + 4, statusCol);
        }
    }

    private static int renderKeystrokesHud(
            GuiGraphicsExtractor graphics,
            Minecraft mc,
            Font font,
            SeaClientOptimizer opt,
            int x,
            int y
    ) {
        int keySize = 18;
        int gap = 2;

        boolean wDown = mc.options.keyUp.isDown();
        boolean aDown = mc.options.keyLeft.isDown();
        boolean sDown = mc.options.keyDown.isDown();
        boolean dDown = mc.options.keyRight.isDown();
        boolean lmbDown = mc.options.keyAttack.isDown();
        boolean rmbDown = mc.options.keyUse.isDown();
        boolean spaceDown = mc.options.keyJump.isDown();

        drawKeyBox(graphics, font, x + keySize + gap, y, keySize, keySize, "W", null, wDown);

        int row2Y = y + keySize + gap;
        drawKeyBox(graphics, font, x, row2Y, keySize, keySize, "A", null, aDown);
        drawKeyBox(graphics, font, x + keySize + gap, row2Y, keySize, keySize, "S", null, sDown);
        drawKeyBox(graphics, font, x + (keySize + gap) * 2, row2Y, keySize, keySize, "D", null, dDown);

        int totalW = keySize * 3 + gap * 2;
        int curY = row2Y + keySize + gap;

        if (opt.hudShowCps) {
            int halfW = (totalW - gap) / 2;
            int mbH = 18;
            String lCps = leftClicks.size() + " CPS";
            String rCps = rightClicks.size() + " CPS";
            drawKeyBox(graphics, font, x, curY, halfW, mbH, "LMB", lCps, lmbDown);
            drawKeyBox(graphics, font, x + halfW + gap, curY, totalW - halfW - gap, mbH, "RMB", rCps, rmbDown);
            curY += mbH + gap;
        }

        int spaceH = 10;
        int bg = spaceDown ? COL_BG_ACTIVE : COL_BG;
        int border = spaceDown ? COL_WHITE : COL_BORDER;
        graphics.fill(x, curY, x + totalW, curY + spaceH, bg);
        graphics.outline(x, curY, totalW, spaceH, border);
        int lineW = 22;
        int lineX = x + (totalW - lineW) / 2;
        graphics.fill(lineX, curY + 4, lineX + lineW, curY + 6, COL_WHITE);

        return curY + spaceH;
    }

    private static void drawKeyBox(
            GuiGraphicsExtractor graphics,
            Font font,
            int x, int y, int w, int h,
            String mainLabel,
            String subLabel,
            boolean pressed
    ) {
        int bg = pressed ? COL_BG_ACTIVE : COL_BG;
        int border = pressed ? COL_WHITE : COL_BORDER;
        graphics.fill(x, y, x + w, y + h, bg);
        graphics.outline(x, y, w, h, border);

        if (subLabel == null) {
            int tw = font.width(mainLabel);
            graphics.text(font, mainLabel, x + (w - tw) / 2, y + (h - 8) / 2, COL_WHITE, false);
        } else {
            graphics.pose().pushMatrix();
            graphics.pose().translate(x + w / 2.0f, y + 3.0f);
            graphics.pose().scale(0.8f, 0.8f);
            int tw1 = font.width(mainLabel);
            graphics.text(font, mainLabel, -tw1 / 2, 0, COL_WHITE, false);
            graphics.pose().popMatrix();

            graphics.pose().pushMatrix();
            graphics.pose().translate(x + w / 2.0f, y + 10.5f);
            graphics.pose().scale(0.68f, 0.68f);
            int tw2 = font.width(subLabel);
            graphics.text(font, subLabel, -tw2 / 2, 0, pressed ? COL_WHITE : COL_MUTED, false);
            graphics.pose().popMatrix();
        }
    }

    private static int renderMusicHud(
            GuiGraphicsExtractor graphics,
            Minecraft mc,
            Font font,
            SeaClientOptimizer opt,
            int screenWidth,
            int y
    ) {
        SystemMediaBridge.MediaSnapshot snap = SystemMediaBridge.getInstance().getSnapshot();
        if (opt.hudMusicHideWhenIdle && (!snap.playing && "No Media Playing".equals(snap.title))) {
            return y;
        }

        if (snap.thumbnailVersion != lastUploadedThumbVersion) {
            lastUploadedThumbVersion = snap.thumbnailVersion;
            if (snap.thumbnailPngBytes != null && snap.thumbnailPngBytes.length > 0) {
                try {
                    NativeImage img = NativeImage.read(snap.thumbnailPngBytes);
                    DynamicTexture dyn = new DynamicTexture(() -> "SeaClientHudAlbumArt", img);
                    mc.getTextureManager().register(HUD_ALBUM_ART_ID, dyn);
                    hasHudAlbumTexture = true;
                } catch (Exception e) {
                    hasHudAlbumTexture = false;
                }
            } else {
                hasHudAlbumTexture = false;
            }
        }

        int w = 148;
        int h = 36;
        int x = screenWidth - w - 6;

        graphics.fill(x, y, x + w, y + h, COL_BG);
        graphics.outline(x, y, w, h, COL_BORDER);
        graphics.fill(x + w - 2, y, x + w, y + h, COL_ACCENT);

        int artSize = 26;
        int artX = x + 5;
        int artY = y + 5;

        if (hasHudAlbumTexture) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    HUD_ALBUM_ART_ID,
                    artX,
                    artY,
                    0.0f,
                    0.0f,
                    artSize,
                    artSize,
                    artSize,
                    artSize
            );
        } else {
            graphics.fillGradient(artX, artY, artX + artSize, artY + artSize, 0xFF142878, 0xFF07091C);
            graphics.blit(RenderPipelines.GUI_TEXTURED, SeaClientIcons.SPARKLES, artX + 7, artY + 7, 0.0f, 0.0f, 12, 12, 12, 12, COL_ACCENT);
        }
        graphics.outline(artX, artY, artSize, artSize, COL_BORDER);

        int rx = artX + artSize + 6;
        int rw = w - (artSize + 18);

        String title = font.plainSubstrByWidth(snap.title, rw);
        graphics.text(font, title, rx, y + 6, COL_WHITE, false);

        graphics.pose().pushMatrix();
        graphics.pose().translate(rx, y + 16);
        graphics.pose().scale(0.8f, 0.8f);
        String artist = font.plainSubstrByWidth(snap.artist, (int) (rw / 0.8f));
        graphics.text(font, artist, 0, 0, COL_MUTED, false);
        graphics.pose().popMatrix();

        int barY = y + 28;
        graphics.fill(rx, barY, rx + rw, barY + 2, 0xFF20263A);
        int fillW = Math.round(rw * snap.progressFraction());
        if (fillW > 0) {
            graphics.fill(rx, barY, rx + fillW, barY + 2, COL_ACCENT);
        }

        return y + h;
    }

    private static void renderPotionTimersHud(
            GuiGraphicsExtractor graphics,
            Minecraft mc,
            Font font,
            SeaClientOptimizer opt,
            int sw,
            int startY
    ) {
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        Collection<MobEffectInstance> effects = player.getActiveEffects();
        if (effects.isEmpty()) {
            return;
        }

        int curY = startY;
        for (MobEffectInstance eff : effects) {
            int ticks = eff.getDuration();
            int totalSec = Math.max(0, ticks / 20);
            String timeStr = eff.isInfiniteDuration() ? "**:**" : String.format("%d:%02d", totalSec / 60, totalSec % 60);

            String effName = Component.translatable(eff.getDescriptionId()).getString();
            int amp = eff.getAmplifier() + 1;
            if (amp > 1 && amp <= 10) {
                effName += " " + amp;
            }

            boolean lowWarn = !eff.isInfiniteDuration() && opt.potionLowWarnSec > 0 && totalSec <= opt.potionLowWarnSec;
            String line = effName + "  " + timeStr;
            int boxW = font.width(line) + 14;
            int boxH = 16;
            int x = sw - boxW - 6;

            int accentCol = lowWarn ? COL_RED : COL_ACCENT;
            graphics.fill(x, curY, x + boxW, curY + boxH, COL_BG);
            graphics.outline(x, curY, boxW, boxH, lowWarn ? COL_RED : COL_BORDER);
            graphics.fill(x + boxW - 2, curY, x + boxW, curY + boxH, accentCol);

            graphics.text(font, effName, x + 5, curY + 4, COL_WHITE, false);
            int timeW = font.width(timeStr);
            graphics.text(font, timeStr, x + boxW - timeW - 6, curY + 4, accentCol, false);

            curY += boxH + 3;
        }
    }

    private static void renderArmorHud(
            GuiGraphicsExtractor graphics,
            Minecraft mc,
            Font font,
            int sw,
            int sh
    ) {
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        EquipmentSlot[] slots = {
                EquipmentSlot.HEAD,
                EquipmentSlot.CHEST,
                EquipmentSlot.LEGS,
                EquipmentSlot.FEET
        };

        int baseX = sw / 2 + 95;
        int baseY = sh - 22;
        int idx = 0;

        for (EquipmentSlot slot : slots) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                int ix = baseX + idx * 20;
                graphics.fill(ix - 1, baseY - 1, ix + 17, baseY + 17, COL_BG);
                graphics.outline(ix - 1, baseY - 1, 18, 18, COL_BORDER);
                graphics.item(stack, ix, baseY);
                graphics.itemDecorations(font, stack, ix, baseY);
                idx++;
            }
        }
    }

    private static int getPlayerPing(Minecraft mc) {
        if (mc.getConnection() != null && mc.player != null) {
            PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
            if (info != null) {
                return info.getLatency();
            }
        }
        return 0;
    }

    private static String getCardinalDirection(float yaw) {
        float norm = (yaw % 360.0f + 360.0f) % 360.0f;
        if (norm >= 315.0f || norm < 45.0f) return "S +Z";
        if (norm < 135.0f) return "W -X";
        if (norm < 225.0f) return "N -Z";
        return "E +X";
    }
}
