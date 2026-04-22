package gg.darkutils.mixin.misc;

import gg.darkutils.events.ReceiveMainThreadPacketEvent;
import gg.darkutils.events.ServerTickEvent;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.network.PacketProcessor$ListenerAndPacket")
final class PacketApplyBatcherEntryMixin<T extends PacketListener> {
    @Unique
    private static int darkutils$lastId;
    @Shadow
    @Final
    @NotNull
    private T listener;
    @Shadow
    @Final
    @NotNull
    private Packet<T> packet;

    private PacketApplyBatcherEntryMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Inject(
            method = "handle",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/protocol/Packet;handle(Lnet/minecraft/network/PacketListener;)V"
            ),
            cancellable = true
    )
    private final void darkutils$beforeMainThreadPacket(@NotNull final CallbackInfo ci) {
        if (PacketFlow.CLIENTBOUND != this.listener.flow()) {
            // Integrated server packet. We're on Server thread, the instanceof will always be false, and ReceiveMainThreadPacketEvent expects Client (Render) thread, so just do nothing and return.
            return;
        }

        final var packet = this.packet;

        if (packet instanceof final ClientboundPingPacket p) {
            final var id = p.getId();

            if (0 > id && id != PacketApplyBatcherEntryMixin.darkutils$lastId) {
                PacketApplyBatcherEntryMixin.darkutils$lastId = id;

                ServerTickEvent.INSTANCE.trigger();
            }
        }

        if (new ReceiveMainThreadPacketEvent(packet).triggerAndCancelled()) {
            ci.cancel(); // prevent executing original packet handler
        }
    }
}
