package com.seaclient.menu.theme;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Global custom sharp-rectangular UI theme engine for Sea Client.
 * Replaces all vanilla Minecraft backgrounds, buttons, sliders, text boxes,
 * selection lists, item cards, and scrollbars across every screen in the game.
 */
public final class SeaClientTheme {

    public static final Identifier PLANET_BG_ID = Identifier.fromNamespaceAndPath("seaclient", "planet_bg_dynamic");

    public static final int COL_BG_TOP = 0xFF06070D;
    public static final int COL_BG_BOTTOM = 0xFF0A0D18;
    public static final int COL_INWORLD_TOP = 0xDC060810;
    public static final int COL_INWORLD_BOTTOM = 0xEA080B16;

    public static final int COL_CARD_BG = 0xE80A0C14;
    public static final int COL_CARD_BG_HOVER = 0xF2111626;
    public static final int COL_CARD_DISABLED = 0x9907090F;

    public static final int COL_BORDER = 0xFF1E2538;
    public static final int COL_BORDER_HOVER = 0xFF4C5F94;
    public static final int COL_BORDER_DISABLED = 0xFF141826;

    public static final int COL_ACCENT = 0xFF8EAFFF;
    public static final int COL_ACCENT_HOVER = 0xFFA4C0FF;
    public static final int COL_ACCENT_BORDER = 0xFFBFD4FF;
    public static final int COL_ACCENT_GLOW = 0x388EAFFF;

    public static final int COL_TEXT_WHITE = 0xFFFFFFFF;
    public static final int COL_TEXT_MUTED = 0xFF8A94B0;
    public static final int COL_TEXT_DIM = 0xFF5D6682;

    private static boolean planetTextureRegistered = false;

    private SeaClientTheme() {
    }

    public static void ensurePlanetTexture(Minecraft mc) {
        if (planetTextureRegistered || mc == null) {
            return;
        }
        planetTextureRegistered = true;
        try {
            int size = 160;
            NativeImage img = new NativeImage(size, size, true);
            double cx = size * 0.62;
            double cy = size * 0.72;
            double radius = size * 0.56;

            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    int qx = (x / 2) * 2;
                    int qy = (y / 2) * 2;
                    double dx = qx - cx;
                    double dy = qy - cy;
                    double dist = Math.sqrt(dx * dx + dy * dy);

                    int argb = 0x00000000;
                    if (dist <= radius + 4.0 && dist >= radius - 1.5) {
                        if (((qx / 2 + qy / 2) & 1) == 0 || dist <= radius + 1.5) {
                            argb = (dist > radius + 1.5) ? 0x9935498E : 0xEE5873D0;
                        }
                    } else if (dist < radius - 1.5) {
                        double norm = dist / radius;
                        if (norm > 0.88) {
                            argb = 0xF01C2A5E;
                        } else if (norm > 0.70) {
                            argb = 0xF5131E46;
                        } else if (norm > 0.45) {
                            argb = 0xF80D1533;
                        } else {
                            argb = 0xFC080D22;
                        }
                    }
                    img.setPixel(x, y, argb);
                }
            }
            DynamicTexture tex = new DynamicTexture(() -> "SeaClientPlanetBg", img);
            mc.getTextureManager().register(PLANET_BG_ID, tex);
        } catch (Exception ignored) {
        }
    }

    public static void renderSpaceBackground(GuiGraphicsExtractor graphics, int width, int height, boolean inWorld, boolean showHeaderBar) {
        Minecraft mc = Minecraft.getInstance();
        ensurePlanetTexture(mc);

        if (inWorld) {
            graphics.fillGradient(0, 0, width, height, COL_INWORLD_TOP, COL_INWORLD_BOTTOM);
        } else {
            graphics.fillGradient(0, 0, width, height, COL_BG_TOP, COL_BG_BOTTOM);
        }

        // Subtle architectural cyber grid
        int gridColor = inWorld ? 0x068EAFFF : 0x088EAFFF;
        for (int gx = 0; gx < width; gx += 32) {
            graphics.fill(gx, 0, gx + 1, height, gridColor);
        }
        for (int gy = 0; gy < height; gy += 32) {
            graphics.fill(0, gy, width, gy + 1, gridColor);
        }

        // Twinkling sharp pixel starfield
        long timeMs = System.currentTimeMillis();
        int starCount = inWorld ? 40 : 70;
        for (int i = 0; i < starCount; i++) {
            int hash = (i * 73856093) ^ 0x5DEECE66;
            int sx = Math.abs(hash % Math.max(1, width));
            int sy = Math.abs(((hash >>> 8) * 19349663) % Math.max(1, height));
            double phase = (timeMs * 0.0022) + (i * 1.7);
            int alpha = 30 + (int) ((Math.sin(phase) * 0.5 + 0.5) * (inWorld ? 70 : 115));
            int rgb = (i % 5 == 0) ? 0x9BB5FF : 0xD8E2FF;
            int starCol = (alpha << 24) | rgb;
            int size = (i % 11 == 0) ? 2 : 1;
            graphics.fill(sx, sy, sx + size, sy + size, starCol);
        }

        if (!inWorld) {
            int planetDrawSize = Math.min(width, height) + 40;
            int planetX = width - (int) (planetDrawSize * 0.72f);
            int planetY = height - (int) (planetDrawSize * 0.68f);
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    PLANET_BG_ID,
                    planetX,
                    planetY,
                    0.0f,
                    0.0f,
                    planetDrawSize,
                    planetDrawSize,
                    planetDrawSize,
                    planetDrawSize
            );
        }

        // Top glowing accent bar across all screens
        graphics.fill(0, 0, width, 2, COL_ACCENT);

        if (showHeaderBar && mc != null && mc.font != null) {
            // Sleek top header bar + small SEA CLIENT corner tag
            graphics.fill(0, 2, width, 30, 0xB8060810);
            graphics.fill(0, 30, width, 31, COL_BORDER);

            int badgeX = 10;
            int badgeY = 8;
            String tag = "SEA CLIENT";
            int tagW = mc.font.width(tag) + 10;
            graphics.fill(badgeX, badgeY, badgeX + tagW, badgeY + 14, 0xE80B0E1A);
            graphics.outline(badgeX, badgeY, tagW, 14, COL_BORDER);
            graphics.fill(badgeX, badgeY, badgeX + 2, badgeY + 14, COL_ACCENT);
            graphics.text(mc.font, tag, badgeX + 6, badgeY + 3, COL_ACCENT, false);
        }
    }

    public static void renderSharpButton(
            GuiGraphicsExtractor graphics,
            int x, int y, int w, int h,
            boolean active, boolean hoveredOrFocused
    ) {
        if (!active) {
            graphics.fill(x, y, x + w, y + h, COL_CARD_DISABLED);
            graphics.outline(x, y, w, h, COL_BORDER_DISABLED);
            return;
        }
        if (hoveredOrFocused) {
            graphics.fill(x, y, x + w, y + h, COL_ACCENT);
            graphics.outline(x, y, w, h, COL_ACCENT_BORDER);
            graphics.fill(x, y, x + 2, y + h, COL_TEXT_WHITE);
        } else {
            graphics.fill(x, y, x + w, y + h, COL_CARD_BG);
            graphics.outline(x, y, w, h, COL_BORDER);
            graphics.fill(x, y, x + 2, y + h, COL_ACCENT);
        }
    }

    public static void renderSharpSlider(
            GuiGraphicsExtractor graphics,
            Font font,
            int x, int y, int w, int h,
            double value,
            Component message,
            boolean active,
            boolean hoveredOrFocused
    ) {
        double clamped = Math.clamp(value, 0.0, 1.0);
        int bg = active ? (hoveredOrFocused ? COL_CARD_BG_HOVER : COL_CARD_BG) : COL_CARD_DISABLED;
        int border = active ? (hoveredOrFocused ? COL_ACCENT : COL_BORDER) : COL_BORDER_DISABLED;

        graphics.fill(x, y, x + w, y + h, bg);

        // Filled progress bar inside slider
        int innerW = Math.max(0, w - 2);
        int filledW = (int) Math.round(innerW * clamped);
        if (filledW > 0 && active) {
            int fillCol = hoveredOrFocused ? 0x778EAFFF : 0x4A8EAFFF;
            graphics.fill(x + 1, y + 1, x + 1 + filledW, y + h - 1, fillCol);
        }

        // Sharp rectangular slider thumb
        if (active) {
            int thumbW = 4;
            int thumbX = x + 1 + (int) Math.round((innerW - thumbW) * clamped);
            int thumbCol = hoveredOrFocused ? COL_TEXT_WHITE : COL_ACCENT;
            graphics.fill(thumbX, y + 1, thumbX + thumbW, y + h - 1, thumbCol);
        }

        graphics.outline(x, y, w, h, border);

        if (font != null && message != null) {
            int textColor = active ? COL_TEXT_WHITE : COL_TEXT_DIM;
            int textY = y + (h - 8) / 2;
            graphics.centeredText(font, message, x + w / 2, textY, textColor);
        }
    }

    public static void renderSharpEditBox(
            GuiGraphicsExtractor graphics,
            int x, int y, int w, int h,
            boolean active,
            boolean focused
    ) {
        int bg = active ? 0xF2070910 : COL_CARD_DISABLED;
        int border = !active ? COL_BORDER_DISABLED : (focused ? COL_ACCENT : COL_BORDER);
        graphics.fill(x, y, x + w, y + h, bg);
        graphics.outline(x, y, w, h, border);
        if (focused && active) {
            graphics.fill(x, y + h - 2, x + w, y + h, COL_ACCENT);
        }
    }

    public static void renderListBackground(GuiGraphicsExtractor graphics, int x, int y, int right, int bottom) {
        graphics.fill(x, y, right, bottom, 0x9E05070D);
    }

    public static void renderListSeparators(GuiGraphicsExtractor graphics, int x, int y, int right, int bottom) {
        int width = right - x;
        graphics.fill(x, y, right, y + 1, COL_BORDER);
        graphics.fill(x, bottom - 1, right, bottom, COL_BORDER);
        // Subtle center periwinkle accent along top and bottom list dividers
        int glowW = Math.min(180, width / 2);
        int glowX = x + (width - glowW) / 2;
        graphics.fill(glowX, y, glowX + glowW, y + 1, COL_ACCENT);
        graphics.fill(glowX, bottom - 1, glowX + glowW, bottom, 0x668EAFFF);
    }

    public static void renderListEntryCard(
            GuiGraphicsExtractor graphics,
            int x, int y, int w, int h,
            boolean selected, boolean hovered
    ) {
        int cardX = x - 2;
        int cardY = y - 1;
        int cardW = w + 4;
        int cardH = h + 2;

        if (selected) {
            graphics.fill(cardX, cardY, cardX + cardW, cardY + cardH, 0x2E8EAFFF);
            graphics.outline(cardX, cardY, cardW, cardH, COL_ACCENT);
            graphics.fill(cardX, cardY, cardX + 3, cardY + cardH, COL_ACCENT);
        } else if (hovered) {
            graphics.fill(cardX, cardY, cardX + cardW, cardY + cardH, COL_CARD_BG_HOVER);
            graphics.outline(cardX, cardY, cardW, cardH, COL_BORDER_HOVER);
            graphics.fill(cardX, cardY, cardX + 2, cardY + cardH, COL_ACCENT);
        } else {
            graphics.fill(cardX, cardY, cardX + cardW, cardY + cardH, 0xC4090C15);
            graphics.outline(cardX, cardY, cardW, cardH, 0xFF181E30);
        }
    }

    public static void renderScrollbar(
            GuiGraphicsExtractor graphics,
            int scrollBarX, int y, int width, int height,
            int scrollerY, int scrollerHeight,
            boolean scrollable
    ) {
        if (!scrollable) {
            return;
        }
        int barW = Math.max(4, width);
        graphics.fill(scrollBarX, y, scrollBarX + barW, y + height, 0xCC070910);
        graphics.outline(scrollBarX, y, barW, height, COL_BORDER);

        int thumbH = Math.max(12, scrollerHeight);
        int thumbY = Math.clamp(scrollerY, y + 1, Math.max(y + 1, y + height - thumbH - 1));
        graphics.fill(scrollBarX + 1, thumbY, scrollBarX + barW - 1, thumbY + thumbH, COL_ACCENT);
    }

    public static void renderSharpTabButton(
            GuiGraphicsExtractor graphics,
            int x, int y, int w, int h,
            boolean selected, boolean hoveredOrFocused
    ) {
        int bg = selected ? 0xF0101628 : (hoveredOrFocused ? COL_CARD_BG_HOVER : COL_CARD_BG);
        int border = selected ? COL_ACCENT : (hoveredOrFocused ? COL_BORDER_HOVER : COL_BORDER);
        graphics.fill(x, y, x + w, y + h, bg);
        graphics.outline(x, y, w, h, border);
        if (selected) {
            graphics.fill(x, y, x + w, y + 2, COL_ACCENT);
            graphics.fill(x + 2, y + h - 2, x + w - 2, y + h, COL_ACCENT);
        }
    }

    public static void renderSharpCheckbox(
            GuiGraphicsExtractor graphics,
            int x, int y, int boxSize,
            boolean selected, boolean hoveredOrFocused
    ) {
        int bg = selected ? COL_ACCENT : (hoveredOrFocused ? COL_CARD_BG_HOVER : COL_CARD_BG);
        int border = selected ? COL_ACCENT_BORDER : (hoveredOrFocused ? COL_ACCENT : COL_BORDER);
        graphics.fill(x, y, x + boxSize, y + boxSize, bg);
        graphics.outline(x, y, boxSize, boxSize, border);
        if (selected) {
            int pad = Math.max(3, boxSize / 4);
            graphics.fill(x + pad, y + pad, x + boxSize - pad, y + boxSize - pad, COL_TEXT_WHITE);
        }
    }
}
