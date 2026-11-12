package com.seaclient.menu.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import com.seaclient.menu.data.LastPlayedTracker;
import com.seaclient.menu.media.SystemMediaBridge;
import com.seaclient.menu.skin.RealSkinResolver;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.CreditsAndAttributionScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screens.options.LanguageSelectScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.options.SkinCustomizationScreen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;

import java.lang.reflect.Constructor;
import java.time.LocalTime;

/**
 * Custom sharp-rectangular (no rounded corners) Main Menu for sea-client:
 * - Dynamic time-of-day greeting + real player username + big username header (no "custom client" text)
 * - Singleplayer, Multiplayer, Realms sharp rectangular buttons with high-res vector SDF icons
 * - Real "Continue" quick-play card (strictly pulls from real worlds/servers only, with real server/world icon)
 * - 6-button bottom action bar with crisp high-res vector SDF icons
 * - Real 3D Player Skin model with drag-to-rotate + online status badge
 * - Real Windows System Media Player widget with live album art, title, artist, progress bar, and controls
 * - Real version/mod count footer + Language / Accessibility / Credits links
 */
public class SeaClientTitleScreen extends TitleScreen {

    private static final Identifier ALBUM_ART_ID = Identifier.fromNamespaceAndPath("seaclient", "album_art_dynamic");
    private static final Identifier CONTINUE_ICON_ID = Identifier.fromNamespaceAndPath("seaclient", "continue_icon_dynamic");
    private static final Identifier PLANET_BG_ID = Identifier.fromNamespaceAndPath("seaclient", "planet_bg_dynamic");

    // Color Palette (all sharp rectangles, 0 rounded corners)
    private static final int COL_BG_TOP = 0xFF06070D;
    private static final int COL_BG_BOTTOM = 0xFF0A0D18;
    private static final int COL_CARD_BG = 0xE80A0C14;
    private static final int COL_CARD_BG_HOVER = 0xF2101422;
    private static final int COL_BORDER = 0xFF1E2436;
    private static final int COL_BORDER_HOVER = 0xFF4A5B8C;
    private static final int COL_ACCENT = 0xFF8EAFFF;
    private static final int COL_ACCENT_HOVER = 0xFFA4C0FF;
    private static final int COL_ACCENT_BORDER = 0xFFBFD4FF;
    private static final int COL_TITLE_BLUE = 0xFFC5D4FF;
    private static final int COL_TEXT_WHITE = 0xFFFFFFFF;
    private static final int COL_TEXT_MUTED = 0xFF8690AC;
    private static final int COL_TEXT_DIM = 0xFF68728E;
    private static final int COL_TEAL_BADGE = 0xFF67B7B2;
    private static final int COL_ONLINE_GREEN = 0xFF4ADE80;
    private static final int COL_QUIT_RED = 0xFFE06C75;

    private Model.Simple widePlayerModel;
    private Model.Simple slimPlayerModel;

    private float skinRotationX = -5.0f;
    private float skinRotationY = -22.0f;
    private boolean draggingSkin = false;
    private double lastDragX;
    private double lastDragY;

    private int lastUploadedThumbnailVersion = -1;
    private boolean hasAlbumTexture = false;

    private int lastUploadedContinueIconVersion = -1;
    private boolean hasContinueIconTexture = false;

    private static boolean planetTextureRegistered = false;

    // Layout coordinates cached per frame for click handling
    private int leftColX;
    private int btnWidth;
    private int btnSingleY;
    private int btnMultiY;
    private final int mainBtnHeight = 27;

    private int continueCardY;
    private final int continueCardH = 38;

    private int iconBarY;
    private final int iconBarW = 166;
    private final int iconBarH = 25;

    private int mediaX;
    private int mediaY;
    private final int mediaW = 172;
    private final int mediaH = 50;
    private int prevBtnX, playBtnX, nextBtnX, mediaCtrlY;

    private int skinBoxX0, skinBoxY0, skinBoxX1, skinBoxY1;

    private int langLinkX, accessLinkX, creditsLinkX, footerY;

    public SeaClientTitleScreen() {
        super(false);
    }

    @Override
    protected void init() {
        // Do not call TitleScreen.init() so vanilla buttons/widgets are not added
        SystemMediaBridge.getInstance().ensureStarted();
        updateLayoutCoordinates();

        if (this.minecraft != null) {
            EntityModelSet models = this.minecraft.getEntityModels();
            this.widePlayerModel = new Model.Simple(models.bakeLayer(ModelLayers.PLAYER), RenderTypes::entityTranslucent);
            this.slimPlayerModel = new Model.Simple(models.bakeLayer(ModelLayers.PLAYER_SLIM), RenderTypes::entityTranslucent);
            ensurePlanetTexture(this.minecraft);
            SeaClientIcons.ensureRegistered(this.minecraft);
            LastPlayedTracker.refreshRealDataAsync(this.minecraft);
        }
    }

    private void updateLayoutCoordinates() {
        this.leftColX = Math.max(24, (int) (this.width * 0.055f));
        this.btnWidth = Math.min(220, Math.max(185, (int) (this.width * 0.31f)));

        int totalLeftHeight = 20 + 34 + (mainBtnHeight * 2 + 8) + 12 + continueCardH + 18 + iconBarH;
        int startY = Math.max(24, (this.height - totalLeftHeight) / 2 - 2);
        int bigTitleY = startY + 26;

        this.btnSingleY = bigTitleY + 38;
        this.btnMultiY = this.btnSingleY + mainBtnHeight + 8;
        this.continueCardY = this.btnMultiY + mainBtnHeight + 12;
        this.iconBarY = Math.min(this.height - 44, this.continueCardY + this.continueCardH + 16);

        this.mediaX = this.width - this.mediaW - 16;
        this.mediaY = 12;
        int artX = this.mediaX + 6;
        int rightX = artX + 38 + 8;
        this.mediaCtrlY = this.mediaY + 26;
        this.prevBtnX = rightX + 16;
        this.playBtnX = rightX + 34;
        this.nextBtnX = rightX + 52;
    }

    private static void ensurePlanetTexture(Minecraft mc) {
        if (planetTextureRegistered) {
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

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Rendered inside extractRenderState
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (this.minecraft == null) {
            return;
        }
        SeaClientIcons.ensureRegistered(this.minecraft);
        updateLayoutCoordinates();

        // 1. Deep Space Background + Starfield + Pixel Planet Horizon
        renderSpaceAndPlanetBackground(graphics);

        int totalLeftHeight = 20 + 34 + (mainBtnHeight * 2 + 8) + 12 + continueCardH + 18 + iconBarH;
        int startY = Math.max(24, (this.height - totalLeftHeight) / 2 - 2);

        String username = this.minecraft.getUser().getName();

        // 2. Top-Left Greeting Badge (changes dynamically by time of day)
        renderGreetingBadge(graphics, this.leftColX, startY, username);

        // 3. Main Brand Title
        int bigTitleY = startY + 26;
        graphics.pose().pushMatrix();
        graphics.pose().translate(this.leftColX, bigTitleY);
        float titleScale = 2.6f;
        graphics.pose().scale(titleScale, titleScale);
        graphics.text(this.font, "SeaClient", 0, 0, COL_TITLE_BLUE, false);
        graphics.pose().popMatrix();

        // 4. Two Main Navigation Buttons (Singleplayer, Multiplayer — Realms removed)
        renderMainNavButton(graphics, this.leftColX, this.btnSingleY, this.btnWidth, this.mainBtnHeight,
                "Singleplayer", SeaClientIcons.SINGLEPLAYER, mouseX, mouseY);
        renderMainNavButton(graphics, this.leftColX, this.btnMultiY, this.btnWidth, this.mainBtnHeight,
                "Multiplayer", SeaClientIcons.MULTIPLAYER, mouseX, mouseY);

        // 5. "Continue" Quick-Play Card (strictly real world/server data only)
        renderContinueCard(graphics, this.leftColX, this.continueCardY, this.btnWidth, this.continueCardH, mouseX, mouseY);

        // 6. Bottom-Left 6-Icon Action Bar (crisp SDF icons)
        renderBottomIconBar(graphics, this.leftColX, this.iconBarY, mouseX, mouseY);

        // 7. Right Side: 3D Player Skin Model + Online Status Badge
        renderPlayerSkinSection(graphics, username, mouseX, mouseY);

        // 8. Top-Right: Live System Media / Music Player Widget
        renderMusicWidget(graphics, this.mediaX, this.mediaY, this.mediaW, this.mediaH, mouseX, mouseY);

        // 9. Bottom Footer: Real Version/Mod Count (Left) + Language/Accessibility/Credits (Right)
        renderFooterBar(graphics, username, mouseX, mouseY);
    }

    private void renderSpaceAndPlanetBackground(GuiGraphicsExtractor graphics) {
        graphics.fillGradient(0, 0, this.width, this.height, COL_BG_TOP, COL_BG_BOTTOM);

        long timeMs = System.currentTimeMillis();
        for (int i = 0; i < 70; i++) {
            int hash = (i * 73856093) ^ 0x5DEECE66;
            int sx = Math.abs(hash % Math.max(1, this.width));
            int sy = Math.abs(((hash >>> 8) * 19349663) % Math.max(1, this.height));
            double phase = (timeMs * 0.0022) + (i * 1.7);
            int alpha = 35 + (int) ((Math.sin(phase) * 0.5 + 0.5) * 115);
            int rgb = (i % 5 == 0) ? 0x9BB5FF : 0xD8E2FF;
            int starCol = (alpha << 24) | rgb;
            int size = (i % 11 == 0) ? 2 : 1;
            graphics.fill(sx, sy, sx + size, sy + size, starCol);
        }

        int planetDrawSize = Math.min(this.width, this.height) + 40;
        int planetX = this.width - (int) (planetDrawSize * 0.72f);
        int planetY = this.height - (int) (planetDrawSize * 0.68f);
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

    private void renderGreetingBadge(GuiGraphicsExtractor graphics, int x, int y, String username) {
        String greeting = getTimeOfDayGreeting() + ", ";
        int greetingW = this.font.width(greeting);
        int nameW = this.font.width(username);
        int boxH = 18;
        int boxW = 19 + greetingW + nameW + 9;

        drawSharpBox(graphics, x, y, boxW, boxH, COL_CARD_BG, COL_BORDER);

        int iconBoxX = x + 3;
        int iconBoxY = y + 3;
        graphics.fill(iconBoxX, iconBoxY, iconBoxX + 12, iconBoxY + 12, COL_ACCENT);
        drawIcon(graphics, SeaClientIcons.USER, iconBoxX + 1, iconBoxY + 1, 10, COL_TEXT_WHITE);

        int textX = x + 19;
        int textY = y + 5;
        graphics.text(this.font, greeting, textX, textY, COL_TEXT_MUTED, false);
        graphics.text(this.font, username, textX + greetingW, textY, COL_TEXT_WHITE, false);
    }

    private static String getTimeOfDayGreeting() {
        int hour = LocalTime.now().getHour();
        if (hour >= 5 && hour < 12) {
            return "Good morning";
        } else if (hour >= 12 && hour < 17) {
            return "Good afternoon";
        } else if (hour >= 17 && hour < 21) {
            return "Good evening";
        } else {
            return "Good night";
        }
    }

    private void renderMainNavButton(
            GuiGraphicsExtractor graphics,
            int x, int y, int w, int h,
            String label, Identifier iconId,
            int mouseX, int mouseY
    ) {
        boolean hovered = isInside(mouseX, mouseY, x, y, w, h);
        int bg = hovered ? COL_ACCENT : COL_CARD_BG;
        int border = hovered ? COL_ACCENT_BORDER : COL_BORDER;

        drawSharpBox(graphics, x, y, w, h, bg, border);

        int labelW = this.font.width(label);
        int iconSize = 10;
        int gap = 5;
        int totalW = iconSize + gap + labelW;
        int startX = x + (w - totalW) / 2;
        int textY = y + (h - 8) / 2;
        int iconY = y + (h - iconSize) / 2;

        int iconCol = hovered ? COL_TEXT_WHITE : COL_ACCENT;
        drawIcon(graphics, iconId, startX, iconY, iconSize, iconCol);
        graphics.text(this.font, label, startX + iconSize + gap, textY, COL_TEXT_WHITE, false);
    }

    private void renderContinueCard(
            GuiGraphicsExtractor graphics,
            int x, int y, int w, int h,
            int mouseX, int mouseY
    ) {
        LastPlayedTracker.Entry entry = LastPlayedTracker.getRecentEntry(this.minecraft);
        int iconVer = LastPlayedTracker.getIconVersion();
        if (iconVer != this.lastUploadedContinueIconVersion) {
            this.lastUploadedContinueIconVersion = iconVer;
            if (entry != null && entry.iconPngBytes != null && entry.iconPngBytes.length > 0) {
                try {
                    NativeImage img = NativeImage.read(entry.iconPngBytes);
                    DynamicTexture dyn = new DynamicTexture(() -> "SeaClientContinueIcon", img);
                    this.minecraft.getTextureManager().register(CONTINUE_ICON_ID, dyn);
                    this.hasContinueIconTexture = true;
                } catch (Exception e) {
                    this.hasContinueIconTexture = false;
                }
            } else {
                this.hasContinueIconTexture = false;
            }
        }

        boolean hasRealEntry = (entry != null && entry.displayName != null && !entry.displayName.isBlank());
        String serverOrWorldName = hasRealEntry ? entry.displayName : "No recent session";
        String subText = hasRealEntry
                ? LastPlayedTracker.formatRelativeTime(entry.timestampMs)
                : "Continue · play a world or server";

        boolean cardHovered = isInside(mouseX, mouseY, x, y, w, h);
        drawSharpBox(graphics, x, y, w, h, cardHovered ? COL_CARD_BG_HOVER : COL_CARD_BG,
                cardHovered ? COL_BORDER_HOVER : COL_BORDER);

        // Left 24x24 sharp square badge (renders real world/server icon if available!)
        int badgeSize = 24;
        int badgeX = x + 7;
        int badgeY = y + (h - badgeSize) / 2;

        if (hasRealEntry && this.hasContinueIconTexture) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    CONTINUE_ICON_ID,
                    badgeX,
                    badgeY,
                    0.0f,
                    0.0f,
                    badgeSize,
                    badgeSize,
                    badgeSize,
                    badgeSize
            );
            graphics.outline(badgeX, badgeY, badgeSize, badgeSize, COL_BORDER_HOVER);
        } else {
            int badgeCol = hasRealEntry ? COL_TEAL_BADGE : 0xFF232A3E;
            graphics.fill(badgeX, badgeY, badgeX + badgeSize, badgeY + badgeSize, badgeCol);
            graphics.outline(badgeX, badgeY, badgeSize, badgeSize, 0x44FFFFFF);

            String initial = hasRealEntry
                    ? String.valueOf(Character.toUpperCase(serverOrWorldName.charAt(0)))
                    : "-";
            int initW = this.font.width(initial);
            graphics.text(this.font, initial, badgeX + (badgeSize - initW) / 2, badgeY + (badgeSize - 8) / 2, COL_TEXT_WHITE, false);
        }

        // Middle two lines
        int textX = badgeX + badgeSize + 8;
        int maxTextW = w - (badgeSize + 8 + 38);
        String trimmedSub = this.font.plainSubstrByWidth(subText, (int) (maxTextW / 0.85f));
        String trimmedName = this.font.plainSubstrByWidth(serverOrWorldName, maxTextW);

        graphics.pose().pushMatrix();
        graphics.pose().translate(textX, y + 9);
        graphics.pose().scale(0.85f, 0.85f);
        graphics.text(this.font, trimmedSub, 0, 0, COL_TEXT_MUTED, false);
        graphics.pose().popMatrix();

        graphics.text(this.font, trimmedName, textX, y + 20, hasRealEntry ? COL_TEXT_WHITE : COL_TEXT_DIM, false);

        // Right sharp square play button with crisp SDF play triangle
        int playSize = 20;
        int playX = x + w - playSize - 8;
        int playY = y + (h - playSize) / 2;
        boolean playHovered = isInside(mouseX, mouseY, playX, playY, playSize, playSize);
        int playBg = hasRealEntry ? (playHovered ? COL_ACCENT_HOVER : COL_ACCENT) : 0xFF232A3E;
        int playBorder = hasRealEntry ? COL_ACCENT_BORDER : COL_BORDER;

        graphics.fill(playX, playY, playX + playSize, playY + playSize, playBg);
        graphics.outline(playX, playY, playSize, playSize, playBorder);
        drawIcon(graphics, SeaClientIcons.PLAY, playX + 5, playY + 5, 10, hasRealEntry ? COL_TEXT_WHITE : COL_TEXT_MUTED);
    }

    private void renderBottomIconBar(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
        drawSharpBox(graphics, x, y, this.iconBarW, this.iconBarH, COL_CARD_BG, COL_BORDER);

        Identifier[] icons = {
                SeaClientIcons.SETTINGS,
                SeaClientIcons.SPARKLES,
                SeaClientIcons.SWORD,
                SeaClientIcons.FOLDER,
                SeaClientIcons.GLOBE,
                SeaClientIcons.POWER
        };

        String[] tooltips = {
                "Options",
                "Sea Client Optimizer (RShift)",
                "Skin Customization",
                "Open Game Folder",
                "Language",
                "Quit Game"
        };

        int hoveredIdx = -1;
        for (int i = 0; i < 6; i++) {
            int slotX = getIconSlotX(x, i);
            int slotY = y + 3;
            boolean hov = isInside(mouseX, mouseY, slotX, slotY, 20, 19);
            if (hov) {
                hoveredIdx = i;
                int hovFill = (i == 5) ? 0x38E06C75 : 0x388EAFFF;
                graphics.fill(slotX, slotY, slotX + 20, slotY + 19, hovFill);
                graphics.outline(slotX, slotY, 20, 19, (i == 5) ? COL_QUIT_RED : COL_ACCENT);
            }
            int iconColor = (i == 5) ? COL_QUIT_RED : (hov ? COL_TEXT_WHITE : COL_TEXT_MUTED);
            drawIcon(graphics, icons[i], slotX + 4, slotY + 3, 12, iconColor);
        }

        // Vertical divider before Quit button
        int divX = x + 132;
        graphics.fill(divX, y + 5, divX + 1, y + this.iconBarH - 5, COL_BORDER);

        if (hoveredIdx >= 0) {
            String tip = tooltips[hoveredIdx];
            int tw = this.font.width(tip) + 8;
            int tx = Math.max(8, mouseX - tw / 2);
            int ty = y - 16;
            drawSharpBox(graphics, tx, ty, tw, 13, 0xF40A0C14, COL_BORDER_HOVER);
            graphics.text(this.font, tip, tx + 4, ty + 3, COL_TEXT_WHITE, false);
        }
    }

    private int getIconSlotX(int barX, int index) {
        if (index < 5) {
            return barX + 5 + index * 25;
        }
        return barX + 139;
    }

    private void renderPlayerSkinSection(GuiGraphicsExtractor graphics, String username, int mouseX, int mouseY) {
        int centerX = this.width - Math.max(125, (int) (this.width * 0.28f));
        int skinHeight = Math.min(200, Math.max(135, (int) (this.height * 0.56f)));
        int skinWidth = (int) (skinHeight * 0.65f);

        this.skinBoxX0 = centerX - skinWidth / 2;
        this.skinBoxX1 = centerX + skinWidth / 2;
        this.skinBoxY0 = Math.max(58, (this.height - skinHeight) / 2 - 8);
        this.skinBoxY1 = this.skinBoxY0 + skinHeight;

        RealSkinResolver.ResolvedSkin resolvedSkin = RealSkinResolver.resolve(this.minecraft);
        Model.Simple activeModel = resolvedSkin.slim() ? this.slimPlayerModel : this.widePlayerModel;

        if (activeModel != null) {
            float idleOffset = (float) Math.sin(System.currentTimeMillis() * 0.0018) * 3.0f;
            float lookOffsetY = (mouseX - centerX) * 0.04f;
            float lookOffsetX = (mouseY - (this.skinBoxY0 + skinHeight * 0.35f)) * -0.025f;

            float finalRotX = Math.clamp(this.skinRotationX + lookOffsetX, -35.0f, 35.0f);
            float finalRotY = this.skinRotationY + idleOffset + lookOffsetY;
            float scale = 0.97f * skinHeight / 2.125f;

            graphics.skin(
                    activeModel,
                    resolvedSkin.texture(),
                    scale,
                    finalRotX,
                    finalRotY,
                    -1.0625f,
                    this.skinBoxX0,
                    this.skinBoxY0,
                    this.skinBoxX1,
                    this.skinBoxY1
            );
        }

        // Sharp rectangular status badge beneath the 3D skin ("• <PlayerName>")
        int nameW = this.font.width(username);
        int tagW = 16 + nameW;
        int tagH = 16;
        int tagX = centerX - tagW / 2;
        int tagY = Math.min(this.height - 34, this.skinBoxY1 + 2);

        drawSharpBox(graphics, tagX, tagY, tagW, tagH, COL_CARD_BG, COL_BORDER);
        graphics.fill(tagX + 5, tagY + 6, tagX + 9, tagY + 10, COL_ONLINE_GREEN);
        graphics.text(this.font, username, tagX + 12, tagY + 4, COL_TEXT_WHITE, false);
    }

    private void renderMusicWidget(
            GuiGraphicsExtractor graphics,
            int x, int y, int w, int h,
            int mouseX, int mouseY
    ) {
        SystemMediaBridge.MediaSnapshot snap = SystemMediaBridge.getInstance().getSnapshot();

        if (snap.thumbnailVersion != this.lastUploadedThumbnailVersion) {
            this.lastUploadedThumbnailVersion = snap.thumbnailVersion;
            if (snap.thumbnailPngBytes != null && snap.thumbnailPngBytes.length > 0) {
                try {
                    NativeImage img = NativeImage.read(snap.thumbnailPngBytes);
                    DynamicTexture dyn = new DynamicTexture(() -> "SeaClientAlbumArt", img);
                    this.minecraft.getTextureManager().register(ALBUM_ART_ID, dyn);
                    this.hasAlbumTexture = true;
                } catch (Exception e) {
                    this.hasAlbumTexture = false;
                }
            } else {
                this.hasAlbumTexture = false;
            }
        }

        drawSharpBox(graphics, x, y, w, h, COL_CARD_BG, COL_BORDER);

        // Left: 38x38 sharp square album cover
        int artSize = 38;
        int artX = x + 6;
        int artY = y + 6;

        if (this.hasAlbumTexture) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    ALBUM_ART_ID,
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
            graphics.fill(artX + 4, artY + 6, artX + artSize - 4, artY + 14, 0xFF3E68FF);
            graphics.fill(artX + 14, artY + 18, artX + 22, artY + 26, 0xFF88B4FF);
        }
        graphics.outline(artX, artY, artSize, artSize, COL_BORDER);

        // Right section: Title, Artist, Crisp SDF Transport Buttons, Progress Bar
        int rightX = artX + artSize + 8;
        int rightW = w - (artSize + 20);

        String songTitle = this.font.plainSubstrByWidth(snap.title, rightW);
        graphics.text(this.font, songTitle, rightX, y + 6, COL_TEXT_WHITE, false);

        graphics.pose().pushMatrix();
        graphics.pose().translate(rightX, y + 16);
        graphics.pose().scale(0.85f, 0.85f);
        String artistName = this.font.plainSubstrByWidth(snap.artist, (int) (rightW / 0.85f));
        graphics.text(this.font, artistName, 0, 0, COL_TEXT_MUTED, false);
        graphics.pose().popMatrix();

        boolean hovPrev = isInside(mouseX, mouseY, this.prevBtnX - 2, this.mediaCtrlY - 1, 12, 12);
        boolean hovPlay = isInside(mouseX, mouseY, this.playBtnX - 2, this.mediaCtrlY - 1, 12, 12);
        boolean hovNext = isInside(mouseX, mouseY, this.nextBtnX - 2, this.mediaCtrlY - 1, 12, 12);

        drawIcon(graphics, SeaClientIcons.PREV, this.prevBtnX, this.mediaCtrlY, 9, hovPrev ? COL_ACCENT : COL_TEXT_MUTED);
        drawIcon(graphics, snap.playing ? SeaClientIcons.PAUSE : SeaClientIcons.PLAY, this.playBtnX, this.mediaCtrlY, 9, hovPlay ? COL_ACCENT : COL_TEXT_WHITE);
        drawIcon(graphics, SeaClientIcons.NEXT, this.nextBtnX, this.mediaCtrlY, 9, hovNext ? COL_ACCENT : COL_TEXT_MUTED);

        // Sharp rectangular progress bar at bottom of widget
        int barY = y + 41;
        graphics.fill(rightX, barY, rightX + rightW, barY + 2, 0xFF22283C);
        int filledW = Math.round(rightW * snap.progressFraction());
        if (filledW > 0) {
            graphics.fill(rightX, barY, rightX + filledW, barY + 2, COL_ACCENT);
        }
        int knobX = Math.clamp(rightX + filledW - 1, rightX, rightX + rightW - 2);
        graphics.fill(knobX, barY - 1, knobX + 2, barY + 3, COL_TEXT_WHITE);
    }

    private void renderFooterBar(GuiGraphicsExtractor graphics, String username, int mouseX, int mouseY) {
        this.footerY = this.height - 13;
        String mcVersion = SharedConstants.getCurrentVersion().name();
        int modCount = FabricLoader.getInstance().getAllMods().size();
        String leftFooter = "Minecraft " + mcVersion + "/Fabric (" + modCount + " Mods) · SeaClient v1.0.0";
        graphics.text(this.font, leftFooter, 8, this.footerY, COL_TEXT_DIM, false);

        String credits = "Credits";
        String access = "Accessibility";
        String lang = "Language";

        int creditsW = this.font.width(credits);
        int accessW = this.font.width(access);
        int langW = this.font.width(lang);

        this.creditsLinkX = this.width - creditsW - 10;
        this.accessLinkX = this.creditsLinkX - accessW - 14;
        this.langLinkX = this.accessLinkX - langW - 14;

        boolean hovLang = isInside(mouseX, mouseY, this.langLinkX - 2, this.footerY - 2, langW + 4, 12);
        boolean hovAccess = isInside(mouseX, mouseY, this.accessLinkX - 2, this.footerY - 2, accessW + 4, 12);
        boolean hovCredits = isInside(mouseX, mouseY, this.creditsLinkX - 2, this.footerY - 2, creditsW + 4, 12);

        graphics.text(this.font, lang, this.langLinkX, this.footerY, hovLang ? COL_TEXT_WHITE : COL_TEXT_MUTED, false);
        graphics.text(this.font, access, this.accessLinkX, this.footerY, hovAccess ? COL_TEXT_WHITE : COL_TEXT_MUTED, false);
        graphics.text(this.font, credits, this.creditsLinkX, this.footerY, hovCredits ? COL_TEXT_WHITE : COL_TEXT_MUTED, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if ((event.button() == 1 || event.button() == 0) && this.minecraft != null) {
            updateLayoutCoordinates();
            int mx = (int) event.x();
            int my = (int) event.y();

            // 1. Main Navigation Buttons
            if (isInside(mx, my, this.leftColX, this.btnSingleY, this.btnWidth, this.mainBtnHeight)) {
                playClickSound();
                this.minecraft.gui.setScreen(new SelectWorldScreen(this));
                return true;
            }
            if (isInside(mx, my, this.leftColX, this.btnMultiY, this.btnWidth, this.mainBtnHeight)) {
                playClickSound();
                this.minecraft.gui.setScreen(new JoinMultiplayerScreen(this));
                return true;
            }

            // 2. Continue Quick-Play Card
            if (isInside(mx, my, this.leftColX, this.continueCardY, this.btnWidth, this.continueCardH)) {
                playClickSound();
                LastPlayedTracker.connectOrOpen(this.minecraft, this);
                return true;
            }

            // 3. Bottom-Left 6-Icon Action Bar
            for (int i = 0; i < 6; i++) {
                int slotX = getIconSlotX(this.leftColX, i);
                int slotY = this.iconBarY + 3;
                if (isInside(mx, my, slotX, slotY, 20, 19)) {
                    playClickSound();
                    handleIconBarAction(i);
                    return true;
                }
            }

            // 4. Top-Right Music Player Controls
            if (isInside(mx, my, this.prevBtnX - 2, this.mediaCtrlY - 1, 12, 12)) {
                playClickSound();
                SystemMediaBridge.getInstance().previousTrack();
                return true;
            }
            if (isInside(mx, my, this.playBtnX - 2, this.mediaCtrlY - 1, 12, 12)) {
                playClickSound();
                SystemMediaBridge.getInstance().togglePlayPause();
                return true;
            }
            if (isInside(mx, my, this.nextBtnX - 2, this.mediaCtrlY - 1, 12, 12)) {
                playClickSound();
                SystemMediaBridge.getInstance().nextTrack();
                return true;
            }

            // 5. Bottom-Right Footer Links
            int langW = this.font.width("Language");
            int accessW = this.font.width("Accessibility");
            int creditsW = this.font.width("Credits");
            if (isInside(mx, my, this.langLinkX - 2, this.footerY - 2, langW + 4, 12)) {
                playClickSound();
                this.minecraft.gui.setScreen(new LanguageSelectScreen(this, this.minecraft.options, this.minecraft.getLanguageManager()));
                return true;
            }
            if (isInside(mx, my, this.accessLinkX - 2, this.footerY - 2, accessW + 4, 12)) {
                playClickSound();
                this.minecraft.gui.setScreen(new AccessibilityOptionsScreen(this, this.minecraft.options));
                return true;
            }
            if (isInside(mx, my, this.creditsLinkX - 2, this.footerY - 2, creditsW + 4, 12)) {
                playClickSound();
                this.minecraft.gui.setScreen(new CreditsAndAttributionScreen(this));
                return true;
            }

            // 6. Start dragging 3D player skin
            if (isInside(mx, my, this.skinBoxX0, this.skinBoxY0, this.skinBoxX1 - this.skinBoxX0, this.skinBoxY1 - this.skinBoxY0)) {
                this.draggingSkin = true;
                this.lastDragX = event.x();
                this.lastDragY = event.y();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 1 || event.button() == 0) {
            this.draggingSkin = false;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.draggingSkin) {
            double dx = event.x() - this.lastDragX;
            double dy = event.y() - this.lastDragY;
            this.lastDragX = event.x();
            this.lastDragY = event.y();
            this.skinRotationY += (float) dx * 1.6f;
            this.skinRotationX = Math.clamp(this.skinRotationX - (float) dy * 1.2f, -40.0f, 40.0f);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    private void handleIconBarAction(int index) {
        if (this.minecraft == null) {
            return;
        }
        switch (index) {
            case 0 -> this.minecraft.gui.setScreen(new OptionsScreen(this, this.minecraft.options));
            case 1 -> this.minecraft.gui.setScreen(new SeaClientOptimizeScreen(this));
            case 2 -> this.minecraft.gui.setScreen(new SkinCustomizationScreen(this, this.minecraft.options));
            case 3 -> openGameDirectory();
            case 4 -> this.minecraft.gui.setScreen(new LanguageSelectScreen(this, this.minecraft.options, this.minecraft.getLanguageManager()));
            case 5 -> this.minecraft.stop();
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_RSHIFT && this.minecraft != null) {
            playClickSound();
            this.minecraft.gui.setScreen(new SeaClientOptimizeScreen(this));
            return true;
        }
        return super.keyPressed(event);
    }

    private void openModMenuOrResourcePacks() {
        if (this.minecraft == null) {
            return;
        }
        try {
            Class<?> modsScreenClass = Class.forName("com.terraformersmc.modmenu.gui.ModsScreen");
            Constructor<?> ctor = modsScreenClass.getConstructor(Screen.class);
            Screen modsScreen = (Screen) ctor.newInstance(this);
            this.minecraft.gui.setScreen(modsScreen);
            return;
        } catch (Exception ignored) {
        }
        this.minecraft.gui.setScreen(new PackSelectionScreen(
                this.minecraft.getResourcePackRepository(),
                repo -> {
                    this.minecraft.options.updateResourcePacks(repo);
                    this.minecraft.gui.setScreen(this);
                },
                this.minecraft.getResourcePackDirectory(),
                Component.translatable("resourcePack.title")
        ));
    }

    private void openGameDirectory() {
        if (this.minecraft == null) {
            return;
        }
        try {
            String path = this.minecraft.gameDirectory.getAbsolutePath();
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("win")) {
                new ProcessBuilder("explorer.exe", path).start();
            } else if (os.contains("mac")) {
                new ProcessBuilder("open", path).start();
            } else {
                new ProcessBuilder("xdg-open", path).start();
            }
        } catch (Exception ignored) {
        }
    }

    private void playClickSound() {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    private static void drawIcon(GuiGraphicsExtractor graphics, Identifier iconId, int x, int y, int size, int color) {
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
                color
        );
    }

    private static void drawSharpBox(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int fillColor, int borderColor) {
        graphics.fill(x, y, x + w, y + h, fillColor);
        graphics.outline(x, y, w, h, borderColor);
    }

    private static boolean isInside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
