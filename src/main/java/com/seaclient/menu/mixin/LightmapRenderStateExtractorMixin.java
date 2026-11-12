package com.seaclient.menu.mixin;

import com.seaclient.menu.optimize.SeaClientOptimizer;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapRenderStateExtractor.class)
public abstract class LightmapRenderStateExtractorMixin {

    @Inject(method = "extract", at = @At("TAIL"))
    private void seaclient$applyFullbright(LightmapRenderState state, float partialTick, CallbackInfo ci) {
        SeaClientOptimizer opt = SeaClientOptimizer.getInstance();
        if (opt.fullbrightEnabled) {
            state.brightness = Math.max(state.brightness, opt.fullbrightLevel);
            state.darknessEffectScale = 0.0f;
        }
    }
}
