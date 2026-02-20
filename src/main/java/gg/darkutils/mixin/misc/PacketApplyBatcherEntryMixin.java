package gg.darkutils.mixin.misc;

import gg.darkutils.DarkUtils;
import gg.darkutils.events.ReceiveMainThreadPacketEvent;
import gg.darkutils.events.ServerTickEvent;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.network.PacketApplyBatcher$Entry")
final class PacketApplyBatcherEntryMixin<T extends PacketListener> {
    @Unique
    private static int lastId;
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
            method = "apply",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/packet/Packet;apply(Lnet/minecraft/network/listener/PacketListener;)V"
            ),
            cancellable = true
    )
    private final void darkutils$beforeMainThreadPacket(@NotNull final CallbackInfo ci) {
        if (NetworkSide.CLIENTBOUND != this.listener.getSide()) {
            // Integrated server packet. We're on Server thread, the instanceof will always be false, and ReceiveMainThreadPacketEvent expects Client (Render) thread, so just do nothing and return.
            return;
        }

        final var packet = this.packet;

        if (packet instanceof final CommonPingS2CPacket p) {
            final var id = p.getParameter();

            if (0 > id && id != PacketApplyBatcherEntryMixin.lastId) {
                PacketApplyBatcherEntryMixin.lastId = id;

                ServerTickEvent.INSTANCE.trigger();
            }
        }

        if (new ReceiveMainThreadPacketEvent(packet).triggerAndCancelled()) {
            ci.cancel(); // prevent executing original packet handler
        }
    }
}
