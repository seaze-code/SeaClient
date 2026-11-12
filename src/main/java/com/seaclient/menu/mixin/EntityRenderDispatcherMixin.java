package com.seaclient.menu.mixin;

import com.seaclient.menu.optimize.SeaClientOptimizer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void seaclient$cullEntities(
            E entity,
            Frustum frustum,
            double camX,
            double camY,
            double camZ,
            float partialTick,
            CallbackInfoReturnable<Boolean> cir
    ) {
        SeaClientOptimizer opt = SeaClientOptimizer.getInstance();
        if (!opt.entityCullingEnabled || entity instanceof Player) {
            return;
        }
        if (opt.skipArmorStands && entity instanceof ArmorStand) {
            cir.setReturnValue(false);
            return;
        }
        if (opt.skipItemFrames && entity instanceof ItemFrame) {
            cir.setReturnValue(false);
            return;
        }
        if (opt.skipPaintings && entity instanceof Painting) {
            cir.setReturnValue(false);
            return;
        }
        if (entity.distanceToSqr(camX, camY, camZ) > opt.maxEntityDistSq) {
            cir.setReturnValue(false);
        }
    }
}
