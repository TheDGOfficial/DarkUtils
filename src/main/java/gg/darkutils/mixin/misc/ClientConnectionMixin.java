package gg.darkutils.mixin.misc;

import gg.darkutils.events.ReceivePacketEvent;
import gg.darkutils.events.ServerTickEvent;
import gg.darkutils.events.base.EventRegistry;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
final class ClientConnectionMixin {
    private ClientConnectionMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Inject(method = "handlePacket", at = @At("HEAD"), cancellable = true)
    private static final void darkutils$handlePacket$cancelIfEnabled(@NotNull final Packet<?> packet, @NotNull final PacketListener listener, @NotNull final CallbackInfo ci) {
        if (packet instanceof CommonPingS2CPacket) {
            EventRegistry.centralRegistry().triggerEvent(ServerTickEvent.INSTANCE);
        }

        if (EventRegistry.centralRegistry().triggerEvent(new ReceivePacketEvent(packet)).isCancelled()) {
            ci.cancel();
        }
    }
}
