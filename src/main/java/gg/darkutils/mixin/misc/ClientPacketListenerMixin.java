package gg.darkutils.mixin.misc;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.events.OpenScreenEvent;
import gg.darkutils.events.SentCommandEvent;
import gg.darkutils.events.SentMessageEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
abstract class ClientPacketListenerMixin extends ClientCommonPacketListenerImpl {
    private ClientPacketListenerMixin(@NotNull final Minecraft minecraft, @NotNull final Connection connection, @NotNull final CommonListenerCookie commonListenerCookie) {
        super(minecraft, connection, commonListenerCookie);

        throw new UnsupportedOperationException("mixin class");
    }

    @Inject(method = "sendChat", at = @At("HEAD"))
    private final void darkutils$onMessage(@NotNull final String content, @NotNull final CallbackInfo ci) {
        new SentMessageEvent(content).trigger();
    }

    @Inject(method = "sendCommand", at = @At("HEAD"))
    private final void darkutils$onCommand(@NotNull final String command, @NotNull final CallbackInfo ci) {
        new SentCommandEvent(command).trigger();
    }

    @Inject(
            method = "handleOpenScreen",
            at = @At("HEAD"),
            cancellable = true
    )
    private final void darkutils$cancelOpenScreenIfEnabled(@NotNull final ClientboundOpenScreenPacket packet, @NotNull final CallbackInfo ci) {
        if (new OpenScreenEvent(packet.getType(), packet.getTitle()).triggerAndCancelled()) {
            // syncId from the OpenScreenS2CPacket tells server which container to close
            this.send(new ServerboundContainerClosePacket(packet.getContainerId()));

            // stop vanilla from opening the screen
            ci.cancel();
        }
    }

    @Shadow
    private final void openCommandSendConfirmationWindow(@NotNull final String command, @NotNull final String message, @Nullable final Screen screenAfterRun) {
        throw new IllegalStateException("shadow failed");
    }

    @Redirect(method = "sendUnattendedCommand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;openCommandSendConfirmationWindow(Ljava/lang/String;Ljava/lang/String;Lnet/minecraft/client/gui/screens/Screen;)V"))
    private final void darkutils$openConfirmCommandScreen$disableIfEnabled(@NotNull final ClientPacketListener handler, @NotNull final String command, @NotNull final String message, @NotNull final Screen afterActionScreen) {
        if (DarkUtilsConfig.INSTANCE.disableCommandConfirmation && message.contains("parse_errors")) {
            handler.send(new ServerboundChatCommandPacket(command));
            Minecraft.getInstance().setScreen(afterActionScreen);
        } else {
            this.openCommandSendConfirmationWindow(command, message, afterActionScreen);
        }
    }
}
