package gg.darkutils.mixin.misc;

import gg.darkutils.events.ReceiveMainThreadPacketEvent;
import gg.darkutils.events.base.EventRegistry;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.network.PacketApplyBatcher$Entry")
final class PacketApplyBatcherEntryMixin<T extends PacketListener> {
    @Shadow
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
        if (EventRegistry.centralRegistry().triggerEvent(new ReceiveMainThreadPacketEvent(this.packet)).isCancelled()) {
            ci.cancel(); // prevent executing original packet handler
        }
    }
}
