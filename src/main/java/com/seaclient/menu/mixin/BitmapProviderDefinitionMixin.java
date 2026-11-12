package com.seaclient.menu.mixin;

import com.seaclient.menu.SeaClientMod;
import net.minecraft.client.gui.font.providers.BitmapProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.IOException;
import java.io.InputStream;

/**
 * Ensures the custom font sheet generated from the user's TTF is loaded globally
 * across all of Minecraft even when fabric-resource-loader (fabric-api) is not installed.
 */
@Mixin(BitmapProvider.Definition.class)
public abstract class BitmapProviderDefinitionMixin {

    @Redirect(
            method = "load",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/packs/resources/ResourceManager;open(Lnet/minecraft/resources/Identifier;)Ljava/io/InputStream;"
            )
    )
    private InputStream seaclient$loadCustomAsciiFont(ResourceManager resourceManager, Identifier location) throws IOException {
        if ("minecraft".equals(location.getNamespace()) && "textures/font/ascii.png".equals(location.getPath())) {
            InputStream customStream = SeaClientMod.class.getResourceAsStream("/assets/minecraft/textures/font/ascii.png");
            if (customStream != null) {
                return customStream;
            }
        }
        return resourceManager.open(location);
    }
}
