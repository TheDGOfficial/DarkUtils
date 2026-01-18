package gg.darkutils.mixin.misc;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.events.OpenScreenEvent;
import gg.darkutils.events.SentCommandEvent;
import gg.darkutils.events.SentMessageEvent;
import gg.darkutils.events.base.EventRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.client.network.ClientConnectionState;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
abstract class ClientPlayNetworkHandlerMixin extends ClientCommonNetworkHandler {
    private ClientPlayNetworkHandlerMixin(@NotNull final MinecraftClient client, @NotNull final ClientConnection connection, @NotNull final ClientConnectionState connectionState) {
        super(client, connection, connectionState);

        throw new UnsupportedOperationException("mixin class");
    }

    @Inject(method = "sendChatMessage", at = @At("HEAD"))
    private final void darkutils$onMessage(@NotNull final String content, @NotNull final CallbackInfo ci) {
        new SentMessageEvent(content).trigger();
    }

    @Inject(method = "sendChatCommand", at = @At("HEAD"))
    private final void darkutils$onCommand(@NotNull final String command, @NotNull final CallbackInfo ci) {
        new SentCommandEvent(command).trigger();
    }

    @Inject(
            method = "onOpenScreen",
            at = @At("HEAD"),
            cancellable = true
    )
    private final void darkutils$cancelOpenScreenIfEnabled(@NotNull final OpenScreenS2CPacket packet, @NotNull final CallbackInfo ci) {
        if (new OpenScreenEvent(packet.getScreenHandlerType(), packet.getName()).triggerAndCancelled()) {
            // syncId from the OpenScreenS2CPacket tells server which container to close
            this.sendPacket(new CloseHandledScreenC2SPacket(packet.getSyncId()));

            // stop vanilla from opening the screen
            ci.cancel();
        }
    }

    @Shadow
    private final void openConfirmRunCommandScreen(@NotNull final String command, @NotNull final String message, @Nullable final Screen screenAfterRun) {
        throw new IllegalStateException("shadow failed");
    }

    @Redirect(method = "runClickEventCommand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;openConfirmRunCommandScreen(Ljava/lang/String;Ljava/lang/String;Lnet/minecraft/client/gui/screen/Screen;)V"))
    private final void darkutils$openConfirmCommandScreen$disableIfEnabled(@NotNull final ClientPlayNetworkHandler handler, @NotNull final String command, @NotNull final String message, @NotNull final Screen afterActionScreen) {
        if (DarkUtilsConfig.INSTANCE.disableCommandConfirmation && message.contains("parse_errors")) {
            handler.sendPacket(new CommandExecutionC2SPacket(command));
            MinecraftClient.getInstance().setScreen(afterActionScreen);
        } else {
            this.openConfirmRunCommandScreen(command, message, afterActionScreen);
        }
    }
}
