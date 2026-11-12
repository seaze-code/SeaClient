package com.seaclient.menu.mixin;

import com.seaclient.menu.hud.SeaClientHudRenderer;
import com.seaclient.menu.hud.SeaClientPvpTracker;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class HudMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    public abstract boolean isHidden();

    @Shadow
    public abstract DebugScreenOverlay getDebugOverlay();

    @Inject(method = "tick", at = @At("TAIL"))
    private void seaclient$tickTracker(boolean pause, CallbackInfo ci) {
        if (!pause) {
            SeaClientPvpTracker.onClientTick(this.minecraft);
        }
    }

    @Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
    private void seaclient$customCrosshair(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!this.isHidden() && SeaClientHudRenderer.renderCustomCrosshair(graphics, this.minecraft)) {
            ci.cancel();
        }
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void seaclient$renderCustomHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!this.isHidden() && !this.getDebugOverlay().showDebugScreen()) {
            SeaClientHudRenderer.render(graphics, this.minecraft);
        }
    }
}
