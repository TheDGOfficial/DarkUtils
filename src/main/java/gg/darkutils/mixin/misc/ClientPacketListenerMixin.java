package gg.darkutils.mixin.misc;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.events.OpenScreenEvent;
import gg.darkutils.events.SentCommandEvent;
import gg.darkutils.events.SentMessageEvent;
import gg.darkutils.feat.qol.BackpackPreview;
import gg.darkutils.utils.LocationUtils;
import gg.darkutils.utils.SkyblockProfileUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
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

    @WrapWithCondition(method = "handleSetEntityPassengersPacket", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;)V"))
    private final boolean darkutils$logUnknownVehicle(@NotNull final Logger logger, @NotNull final String message) {
        return !DarkUtilsConfig.INSTANCE.suppressLogspam || !LocationUtils.isInHypixel();
    }

    // The entity is already spawned. The rest of this branch only records seen-player history.
    @Inject(method = "handleAddEntity", at = @At(value = "FIELD", target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;playerInfoMap:Ljava/util/Map;"), cancellable = true)
    private final void darkutils$skipSeenPlayerHistory(@NotNull final CallbackInfo ci) {
        if (DarkUtilsConfig.INSTANCE.disableSocialInteractions) {
            ci.cancel();
        }
    }

    @WrapWithCondition(method = "handlePlayerInfoUpdate", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V"))
    private final boolean darkutils$logUnknownPlayerInfo(@NotNull final Logger logger, @NotNull final String message, @NotNull final Object player, @NotNull final Object action) {
        return !DarkUtilsConfig.INSTANCE.suppressLogspam || !LocationUtils.isInHypixel();
    }

    @WrapWithCondition(method = "handleUnknownCustomPayload", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;)V"))
    private final boolean darkutils$logUnknownPayload(@NotNull final Logger logger, @NotNull final String message, @NotNull final Object id) {
        return !(DarkUtilsConfig.INSTANCE.suppressLogspam && LocationUtils.isInHypixel()
                && id instanceof final Identifier identifier && identifier.getNamespace().equals("badlion"));
    }

    @WrapWithCondition(method = {"handleSetScore", "handleResetScore"}, at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;)V"), require = 2)
    private final boolean darkutils$logUnknownScoreboardObjective(@NotNull final Logger logger, @NotNull final String message, @NotNull final Object objective) {
        return !DarkUtilsConfig.INSTANCE.suppressLogspam || !LocationUtils.isInHypixel();
    }

    @WrapWithCondition(method = "handleSetPlayerTeamPacket", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;[Ljava/lang/Object;)V"))
    private final boolean darkutils$logUnknownTeam(@NotNull final Logger logger, @NotNull final String message, @NotNull final Object[] args) {
        return !DarkUtilsConfig.INSTANCE.suppressLogspam || !LocationUtils.isInHypixel();
    }

    @Inject(method = "sendChat", at = @At("HEAD"))
    private final void darkutils$onMessage(@NotNull final String content, @NotNull final CallbackInfo ci) {
        new SentMessageEvent(content).trigger();
    }

    @Inject(method = "sendCommand", at = @At("HEAD"))
    private final void darkutils$onCommand(@NotNull final String command, @NotNull final CallbackInfo ci) {
        new SentCommandEvent(command).trigger();
    }

    @Inject(method = "handleContainerContent", at = @At("TAIL"))
    private final void darkutils$onStorageContents(@NotNull final ClientboundContainerSetContentPacket packet, @NotNull final CallbackInfo ci) {
        if (DarkUtilsConfig.INSTANCE.backpackPreview && LocationUtils.isInSkyblock()
                && this.minecraft.gui.screen() instanceof final ContainerScreen screen
                && screen.getMenu().containerId == packet.containerId()
                && packet.items().size() >= screen.getMenu().getRowCount() * 9) {
            BackpackPreview.onContents(screen, (ClientPacketListener) (Object) this);
        }
    }

    @Inject(method = "handleSystemChat", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/chat/ChatListener;handleSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"), cancellable = true)
    private final void darkutils$onProfileId(@NotNull final ClientboundSystemChatPacket packet, @NotNull final CallbackInfo ci) {
        if (DarkUtilsConfig.INSTANCE.backpackPreview && LocationUtils.isInSkyblock()
                && SkyblockProfileUtils.onSystemChat(packet.content(), (ClientPacketListener) (Object) this)) {
            ci.cancel();
        }
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

    @Inject(method = "handleOpenScreen", at = @At("TAIL"))
    private final void darkutils$onStorageOpen(@NotNull final ClientboundOpenScreenPacket packet, @NotNull final CallbackInfo ci) {
        if (DarkUtilsConfig.INSTANCE.backpackPreview && LocationUtils.isInSkyblock()
                && this.minecraft.gui.screen() instanceof final ContainerScreen screen
                && screen.getMenu().containerId == packet.getContainerId()) {
            BackpackPreview.onOpen(screen, (ClientPacketListener) (Object) this);
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
            Minecraft.getInstance().gui.setScreen(afterActionScreen);
        } else {
            this.openCommandSendConfirmationWindow(command, message, afterActionScreen);
        }
    }
}
