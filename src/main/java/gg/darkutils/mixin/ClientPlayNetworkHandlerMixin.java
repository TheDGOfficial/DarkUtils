package gg.darkutils.mixin;

import gg.darkutils.feat.dungeons.AutoCloseSecretChests;
import gg.darkutils.utils.ChatUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
final class ClientPlayNetworkHandlerMixin {
    private ClientPlayNetworkHandlerMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Inject(method = "sendChatMessage", at = @At("HEAD"))
    private final void darkutils$onMessage(@NotNull final String content, @NotNull final CallbackInfo ci) {
        ChatUtils.lastSentMessageOrCommandAt = System.nanoTime();
    }

    @Inject(method = "sendChatCommand", at = @At("HEAD"))
    private final void darkutils$onCommand(@NotNull final String command, @NotNull final CallbackInfo ci) {
        ChatUtils.lastSentMessageOrCommandAt = System.nanoTime();
    }

    @Inject(
            method = "onOpenScreen",
            at = @At("HEAD"),
            cancellable = true
    )
    private final void darkutils$cancelOpenScreenIfEnabled(@NotNull final OpenScreenS2CPacket packet, @NotNull final CallbackInfo ci) {
        if (AutoCloseSecretChests.shouldCancelOpen(packet)) {
            final var client = MinecraftClient.getInstance();
            if (null != client && null != client.getNetworkHandler()) {
                // syncId from the OpenScreenS2CPacket tells server which container to close
                client.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(packet.getSyncId()));

                // stop vanilla from opening the screen
                ci.cancel();
            }
        }
    }
}
