package com.seaclient.menu.mixin;

import com.seaclient.menu.theme.SeaClientTheme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.TabButton;
import net.minecraft.client.gui.components.tabs.MenuTabBar;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.components.tabs.TabManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MenuTabBar.MenuTabButton.class)
public abstract class MenuTabButtonThemeMixin extends TabButton {

    public MenuTabButtonThemeMixin(TabManager tabManager, Tab tab, int width, int height) {
        super(tabManager, tab, width, height);
    }

    @Inject(
            method = "extractWidgetRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V",
                    shift = At.Shift.AFTER
            )
    )
    private void seaclient$renderSharpTabButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        SeaClientTheme.renderSharpTabButton(
                graphics,
                this.getX(),
                this.getY(),
                this.getWidth(),
                this.getHeight(),
                this.isSelected(),
                this.isHoveredOrFocused()
        );
    }
}
