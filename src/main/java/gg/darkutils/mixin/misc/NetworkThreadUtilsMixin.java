package gg.darkutils.mixin.misc;

import com.llamalad7.mixinextras.sugar.Local;
import gg.darkutils.events.ReceiveMainThreadPacketEvent;
import gg.darkutils.events.base.EventRegistry;
import net.minecraft.network.NetworkThreadUtils;
import net.minecraft.network.packet.Packet;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(NetworkThreadUtils.class)
final class NetworkThreadUtilsMixin {
    private NetworkThreadUtilsMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @ModifyArg(method = "forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/util/thread/ThreadExecutor;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/thread/ThreadExecutor;executeSync(Ljava/lang/Runnable;)V"))
    private static final @NotNull Runnable darkutils$onPacketReceiveInMainThread(final @NotNull Runnable originalHandler, @Local(argsOnly = true) final @NotNull Packet<?> packet) {
        return () -> {
            EventRegistry.centralRegistry().triggerEvent(new ReceiveMainThreadPacketEvent(packet));
            originalHandler.run();
        };
    }
}
