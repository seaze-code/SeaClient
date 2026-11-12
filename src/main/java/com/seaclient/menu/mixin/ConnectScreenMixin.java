package com.seaclient.menu.mixin;

import com.seaclient.menu.data.LastPlayedTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.TransferState;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConnectScreen.class)
public abstract class ConnectScreenMixin {

    @Inject(method = "startConnecting", at = @At("HEAD"))
    private static void seaclient$onStartConnecting(
            Screen parent,
            Minecraft minecraft,
            ServerAddress address,
            ServerData serverData,
            boolean quickPlay,
            TransferState transferState,
            CallbackInfo ci
    ) {
        String ip = (serverData != null && serverData.ip != null && !serverData.ip.isBlank())
                ? serverData.ip
                : (address != null ? address.getHost() : null);
        if (ip != null && !ip.isBlank()) {
            String displayName = (serverData != null && serverData.name != null && !serverData.name.isBlank() && !"Minecraft Server".equals(serverData.name))
                    ? serverData.name
                    : ip;
            byte[] iconBytes = (serverData != null) ? serverData.getIconBytes() : null;
            LastPlayedTracker.recordServerJoin(minecraft, displayName, ip, iconBytes);
        }
    }
}
