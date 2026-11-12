package com.seaclient.menu.skin;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.function.Supplier;

/**
 * Resolves the player's real skin texture and model type (wide vs slim).
 * Uses Minecraft's SkinManager first, and if running in offline/dev mode without
 * signed profile textures, automatically downloads the real skin for the player's
 * username so the 3D character on the Main Menu always wears their real skin.
 */
public final class RealSkinResolver {

    public record ResolvedSkin(Identifier texture, boolean slim) {
    }

    private static final Identifier CUSTOM_SKIN_ID = Identifier.fromNamespaceAndPath("seaclient", "player_skin_dynamic");

    private static String fetchedUsername = null;
    private static volatile byte[] downloadedSkinPng = null;
    private static volatile boolean uploadPending = false;
    private static Identifier uploadedTextureId = null;
    private static boolean uploadedSlim = false;
    private static Supplier<PlayerSkin> vanillaSkinLookup = null;
    private static String vanillaLookupName = null;

    private RealSkinResolver() {
    }

    public static ResolvedSkin resolve(Minecraft mc) {
        GameProfile profile = mc.getGameProfile();
        String username = mc.getUser().getName();

        if (vanillaSkinLookup == null || !username.equals(vanillaLookupName)) {
            vanillaLookupName = username;
            vanillaSkinLookup = mc.getSkinManager().createLookup(profile, false);
        }

        // Upload downloaded skin bytes on the render thread if ready
        if (uploadPending && downloadedSkinPng != null) {
            uploadPending = false;
            try {
                NativeImage img = NativeImage.read(downloadedSkinPng);
                if (img.getWidth() == 64 && (img.getHeight() == 64 || img.getHeight() == 32)) {
                    // Check pixel (54, 20) alpha to detect slim (3px arm) vs wide (4px arm)
                    int pixel = img.getPixel(54, 20);
                    int alpha = (pixel >>> 24) & 0xFF;
                    uploadedSlim = (alpha == 0);
                    DynamicTexture dyn = new DynamicTexture(() -> "SeaClientPlayerSkin", img);
                    mc.getTextureManager().register(CUSTOM_SKIN_ID, dyn);
                    uploadedTextureId = CUSTOM_SKIN_ID;
                } else {
                    img.close();
                }
            } catch (Exception ignored) {
            }
        }

        boolean hasProfileTextures = profile != null
                && profile.properties() != null
                && !profile.properties().get("textures").isEmpty();

        // Trigger async skin fetch if offline/unauthenticated or if local override exists
        if (!username.equals(fetchedUsername)) {
            fetchedUsername = username;
            startAsyncSkinFetch(mc, username, hasProfileTextures);
        }

        if (uploadedTextureId != null) {
            return new ResolvedSkin(uploadedTextureId, uploadedSlim);
        }

        PlayerSkin vanillaSkin = vanillaSkinLookup.get();
        boolean isSlim = vanillaSkin.model() == PlayerModelType.SLIM;
        return new ResolvedSkin(vanillaSkin.body().texturePath(), isSlim);
    }

    private static void startAsyncSkinFetch(Minecraft mc, String username, boolean hasProfileTextures) {
        Thread t = new Thread(() -> {
            try {
                // 1. Check optional local skin override file (.minecraft/config/sea-client-skin.png)
                File localSkin = new File(new File(mc.gameDirectory, "config"), "sea-client-skin.png");
                if (localSkin.isFile() && localSkin.length() > 64) {
                    try (InputStream in = new FileInputStream(localSkin)) {
                        downloadedSkinPng = in.readAllBytes();
                        uploadPending = true;
                        return;
                    }
                }

                // 2. If already authenticated with Mojang textures, SkinManager handles it
                if (hasProfileTextures) {
                    return;
                }

                // 3. Fetch real skin by username for offline/dev sessions
                String cleanName = username.replaceAll("[^a-zA-Z0-9_]", "");
                if (cleanName.isEmpty()) {
                    return;
                }
                URI uri = URI.create("https://mc-heads.net/skin/" + cleanName);
                HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
                conn.setRequestProperty("User-Agent", "SeaClient/1.0");
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(4000);
                if (conn.getResponseCode() == 200) {
                    try (InputStream in = conn.getInputStream()) {
                        byte[] bytes = in.readAllBytes();
                        if (bytes.length > 100) {
                            downloadedSkinPng = bytes;
                            uploadPending = true;
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }, "SeaClient-SkinFetcher");
        t.setDaemon(true);
        t.start();
    }
}
