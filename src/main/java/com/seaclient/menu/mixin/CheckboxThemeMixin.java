package com.seaclient.menu.mixin;

import com.seaclient.menu.theme.SeaClientTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Checkbox.class)
public abstract class CheckboxThemeMixin extends AbstractButton {

    @Shadow
    private boolean selected;

    @Shadow
    public static int getBoxSize(Font font) {
        return 16;
    }

    public CheckboxThemeMixin(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Inject(
            method = "extractContents",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V",
                    shift = At.Shift.AFTER
            )
    )
    private void seaclient$renderSharpCheckbox(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        int boxSize = getBoxSize(Minecraft.getInstance().font);
        SeaClientTheme.renderSharpCheckbox(
                graphics,
                this.getX(),
                this.getY(),
                boxSize,
                this.selected,
                this.isHoveredOrFocused()
        );
    }
}
