package com.seaclient.menu.mixin;

import com.seaclient.menu.screen.SeaClientTitleScreen;
import com.seaclient.menu.theme.SeaClientTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenThemeMixin {

    @Shadow
    public int width;

    @Shadow
    public int height;

    @Shadow
    protected Minecraft minecraft;

    @Inject(method = "extractPanorama", at = @At("HEAD"), cancellable = true)
    private void seaclient$replacePanorama(GuiGraphicsExtractor graphics, float partialTick, CallbackInfo ci) {
        boolean inWorld = this.minecraft != null && this.minecraft.level != null;
        boolean showHeader = !((Object) this instanceof SeaClientTitleScreen);
        SeaClientTheme.renderSpaceBackground(graphics, this.width, this.height, inWorld, showHeader);
        ci.cancel();
    }

    @Inject(method = "extractBlurredBackground", at = @At("HEAD"), cancellable = true)
    private void seaclient$disableMenuBlur(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (this.minecraft == null || this.minecraft.level == null) {
            ci.cancel();
        }
    }

    @Inject(method = "extractMenuBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIII)V", at = @At("HEAD"), cancellable = true)
    private void seaclient$replaceMenuBackground(GuiGraphicsExtractor graphics, int x, int y, int width, int height, CallbackInfo ci) {
        boolean inWorld = this.minecraft != null && this.minecraft.level != null;
        boolean showHeader = !((Object) this instanceof SeaClientTitleScreen);
        SeaClientTheme.renderSpaceBackground(graphics, this.width, this.height, inWorld, showHeader);
        ci.cancel();
    }

    @Inject(method = "extractTransparentBackground", at = @At("HEAD"), cancellable = true)
    private void seaclient$replaceTransparentBackground(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        boolean showHeader = !((Object) this instanceof SeaClientTitleScreen);
        SeaClientTheme.renderSpaceBackground(graphics, this.width, this.height, true, showHeader);
        ci.cancel();
    }

    @Inject(method = "extractMenuBackgroundTexture", at = @At("HEAD"), cancellable = true)
    private static void seaclient$replaceMenuBackgroundTexture(
            GuiGraphicsExtractor graphics,
            Identifier texture,
            int x,
            int y,
            float u,
            float v,
            int width,
            int height,
            CallbackInfo ci
    ) {
        graphics.fill(x, y, x + width, y + height, 0xE8080B14);
        ci.cancel();
    }
}
