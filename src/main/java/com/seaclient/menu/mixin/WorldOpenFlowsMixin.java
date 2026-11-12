package com.seaclient.menu.mixin;

import com.seaclient.menu.data.LastPlayedTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldOpenFlows.class)
public abstract class WorldOpenFlowsMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "openWorld", at = @At("HEAD"))
    private void seaclient$onOpenWorld(String levelId, Runnable onCancel, CallbackInfo ci) {
        if (levelId != null && !levelId.isBlank()) {
            LastPlayedTracker.recordWorldJoin(this.minecraft, levelId, levelId);
        }
    }
}
