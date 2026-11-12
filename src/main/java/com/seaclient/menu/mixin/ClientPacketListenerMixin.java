package com.seaclient.menu.mixin;

import com.seaclient.menu.hud.SeaClientPvpTracker;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundDisguisedChatPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

    @Inject(method = "handleSystemChat", at = @At("TAIL"))
    private void seaclient$onSystemChat(ClientboundSystemChatPacket packet, CallbackInfo ci) {
        if (packet != null && packet.content() != null) {
            SeaClientPvpTracker.onChatMessage(packet.content().getString());
        }
    }

    @Inject(method = "handleDisguisedChat", at = @At("TAIL"))
    private void seaclient$onDisguisedChat(ClientboundDisguisedChatPacket packet, CallbackInfo ci) {
        if (packet != null && packet.message() != null) {
            SeaClientPvpTracker.onChatMessage(packet.message().getString());
        }
    }
}
