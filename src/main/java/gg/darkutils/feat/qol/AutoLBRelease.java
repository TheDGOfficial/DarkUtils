package gg.darkutils.feat.qol;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.multiplayer.ClientLevel;

import gg.darkutils.utils.TickUtils;
import gg.darkutils.utils.Helpers;
import gg.darkutils.config.DarkUtilsConfig;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class AutoLBRelease {
    private static boolean isCharging;
    private static int lbChargedTicks;

    private AutoLBRelease() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final void init() {
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register(AutoLBRelease::onWorldChange);

        TickUtils.queueRepeatingServerTickTask(AutoLBRelease::onServerTick, 1);
    }

    private static final void resetState() {
        AutoLBRelease.isCharging = false;
        AutoLBRelease.lbChargedTicks = 0;
    }

    private static final void onWorldChange(@NotNull final Minecraft client, @Nullable final ClientLevel world) {
        AutoLBRelease.resetState();
    }

    private static final boolean isEnabled() {
        return DarkUtilsConfig.INSTANCE.autoLBRelease;
    }

    private static final void onServerTick() {
        if (AutoLBRelease.isEnabled() && AutoLBRelease.isCharging && Helpers.isHoldingALastBreath()) {
            ++AutoLBRelease.lbChargedTicks;
        }
    }

    public static final boolean isDown(@NotNull final KeyMapping keyMapping, final boolean actual) {
        final var rc = Minecraft.getInstance().options.keyUse == keyMapping;

        if (!rc) {
            return actual;
        }

        if (!AutoLBRelease.isEnabled() || !actual || !Helpers.isHoldingALastBreath()) {
            AutoLBRelease.resetState();
            return actual;
        }

        if (AutoLBRelease.lbChargedTicks >= 8) {
            AutoLBRelease.resetState();
            return false;
        }

        AutoLBRelease.isCharging = true;
        return true;
    }
}
