package com.seaclient.menu.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.seaclient.menu.hud.SeaClientPvpTracker;
import com.seaclient.menu.optimize.SeaClientOptimizer;
import com.seaclient.menu.optimize.SeaClientOptimizer.Category;
import com.seaclient.menu.optimize.SeaClientOptimizer.OptModule;
import com.seaclient.menu.optimize.SeaClientOptimizer.SubOption;
import com.seaclient.menu.skin.RealSkinResolver;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;

import java.util.List;

/**
 * Custom sharp-rectangular Optimization Control Center Screen for Sea Client.
 * Follows the exact two-column sidebar + expandable module cards layout from the
 * reference image, tailored specifically as a real-time Minecraft Optimizer.
 */
public class SeaClientOptimizeScreen extends Screen {

    private static final int COL_MODAL_BG = 0xF60A0C12;
    private static final int COL_SIDEBAR_BG = 0xFA08090F;
    private static final int COL_MODAL_BORDER = 0xFF20263A;
    private static final int COL_DIVIDER = 0xFF171C2B;

    private static final int COL_CARD_BG = 0xFF10131E;
    private static final int COL_CARD_BG_OFF = 0xFF0C0E16;
    private static final int COL_CARD_HOVER = 0xFF141827;
    private static final int COL_CARD_BORDER_ON = 0xFF3A4972;
    private static final int COL_CARD_BORDER_OFF = 0xFF1B2030;

    private static final int COL_ACCENT = 0xFF8EAFFF;
    private static final int COL_ACCENT_HOVER = 0xFFA5C2FF;
    private static final int COL_ACCENT_BORDER = 0xFFBFD4FF;

    private static final int COL_TEXT_WHITE = 0xFFFFFFFF;
    private static final int COL_TEXT_SUB = 0xFF828CA8;
    private static final int COL_TEXT_DIM = 0xFF5D6680;

    private final Screen parent;
    private Category selectedCategory = Category.BEDWARS;
    private double scrollOffset = 0.0;
    private double maxScroll = 0.0;
    private OptModule listeningForKeyModule = null;

    // Layout geometry cached each frame
    private int modalX, modalY, modalW, modalH;
    private final int sidebarW = 116;
    private int contentX, contentY, contentW, contentH;

    public SeaClientOptimizeScreen(Screen parent) {
        super(Component.literal("Sea Client Optimizer"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (this.minecraft != null) {
            SeaClientIcons.ensureRegistered(this.minecraft);
        }
        updateLayout();
    }

    private void updateLayout() {
        this.modalW = Math.min(540, Math.max(400, this.width - 36));
        this.modalH = Math.min(316, Math.max(230, this.height - 26));
        this.modalX = (this.width - this.modalW) / 2;
        this.modalY = (this.height - this.modalH) / 2;

        this.contentX = this.modalX + this.sidebarW + 1;
        this.contentY = this.modalY + 42;
        this.contentW = this.modalW - this.sidebarW - 1;
        this.contentH = this.modalH - 46;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        SeaClientOptimizer.getInstance().saveConfig();
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(this.parent);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Handled inside extractRenderState so in-game world stays visible behind the modal
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (this.minecraft == null) {
            return;
        }
        SeaClientIcons.ensureRegistered(this.minecraft);
        updateLayout();

        // 1. Background dim overlay (keeps live game world visible when opened in-game, just like the reference image)
        if (this.minecraft.level != null) {
            graphics.fill(0, 0, this.width, this.height, 0x7804060C);
        } else {
            if (this.parent != null) {
                this.parent.extractRenderState(graphics, -1000, -1000, partialTick);
            }
            graphics.fill(0, 0, this.width, this.height, 0xAA04060C);
        }

        // 2. Main Sharp-Rectangular Modal Window
        graphics.fill(modalX, modalY, modalX + modalW, modalY + modalH, COL_MODAL_BG);
        graphics.fill(modalX, modalY, modalX + sidebarW, modalY + modalH, COL_SIDEBAR_BG);
        graphics.outline(modalX, modalY, modalW, modalH, COL_MODAL_BORDER);
        // Vertical divider between sidebar and main content
        graphics.fill(modalX + sidebarW, modalY + 1, modalX + sidebarW + 1, modalY + modalH - 1, COL_DIVIDER);

        String username = this.minecraft.getUser().getName();

        // 3. Left Sidebar
        renderSidebar(graphics, username, mouseX, mouseY);

        // 4. Right Content Header
        renderContentHeader(graphics, mouseX, mouseY);

        // 5. Right Scrollable Optimization Cards
        renderScrollableCards(graphics, mouseX, mouseY);
    }

    private void renderSidebar(GuiGraphicsExtractor graphics, String username, int mouseX, int mouseY) {
        int sx = modalX + 8;
        int sy = modalY + 10;

        // Top-left Brand Icon Box (22x22 sharp square) + Player Name
        graphics.fill(sx, sy, sx + 22, sy + 22, COL_ACCENT);
        graphics.outline(sx, sy, 22, 22, COL_ACCENT_BORDER);
        drawIcon(graphics, SeaClientIcons.SPARKLES, sx + 4, sy + 4, 14, COL_TEXT_WHITE);

        String clippedUser = this.font.plainSubstrByWidth(username, sidebarW - 38);
        graphics.text(this.font, clippedUser, sx + 28, sy + 3, COL_TEXT_WHITE, false);

        graphics.pose().pushMatrix();
        graphics.pose().translate(sx + 28, sy + 13);
        graphics.pose().scale(0.75f, 0.75f);
        graphics.text(this.font, "O P T I M I Z E", 0, 0, COL_TEXT_SUB, false);
        graphics.pose().popMatrix();

        // "M E N U" section label
        graphics.pose().pushMatrix();
        graphics.pose().translate(sx + 2, sy + 30);
        graphics.pose().scale(0.75f, 0.75f);
        graphics.text(this.font, "M E N U", 0, 0, COL_TEXT_DIM, false);
        graphics.pose().popMatrix();

        // Category items (Home, Bedwars, HUD, Render, Particles, World, System)
        Category[] cats = Category.values();
        int navY = modalY + 45;
        int rowW = sidebarW - 14;
        int rowH = 16;

        for (int i = 0; i < cats.length; i++) {
            Category cat = cats[i];
            int ry = navY + i * 17;
            boolean selected = (cat == this.selectedCategory);
            boolean hovered = isInside(mouseX, mouseY, sx - 1, ry, rowW, rowH);

            if (selected) {
                graphics.fill(sx - 1, ry, sx - 1 + rowW, ry + rowH, 0xFF161B2A);
                graphics.outline(sx - 1, ry, rowW, rowH, 0xFF2B3552);
                // Left sharp accent icon badge inside selected row
                graphics.fill(sx + 2, ry + 2, sx + 14, ry + 14, COL_ACCENT);
                drawIcon(graphics, cat.icon, sx + 3, ry + 3, 10, COL_TEXT_WHITE);
                graphics.text(this.font, cat.displayName, sx + 20, ry + 4, COL_TEXT_WHITE, false);
            } else {
                if (hovered) {
                    graphics.fill(sx - 1, ry, sx - 1 + rowW, ry + rowH, 0xFF101320);
                }
                drawIcon(graphics, cat.icon, sx + 3, ry + 3, 10, hovered ? COL_TEXT_WHITE : COL_TEXT_SUB);
                graphics.text(this.font, cat.displayName, sx + 20, ry + 4, hovered ? COL_TEXT_WHITE : 0xFFB8C2DC, false);
            }
        }

        // Divider before utility actions
        int utilStartY = navY + cats.length * 17 + 4;
        graphics.fill(sx, utilStartY - 3, sx + rowW - 2, utilStartY - 2, COL_DIVIDER);

        String[] utilLabels = {"Folder", "Settings", "Boost All"};
        Identifier[] utilIcons = {SeaClientIcons.FOLDER, SeaClientIcons.SETTINGS, SeaClientIcons.BOLT};

        for (int i = 0; i < utilLabels.length; i++) {
            int uy = utilStartY + i * 16;
            boolean hov = isInside(mouseX, mouseY, sx - 1, uy, rowW, 15);
            if (hov) {
                graphics.fill(sx - 1, uy, sx - 1 + rowW, uy + 15, 0xFF101320);
            }
            int iconCol = (i == 2) ? COL_ACCENT : (hov ? COL_TEXT_WHITE : COL_TEXT_SUB);
            drawIcon(graphics, utilIcons[i], sx + 3, uy + 3, 9, iconCol);
            graphics.text(this.font, utilLabels[i], sx + 20, uy + 4, hov ? COL_TEXT_WHITE : 0xFFB8C2DC, false);
        }

        // Bottom Player Profile Card (with real 2D skin face + live FPS)
        int profH = 26;
        int profY = modalY + modalH - profH - 7;
        graphics.fill(sx - 1, profY, sx - 1 + rowW, profY + profH, 0xFF0E111C);
        graphics.outline(sx - 1, profY, rowW, profH, COL_MODAL_BORDER);

        // Render 16x16 player head from real skin texture
        int headX = sx + 4;
        int headY = profY + 5;
        Identifier skinTex = RealSkinResolver.resolve(this.minecraft).texture();
        graphics.blit(RenderPipelines.GUI_TEXTURED, skinTex, headX, headY, 8.0f, 8.0f, 16, 16, 8, 8, 64, 64);
        graphics.blit(RenderPipelines.GUI_TEXTURED, skinTex, headX, headY, 40.0f, 8.0f, 16, 16, 8, 8, 64, 64);
        graphics.outline(headX, headY, 16, 16, COL_ACCENT);

        graphics.text(this.font, clippedUser, headX + 20, profY + 4, COL_TEXT_WHITE, false);

        int fps = this.minecraft.getFps();
        graphics.pose().pushMatrix();
        graphics.pose().translate(headX + 20, profY + 15);
        graphics.pose().scale(0.75f, 0.75f);
        graphics.text(this.font, fps + " FPS · Active", 0, 0, COL_ACCENT, false);
        graphics.pose().popMatrix();
    }

    private void renderContentHeader(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int hx = contentX + 14;
        int hy = modalY + 10;

        // Category title
        graphics.pose().pushMatrix();
        graphics.pose().translate(hx, hy);
        graphics.pose().scale(1.25f, 1.25f);
        graphics.text(this.font, selectedCategory.displayName, 0, 0, COL_TEXT_WHITE, false);
        graphics.pose().popMatrix();

        // Subtitle matching screenshot ("Click to switch · right-click for settings")
        graphics.pose().pushMatrix();
        graphics.pose().translate(hx, hy + 14);
        graphics.pose().scale(0.85f, 0.85f);
        graphics.text(this.font, "Click to switch · right-click for settings", 0, 0, COL_TEXT_SUB, false);
        graphics.pose().popMatrix();

        // Top-right quick preset & RAM clean buttons
        Runtime rt = Runtime.getRuntime();
        long usedMb = (rt.totalMemory() - rt.freeMemory()) / (1024L * 1024L);
        long maxMb = rt.maxMemory() / (1024L * 1024L);
        String ramBtnLabel = "RAM: " + usedMb + "/" + maxMb + "MB";
        int ramBtnW = this.font.width(ramBtnLabel) + 12;
        int ramBtnX = modalX + modalW - ramBtnW - 14;
        int ramBtnY = hy + 3;
        boolean hovRam = isInside(mouseX, mouseY, ramBtnX, ramBtnY, ramBtnW, 18);

        graphics.fill(ramBtnX, ramBtnY, ramBtnX + ramBtnW, ramBtnY + 18, hovRam ? 0xFF1A2238 : 0xFF111522);
        graphics.outline(ramBtnX, ramBtnY, ramBtnW, 18, hovRam ? COL_ACCENT : COL_MODAL_BORDER);
        graphics.text(this.font, ramBtnLabel, ramBtnX + 6, ramBtnY + 5, hovRam ? COL_TEXT_WHITE : COL_ACCENT, false);

        // Close [X] button in top-right corner
        int closeX = modalX + modalW - 11;
        int closeY = modalY + 3;
        boolean hovClose = isInside(mouseX, mouseY, closeX - 3, closeY - 1, 10, 10);
        graphics.text(this.font, "x", closeX, closeY, hovClose ? 0xFFE06C75 : COL_TEXT_DIM, false);
    }

    private void renderScrollableCards(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        List<OptModule> mods = SeaClientOptimizer.getInstance().getModulesForCategory(this.selectedCategory);

        int padX = 14;
        int cardX = contentX + padX;
        int cardW = contentW - padX * 2 - 6;

        // Calculate total scrollable content height
        int totalH = 0;
        if (this.selectedCategory == Category.HOME || this.selectedCategory == Category.BEDWARS) {
            totalH += 42; // Quick Preset / Bedwars QuickPlay Bar height
        }
        for (OptModule mod : mods) {
            totalH += getModuleCardHeight(mod) + 6;
        }
        this.maxScroll = Math.max(0.0, totalH - this.contentH + 6);
        this.scrollOffset = Math.clamp(this.scrollOffset, 0.0, this.maxScroll);

        graphics.enableScissor(contentX, contentY, contentX + contentW, contentY + contentH);

        int curY = contentY - (int) Math.round(this.scrollOffset);

        // If on HOME or BEDWARS tab, render top action bar
        if (this.selectedCategory == Category.HOME) {
            renderHomePresetBar(graphics, cardX, curY, cardW, mouseX, mouseY);
            curY += 42;
        } else if (this.selectedCategory == Category.BEDWARS) {
            renderBedwarsQuickPlayBar(graphics, cardX, curY, cardW, mouseX, mouseY);
            curY += 42;
        }

        for (OptModule mod : mods) {
            int cardH = getModuleCardHeight(mod);
            if (curY + cardH >= contentY - 4 && curY <= contentY + contentH + 4) {
                renderModuleCard(graphics, mod, cardX, curY, cardW, cardH, mouseX, mouseY);
            }
            curY += cardH + 6;
        }

        graphics.disableScissor();

        // Sharp Scrollbar on the right edge if content overflows
        if (this.maxScroll > 0.5) {
            int sbX = contentX + contentW - 6;
            int sbY = contentY;
            int sbH = contentH - 4;
            graphics.fill(sbX, sbY, sbX + 3, sbY + sbH, 0xFF0D101A);
            int thumbH = Math.max(18, (int) ((double) sbH * sbH / Math.max(1, totalH)));
            int thumbY = sbY + (int) ((sbH - thumbH) * (this.scrollOffset / this.maxScroll));
            graphics.fill(sbX, thumbY, sbX + 3, thumbY + thumbH, COL_ACCENT);
        }
    }

    private void renderHomePresetBar(GuiGraphicsExtractor graphics, int x, int y, int w, int mouseX, int mouseY) {
        graphics.fill(x, y, x + w, y + 36, 0xFF0E121E);
        graphics.outline(x, y, w, 36, COL_CARD_BORDER_ON);
        graphics.fill(x, y, x + 2, y + 36, COL_ACCENT);

        graphics.text(this.font, "Optimization Profiles", x + 10, y + 6, COL_TEXT_WHITE, false);
        graphics.pose().pushMatrix();
        graphics.pose().translate(x + 10, y + 18);
        graphics.pose().scale(0.8f, 0.8f);
        graphics.text(this.font, "Apply instant performance presets with one click", 0, 0, COL_TEXT_SUB, false);
        graphics.pose().popMatrix();

        String[] presets = {"Ultra FPS", "Competitive", "Balanced"};
        int btnW = 64;
        int btnH = 18;
        int startBtnX = x + w - (btnW * 3 + 12) - 6;

        for (int i = 0; i < presets.length; i++) {
            int bx = startBtnX + i * (btnW + 4);
            int by = y + 9;
            boolean hov = isInside(mouseX, mouseY, bx, by, btnW, btnH) && mouseY >= contentY && mouseY <= contentY + contentH;
            int bg = hov ? COL_ACCENT : 0xFF171C2E;
            int border = hov ? COL_ACCENT_BORDER : 0xFF2D3756;
            graphics.fill(bx, by, bx + btnW, by + btnH, bg);
            graphics.outline(bx, by, btnW, btnH, border);
            int tw = this.font.width(presets[i]);
            graphics.text(this.font, presets[i], bx + (btnW - tw) / 2, by + 5, COL_TEXT_WHITE, false);
        }
    }

    private void renderBedwarsQuickPlayBar(GuiGraphicsExtractor graphics, int x, int y, int w, int mouseX, int mouseY) {
        graphics.fill(x, y, x + w, y + 36, 0xFF0E121E);
        graphics.outline(x, y, w, 36, COL_CARD_BORDER_ON);
        graphics.fill(x, y, x + 2, y + 36, 0xFF4ADE80);

        graphics.text(this.font, "Hypixel QuickPlay", x + 10, y + 6, COL_TEXT_WHITE, false);
        graphics.pose().pushMatrix();
        graphics.pose().translate(x + 10, y + 18);
        graphics.pose().scale(0.8f, 0.8f);
        graphics.text(this.font, "Instant Bedwars queue & match timer reset", 0, 0, COL_TEXT_SUB, false);
        graphics.pose().popMatrix();

        String[] labels = {"Solo", "Doubles", "3v3v3v3", "4v4v4v4", "Rejoin"};
        int btnW = 42;
        int btnH = 18;
        int startBtnX = x + w - (btnW * labels.length + (labels.length - 1) * 3) - 6;

        for (int i = 0; i < labels.length; i++) {
            int bx = startBtnX + i * (btnW + 3);
            int by = y + 9;
            boolean hov = isInside(mouseX, mouseY, bx, by, btnW, btnH) && mouseY >= contentY && mouseY <= contentY + contentH;
            int bg = hov ? COL_ACCENT : 0xFF171C2E;
            int border = hov ? COL_ACCENT_BORDER : 0xFF2D3756;
            graphics.fill(bx, by, bx + btnW, by + btnH, bg);
            graphics.outline(bx, by, btnW, btnH, border);
            int tw = this.font.width(labels[i]);
            graphics.text(this.font, labels[i], bx + (btnW - tw) / 2, by + 5, COL_TEXT_WHITE, false);
        }
    }

    private int getModuleCardHeight(OptModule mod) {
        if (!mod.expanded) {
            return 36;
        }
        return 36 + 6 + (1 + mod.subOptions.size()) * 20;
    }

    private void renderModuleCard(
            GuiGraphicsExtractor graphics,
            OptModule mod,
            int x, int y, int w, int h,
            int mouseX, int mouseY
    ) {
        boolean inViewport = mouseY >= contentY && mouseY <= contentY + contentH;
        boolean topRowHovered = inViewport && isInside(mouseX, mouseY, x, y, w, 36);

        int bg = mod.enabled ? (topRowHovered ? COL_CARD_HOVER : COL_CARD_BG) : (topRowHovered ? 0xFF10131F : COL_CARD_BG_OFF);
        int border = (mod.enabled || mod.expanded || topRowHovered) ? COL_CARD_BORDER_ON : COL_CARD_BORDER_OFF;

        // Sharp outer card rectangle
        graphics.fill(x, y, x + w, y + h, bg);
        graphics.outline(x, y, w, h, border);

        // Left 22x22 Sharp Colored Icon Badge (matches the vibrant badges in the screenshot)
        int badgeX = x + 7;
        int badgeY = y + 7;
        int badgeSize = 22;
        int badgeBg = mod.enabled ? mod.badgeColor : 0xFF171B28;
        int badgeBorder = mod.enabled ? 0x55FFFFFF : 0xFF252C42;
        graphics.fill(badgeX, badgeY, badgeX + badgeSize, badgeY + badgeSize, badgeBg);
        graphics.outline(badgeX, badgeY, badgeSize, badgeSize, badgeBorder);
        drawIcon(graphics, mod.icon, badgeX + 5, badgeY + 5, 12, mod.enabled ? COL_TEXT_WHITE : COL_TEXT_SUB);

        // Module Title & Subtitle
        int textX = badgeX + badgeSize + 9;
        graphics.text(this.font, mod.title, textX, y + 8, COL_TEXT_WHITE, false);

        graphics.pose().pushMatrix();
        graphics.pose().translate(textX, y + 20);
        graphics.pose().scale(0.85f, 0.85f);
        String desc = this.font.plainSubstrByWidth(mod.description, (int) ((w - 96) / 0.85f));
        graphics.text(this.font, desc, 0, 0, COL_TEXT_SUB, false);
        graphics.pose().popMatrix();

        // Expand/Collapse Chevron (">" or "v")
        int chevronX = x + w - 44;
        int chevronY = y + 14;
        String chevron = mod.expanded ? "v" : ">";
        boolean chevHov = inViewport && isInside(mouseX, mouseY, chevronX - 4, y + 6, 14, 24);
        graphics.text(this.font, chevron, chevronX, chevronY, chevHov ? COL_TEXT_WHITE : COL_TEXT_SUB, false);

        // Sharp Rectangular Toggle Switch (24x13)
        int swW = 24;
        int swH = 13;
        int swX = x + w - swW - 8;
        int swY = y + 11;
        int swBg = mod.enabled ? COL_ACCENT : 0xFF222738;
        int swBorder = mod.enabled ? COL_ACCENT_BORDER : 0xFF333B54;
        graphics.fill(swX, swY, swX + swW, swY + swH, swBg);
        graphics.outline(swX, swY, swW, swH, swBorder);

        int knobSize = 9;
        int knobX = mod.enabled ? (swX + swW - knobSize - 2) : (swX + 2);
        int knobY = swY + 2;
        graphics.fill(knobX, knobY, knobX + knobSize, knobY + knobSize, COL_TEXT_WHITE);

        // Expanded Sub-Settings Rows (exact layout from the screenshot)
        if (mod.expanded) {
            int rowStartY = y + 36;
            graphics.fill(x + 6, rowStartY, x + w - 6, rowStartY + 1, COL_DIVIDER);

            // Row 0: Keybind row ("Keybind  Backspace: none" + "[ None ]")
            int kbY = rowStartY + 3;
            graphics.text(this.font, "Keybind", x + 10, kbY + 5, 0xFFE2E8F8, false);
            int kbLabelW = this.font.width("Keybind");
            graphics.text(this.font, "Backspace: none", x + 10 + kbLabelW + 6, kbY + 5, COL_TEXT_DIM, false);

            int kbBtnW = 68;
            int kbBtnH = 15;
            int kbBtnX = x + w - kbBtnW - 8;
            int kbBtnY = kbY + 2;
            boolean listening = (this.listeningForKeyModule == mod);
            boolean kbHov = inViewport && isInside(mouseX, mouseY, kbBtnX, kbBtnY, kbBtnW, kbBtnH);
            graphics.fill(kbBtnX, kbBtnY, kbBtnX + kbBtnW, kbBtnY + kbBtnH, listening ? 0xFF222C4A : 0xFF141826);
            graphics.outline(kbBtnX, kbBtnY, kbBtnW, kbBtnH, (listening || kbHov) ? COL_ACCENT : 0xFF2A334E);
            String kbText = listening ? "..." : mod.keyName;
            int kbTw = this.font.width(kbText);
            graphics.text(this.font, kbText, kbBtnX + (kbBtnW - kbTw) / 2, kbBtnY + 4, COL_TEXT_WHITE, false);

            // SubOption Rows ("<   Value   >" with underlined value)
            for (int i = 0; i < mod.subOptions.size(); i++) {
                SubOption sub = mod.subOptions.get(i);
                int subY = rowStartY + 3 + (i + 1) * 20;

                graphics.fill(x + 8, subY - 1, x + w - 8, subY, 0xFF141826);
                graphics.text(this.font, sub.label, x + 10, subY + 5, 0xFFE2E8F8, false);

                int ctrlW = 104;
                int ctrlX = x + w - ctrlW - 8;
                int btnSize = 14;
                int leftBtnX = ctrlX;
                int rightBtnX = ctrlX + ctrlW - btnSize;
                int btnY = subY + 2;

                boolean hovLeft = inViewport && isInside(mouseX, mouseY, leftBtnX, btnY, btnSize, btnSize);
                boolean hovRight = inViewport && isInside(mouseX, mouseY, rightBtnX, btnY, btnSize, btnSize);

                // Left "<" sharp button
                graphics.fill(leftBtnX, btnY, leftBtnX + btnSize, btnY + btnSize, hovLeft ? COL_ACCENT : 0xFF171C2C);
                graphics.outline(leftBtnX, btnY, btnSize, btnSize, hovLeft ? COL_ACCENT_BORDER : 0xFF28314A);
                graphics.text(this.font, "<", leftBtnX + 4, btnY + 3, COL_TEXT_WHITE, false);

                // Right ">" sharp button
                graphics.fill(rightBtnX, btnY, rightBtnX + btnSize, btnY + btnSize, hovRight ? COL_ACCENT : 0xFF171C2C);
                graphics.outline(rightBtnX, btnY, btnSize, btnSize, hovRight ? COL_ACCENT_BORDER : 0xFF28314A);
                graphics.text(this.font, ">", rightBtnX + 5, btnY + 3, COL_TEXT_WHITE, false);

                // Centered current value + underline (matches the underlined values in the screenshot!)
                String valStr = sub.currentValue();
                int valW = this.font.width(valStr);
                int valX = ctrlX + (ctrlW - valW) / 2;
                graphics.text(this.font, valStr, valX, btnY + 3, COL_TEXT_WHITE, false);
                graphics.fill(valX - 1, btnY + 12, valX + valW + 1, btnY + 13, 0xAA8EAFFF);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.minecraft == null) {
            return false;
        }
        updateLayout();
        int mx = (int) event.x();
        int my = (int) event.y();
        int btn = event.button();
        boolean isLeft = (btn == 1 || btn == 0);
        boolean isRight = (btn == 3 || btn == 2);

        // Clear keybind listening if clicking elsewhere
        this.listeningForKeyModule = null;

        // Close button [x] or click outside modal
        int closeX = modalX + modalW - 11;
        int closeY = modalY + 3;
        if (isLeft && (isInside(mx, my, closeX - 3, closeY - 1, 10, 10)
                || !isInside(mx, my, modalX, modalY, modalW, modalH))) {
            playClickSound();
            this.onClose();
            return true;
        }

        // 1. Sidebar Category Clicks
        int sx = modalX + 8;
        int navY = modalY + 45;
        int rowW = sidebarW - 14;
        Category[] cats = Category.values();
        if (isLeft) {
            for (int i = 0; i < cats.length; i++) {
                int ry = navY + i * 17;
                if (isInside(mx, my, sx - 1, ry, rowW, 16)) {
                    playClickSound();
                    this.selectedCategory = cats[i];
                    this.scrollOffset = 0.0;
                    return true;
                }
            }

            // Sidebar Utility Actions (Folder, Settings, Boost All)
            int utilStartY = navY + cats.length * 17 + 4;
            for (int i = 0; i < 3; i++) {
                int uy = utilStartY + i * 16;
                if (isInside(mx, my, sx - 1, uy, rowW, 15)) {
                    playClickSound();
                    if (i == 0) {
                        openGameFolder();
                    } else if (i == 1) {
                        this.minecraft.gui.setScreen(new OptionsScreen(this, this.minecraft.options));
                    } else {
                        SeaClientOptimizer.getInstance().applyPreset("COMPETITIVE");
                        SeaClientOptimizer.getInstance().cleanMemoryNow();
                    }
                    return true;
                }
            }

            // Top-right RAM Clean button
            Runtime rt = Runtime.getRuntime();
            long usedMb = (rt.totalMemory() - rt.freeMemory()) / (1024L * 1024L);
            long maxMb = rt.maxMemory() / (1024L * 1024L);
            String ramBtnLabel = "RAM: " + usedMb + "/" + maxMb + "MB";
            int ramBtnW = this.font.width(ramBtnLabel) + 12;
            int ramBtnX = modalX + modalW - ramBtnW - 14;
            int ramBtnY = modalY + 13;
            if (isInside(mx, my, ramBtnX, ramBtnY, ramBtnW, 18)) {
                playClickSound();
                SeaClientOptimizer.getInstance().cleanMemoryNow();
                return true;
            }
        }

        // 2. Scrollable Content Area Clicks
        if (mx >= contentX && mx <= contentX + contentW && my >= contentY && my <= contentY + contentH) {
            List<OptModule> mods = SeaClientOptimizer.getInstance().getModulesForCategory(this.selectedCategory);
            int padX = 14;
            int cardX = contentX + padX;
            int cardW = contentW - padX * 2 - 6;
            int curY = contentY - (int) Math.round(this.scrollOffset);

            // Home Preset Buttons
            if (this.selectedCategory == Category.HOME) {
                int btnW = 64;
                int btnH = 18;
                int startBtnX = cardX + cardW - (btnW * 3 + 12) - 6;
                String[] keys = {"ULTRA_FPS", "COMPETITIVE", "BALANCED"};
                for (int i = 0; i < 3; i++) {
                    int bx = startBtnX + i * (btnW + 4);
                    int by = curY + 9;
                    if (isLeft && isInside(mx, my, bx, by, btnW, btnH)) {
                        playClickSound();
                        SeaClientOptimizer.getInstance().applyPreset(keys[i]);
                        return true;
                    }
                }
                curY += 42;
            } else if (this.selectedCategory == Category.BEDWARS) {
                String[] cmds = {
                        "play bedwars_eight_one",
                        "play bedwars_eight_two",
                        "play bedwars_four_three",
                        "play bedwars_four_four",
                        "rejoin"
                };
                int btnW = 42;
                int btnH = 18;
                int startBtnX = cardX + cardW - (btnW * cmds.length + (cmds.length - 1) * 3) - 6;
                for (int i = 0; i < cmds.length; i++) {
                    int bx = startBtnX + i * (btnW + 3);
                    int by = curY + 9;
                    if (isLeft && isInside(mx, my, bx, by, btnW, btnH)) {
                        playClickSound();
                        SeaClientPvpTracker.resetBedwarsTimer();
                        SeaClientPvpTracker.sendHypixelCommand(cmds[i]);
                        return true;
                    }
                }
                curY += 42;
            }

            for (OptModule mod : mods) {
                int cardH = getModuleCardHeight(mod);

                // Top header row of card (height 36)
                if (isInside(mx, my, cardX, curY, cardW, 36)) {
                    int chevronX = cardX + cardW - 44;
                    boolean clickedChevron = isInside(mx, my, chevronX - 6, curY + 4, 16, 28);
                    if (isRight || (isLeft && clickedChevron)) {
                        playClickSound();
                        mod.expanded = !mod.expanded;
                        SeaClientOptimizer.getInstance().saveConfig();
                        return true;
                    } else if (isLeft) {
                        playClickSound();
                        mod.enabled = !mod.enabled;
                        SeaClientOptimizer.getInstance().onSettingChanged();
                        return true;
                    }
                }

                // Expanded sub-settings rows
                if (mod.expanded && isLeft && isInside(mx, my, cardX, curY + 36, cardW, cardH - 36)) {
                    int rowStartY = curY + 36;
                    int kbY = rowStartY + 3;
                    int kbBtnW = 68;
                    int kbBtnH = 15;
                    int kbBtnX = cardX + cardW - kbBtnW - 8;
                    int kbBtnY = kbY + 2;

                    if (isInside(mx, my, kbBtnX, kbBtnY, kbBtnW, kbBtnH)) {
                        playClickSound();
                        this.listeningForKeyModule = mod;
                        return true;
                    }

                    for (int i = 0; i < mod.subOptions.size(); i++) {
                        SubOption sub = mod.subOptions.get(i);
                        int subY = rowStartY + 3 + (i + 1) * 20;
                        int ctrlW = 104;
                        int ctrlX = cardX + cardW - ctrlW - 8;
                        int btnSize = 14;
                        int leftBtnX = ctrlX;
                        int rightBtnX = ctrlX + ctrlW - btnSize;
                        int btnY = subY + 2;

                        if (isInside(mx, my, leftBtnX, btnY, btnSize, btnSize)) {
                            playClickSound();
                            sub.prev();
                            return true;
                        }
                        if (isInside(mx, my, rightBtnX, btnY, btnSize, btnSize)
                                || isInside(mx, my, leftBtnX + btnSize, btnY, ctrlW - btnSize * 2, btnSize)) {
                            playClickSound();
                            sub.next();
                            return true;
                        }
                    }
                }

                curY += cardH + 6;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.maxScroll > 0.0) {
            this.scrollOffset = Math.clamp(this.scrollOffset - scrollY * 18.0, 0.0, this.maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.listeningForKeyModule != null) {
            int key = event.key();
            if (key == InputConstants.KEY_BACKSPACE || key == InputConstants.KEY_ESCAPE || key == InputConstants.KEY_DELETE) {
                this.listeningForKeyModule.keyCode = -1;
                this.listeningForKeyModule.keyName = "None";
            } else {
                this.listeningForKeyModule.keyCode = key;
                this.listeningForKeyModule.keyName = InputConstants.getKey(event).getDisplayName().getString();
            }
            this.listeningForKeyModule = null;
            SeaClientOptimizer.getInstance().saveConfig();
            playClickSound();
            return true;
        }
        if (event.key() == InputConstants.KEY_ESCAPE || event.key() == InputConstants.KEY_RSHIFT) {
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    private void openGameFolder() {
        try {
            String path = FabricLoader.getInstance().getGameDir().toFile().getAbsolutePath();
            new ProcessBuilder("explorer.exe", path).start();
        } catch (Exception ignored) {
        }
    }

    private void playClickSound() {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    private static void drawIcon(GuiGraphicsExtractor graphics, Identifier iconId, int x, int y, int size, int argbColor) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                iconId,
                x,
                y,
                0.0f,
                0.0f,
                size,
                size,
                size,
                size,
                argbColor
        );
    }

    private static boolean isInside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}
