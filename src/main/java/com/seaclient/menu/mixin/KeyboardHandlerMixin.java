package com.seaclient.menu.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import com.seaclient.menu.optimize.SeaClientOptimizer;
import com.seaclient.menu.screen.SeaClientOptimizeScreen;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void seaclient$onKeyPress(long window, int action, KeyEvent event, CallbackInfo ci) {
        if (action != InputConstants.PRESS || this.minecraft == null || this.minecraft.gui == null) {
            return;
        }
        Screen current = this.minecraft.gui.screen();
        int key = event.key();

        if (key == InputConstants.KEY_RSHIFT) {
            if (current == null || current instanceof PauseScreen) {
                this.minecraft.gui.setScreen(new SeaClientOptimizeScreen(current));
                ci.cancel();
                return;
            }
        }

        if (current == null) {
            SeaClientOptimizer.getInstance().handleKeyPress(key);
        }
    }
}
