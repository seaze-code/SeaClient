package com.seaclient.menu.mixin;

import com.seaclient.menu.optimize.SeaClientOptimizer;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {

    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
    private void seaclient$filterParticles(
            ParticleOptions options,
            double x,
            double y,
            double z,
            double vx,
            double vy,
            double vz,
            CallbackInfoReturnable<Particle> cir
    ) {
        SeaClientOptimizer opt = SeaClientOptimizer.getInstance();
        if (!opt.hideExplosionParticles && !opt.particleLimiterEnabled) {
            return;
        }
        Identifier key = BuiltInRegistries.PARTICLE_TYPE.getKey(options.getType());
        if (key == null) {
            return;
        }
        String path = key.getPath();

        if (opt.hideExplosionParticles && (path.contains("explosion") || path.equals("flash") || path.equals("poof") || path.contains("gust"))) {
            cir.setReturnValue(null);
            return;
        }

        if (opt.particleLimiterEnabled) {
            if (opt.hideSmokeParticles && (path.contains("smoke") || path.contains("campfire") || path.equals("ash") || path.equals("white_ash"))) {
                cir.setReturnValue(null);
                return;
            }
            if (opt.hideBlockBreakParticles && (path.equals("block") || path.equals("block_crumble") || path.equals("dust_plume"))) {
                cir.setReturnValue(null);
                return;
            }
            if (opt.hideRainSplashParticles && (path.equals("rain") || path.equals("splash") || path.contains("dripping"))) {
                cir.setReturnValue(null);
                return;
            }
            if (opt.hidePortalParticles && (path.equals("portal") || path.equals("reverse_portal") || path.equals("enchant"))) {
                cir.setReturnValue(null);
            }
        }
    }
}
