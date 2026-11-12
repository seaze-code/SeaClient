package com.seaclient.menu.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.seaclient.menu.optimize.SeaClientOptimizer;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererMixin {

    @Inject(method = "submitFire", at = @At("HEAD"), cancellable = true)
    private static void seaclient$hideFireOverlay(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            TextureAtlasSprite sprite,
            CallbackInfo ci
    ) {
        if (SeaClientOptimizer.getInstance().hideFireOverlay) {
            ci.cancel();
        }
    }

    @Inject(method = "submitWater", at = @At("HEAD"), cancellable = true)
    private static void seaclient$hideWaterOverlay(
            PlayerRenderState.WaterOverlay waterOverlay,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CallbackInfo ci
    ) {
        if (SeaClientOptimizer.getInstance().hideUnderwaterOverlay) {
            ci.cancel();
        }
    }

    @Inject(method = "renderItemActivationAnimation", at = @At("HEAD"), cancellable = true)
    private void seaclient$hideTotemPopOverlay(
            PlayerRenderState playerState,
            PoseStack poseStack,
            float partialTick,
            SubmitNodeCollector collector,
            CallbackInfo ci
    ) {
        if (SeaClientOptimizer.getInstance().hideTotemPopOverlay) {
            ci.cancel();
        }
    }
}
