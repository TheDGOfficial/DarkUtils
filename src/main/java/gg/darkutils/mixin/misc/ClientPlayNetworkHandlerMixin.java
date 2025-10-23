package gg.darkutils.mixin.misc;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.events.ScreenOpenEvent;
import gg.darkutils.events.base.EventRegistry;
import gg.darkutils.utils.chat.ChatUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
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
        if (EventRegistry.centralRegistry().triggerEvent(new ScreenOpenEvent(packet.getScreenHandlerType(), packet.getName())).isCancelled()) {
            final var client = MinecraftClient.getInstance();
            if (null != client && null != client.getNetworkHandler()) {
                // syncId from the OpenScreenS2CPacket tells server which container to close
                client.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(packet.getSyncId()));

                // stop vanilla from opening the screen
                ci.cancel();
            }
        }
    }

    @Shadow
    private final void openConfirmCommandScreen(@NotNull final String command, @NotNull final String message, @Nullable final Screen screenAfterRun) {
        throw new IllegalStateException("shadow failed");
    }

    @Redirect(method = "runClickEventCommand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;openConfirmCommandScreen(Ljava/lang/String;Ljava/lang/String;Lnet/minecraft/client/gui/screen/Screen;)V"))
    private final void darkutils$openConfirmCommandScreen$disableIfEnabled(@NotNull final ClientPlayNetworkHandler handler, @NotNull final String command, @NotNull final String message, @NotNull final Screen afterActionScreen) {
        if (DarkUtilsConfig.INSTANCE.disableCommandConfirmation && message.contains("parse_errors")) {
            handler.sendPacket(new CommandExecutionC2SPacket(command));
            MinecraftClient.getInstance().setScreen(afterActionScreen);
        } else {
            this.openConfirmCommandScreen(command, message, afterActionScreen);
        }
    }
}
