package gg.darkutils.mixin.qol;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.mixinquirks.HolderFields;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
final class ConnectionMixin {
    private ConnectionMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Inject(method = "sendPacket", at = @At("HEAD"), cancellable = true)
    private final void darkutils$cancelSendPacketIfNotEnabled(@NotNull final Packet<?> packet, @NotNull final ChannelFutureListener listener, final boolean flush, @NotNull final CallbackInfo ci) {
        if (!DarkUtilsConfig.INSTANCE.enableModAnnouncer && packet instanceof ServerboundCustomPayloadPacket(
                @NotNull final CustomPacketPayload payload
        ) && HolderFields.FirmamentValues.MOD_LIST_IDENTIFIER.equals(payload.type().id())) {
            ci.cancel();
        }
    }
}

