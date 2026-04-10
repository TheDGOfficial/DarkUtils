package gg.darkutils.mixin.qol;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.mixinquirks.HolderFields;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
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

    @Inject(method = "sendImmediately", at = @At("HEAD"), cancellable = true)
    private final void darkutils$cancelSendPacketIfNotEnabled(@NotNull final Packet<?> packet, @NotNull final ChannelFutureListener listener, final boolean flush, @NotNull final CallbackInfo ci) {
        if (!DarkUtilsConfig.INSTANCE.enableModAnnouncer && packet instanceof CustomPayloadC2SPacket(
                @NotNull final CustomPayload payload
        ) && HolderFields.FirmamentValues.MOD_LIST_IDENTIFIER.equals(payload.getId().id())) {
            ci.cancel();
        }
    }
}

