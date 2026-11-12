package com.seaclient.menu.mixin;

import com.seaclient.menu.optimize.SeaClientOptimizer;
import com.seaclient.menu.screen.SeaClientTitleScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private boolean clientLevelTeardownInProgress;

    @Shadow
    public abstract void setScreen(Screen screen);

    @Inject(method = "tick", at = @At("HEAD"))
    private void seaclient$onTick(CallbackInfo ci) {
        SeaClientOptimizer.getInstance().onClientTick(this.minecraft);
    }

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void seaclient$onSetScreen(Screen screen, CallbackInfo ci) {
        if (screen instanceof TitleScreen && !(screen instanceof SeaClientTitleScreen)) {
            this.setScreen(new SeaClientTitleScreen());
            ci.cancel();
        } else if (screen == null && this.minecraft.level == null && !this.clientLevelTeardownInProgress) {
            this.setScreen(new SeaClientTitleScreen());
            ci.cancel();
        }
    }
}
