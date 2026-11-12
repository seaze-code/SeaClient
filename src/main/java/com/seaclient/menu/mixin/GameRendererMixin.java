package com.seaclient.menu.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.seaclient.menu.optimize.SeaClientOptimizer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void seaclient$disableHurtCameraShake(CameraRenderState cameraState, PoseStack poseStack, CallbackInfo ci) {
        if (SeaClientOptimizer.getInstance().noHurtCam) {
            ci.cancel();
        }
    }
}
