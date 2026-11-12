package com.seaclient.menu.mixin;

import com.seaclient.menu.theme.SeaClientTheme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractScrollArea.class)
public abstract class ScrollAreaThemeMixin extends AbstractWidget {

    @Shadow
    public abstract boolean scrollable();

    @Shadow
    protected abstract int scrollBarX();

    @Shadow
    protected abstract int scrollBarY();

    @Shadow
    protected abstract int scrollerHeight();

    @Shadow
    protected abstract int scrollbarWidth();

    public ScrollAreaThemeMixin(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Inject(method = "extractScrollbar", at = @At("HEAD"), cancellable = true)
    private void seaclient$renderSharpScrollbar(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
        SeaClientTheme.renderScrollbar(
                graphics,
                this.scrollBarX(),
                this.getY(),
                this.scrollbarWidth(),
                this.getHeight(),
                this.scrollBarY(),
                this.scrollerHeight(),
                this.scrollable()
        );
        ci.cancel();
    }
}
