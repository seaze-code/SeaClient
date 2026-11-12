package com.seaclient.menu.mixin;

import com.seaclient.menu.optimize.SeaClientOptimizer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {

    @Inject(
            method = "calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F",
            at = @At("RETURN"),
            cancellable = true
    )
    private void seaclient$adjustSoundVolume(SoundInstance sound, CallbackInfoReturnable<Float> cir) {
        if (sound == null) {
            return;
        }
        SeaClientOptimizer opt = SeaClientOptimizer.getInstance();
        if (!opt.muteExplosionsEnabled && !opt.ambientSoundFilterEnabled) {
            return;
        }
        Identifier id = sound.getIdentifier();
        if (id == null) {
            return;
        }
        String path = id.getPath();
        float currentVol = cir.getReturnValueF();

        if (opt.muteExplosionsEnabled) {
            if (path.contains("explode") || path.contains("explosion") || path.contains("respawn_anchor") || path.contains("end_crystal")) {
                cir.setReturnValue(currentVol * opt.explosionVolumeMultiplier);
                return;
            }
            if (path.contains("totem.use")) {
                cir.setReturnValue(currentVol * opt.totemVolumeMultiplier);
                return;
            }
        }

        if (opt.ambientSoundFilterEnabled) {
            if (path.contains("weather.rain") || path.contains("lightning_bolt")) {
                cir.setReturnValue(currentVol * opt.weatherSoundMultiplier);
                return;
            }
            if (path.contains("piston")) {
                cir.setReturnValue(currentVol * opt.pistonSoundMultiplier);
            }
        }
    }
}
