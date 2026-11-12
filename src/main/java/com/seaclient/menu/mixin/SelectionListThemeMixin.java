package com.seaclient.menu.mixin;

import com.seaclient.menu.theme.SeaClientTheme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Objects;

@Mixin(AbstractSelectionList.class)
public abstract class SelectionListThemeMixin extends AbstractContainerWidget {

    @Shadow
    protected abstract boolean entriesCanBeSelected();

    public SelectionListThemeMixin(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Inject(method = "extractListBackground", at = @At("HEAD"), cancellable = true)
    private void seaclient$renderListBackground(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        SeaClientTheme.renderListBackground(graphics, this.getX(), this.getY(), this.getRight(), this.getBottom());
        ci.cancel();
    }

    @Inject(method = "extractListSeparators", at = @At("HEAD"), cancellable = true)
    private void seaclient$renderListSeparators(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        SeaClientTheme.renderListSeparators(graphics, this.getX(), this.getY(), this.getRight(), this.getBottom());
        ci.cancel();
    }

    @Inject(method = "extractSelection", at = @At("HEAD"), cancellable = true)
    private void seaclient$cancelDefaultSelectionBox(
            GuiGraphicsExtractor graphics,
            @Coerce Object entry,
            int outlineColor,
            CallbackInfo ci
    ) {
        ci.cancel();
    }

    @Inject(method = "extractItem", at = @At("HEAD"))
    private void seaclient$renderSharpItemCard(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick,
            @Coerce LayoutElement entry,
            CallbackInfo ci
    ) {
        if (this.entriesCanBeSelected()) {
            AbstractSelectionList<?> self = (AbstractSelectionList<?>) (Object) this;
            Object selectedEntry = self.getSelected();
            boolean selected = Objects.equals(selectedEntry, entry);
            boolean hovered = entry.getX() <= mouseX && mouseX <= entry.getX() + entry.getWidth()
                    && entry.getY() <= mouseY && mouseY <= entry.getY() + entry.getHeight();
            SeaClientTheme.renderListEntryCard(
                    graphics,
                    entry.getX(),
                    entry.getY(),
                    entry.getWidth(),
                    entry.getHeight(),
                    selected,
                    hovered
            );
        }
    }
}
