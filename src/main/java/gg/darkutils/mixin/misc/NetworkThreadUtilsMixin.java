package gg.darkutils.mixin.misc;

import gg.darkutils.events.ReceiveMainThreadPacketEvent;
import gg.darkutils.events.base.EventRegistry;
import net.minecraft.network.NetworkThreadUtils;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetworkThreadUtils.class)
final class NetworkThreadUtilsMixin {
    private NetworkThreadUtilsMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Inject(
            method = "method_11072(Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/packet/Packet;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/packet/Packet;apply(Lnet/minecraft/network/listener/PacketListener;)V"
            ),
            cancellable = true
    )
    private static final void darkutils$beforeMainThreadPacket(
            final @NotNull PacketListener listener,
            final @NotNull Packet<?> packet,
            final @NotNull CallbackInfo ci
    ) {
        if (EventRegistry.centralRegistry().triggerEvent(new ReceiveMainThreadPacketEvent(packet)).isCancelled()) {
            ci.cancel(); // prevent executing original packet handler
        }
    }
}
