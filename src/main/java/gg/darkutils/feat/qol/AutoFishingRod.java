package gg.darkutils.feat.qol;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.mixin.accessors.MinecraftClientAccessor;
import gg.darkutils.utils.TickUtils;
import gg.darkutils.utils.chat.ChatUtils;
import gg.darkutils.utils.chat.SimpleColor;
import gg.darkutils.utils.chat.SimpleFormatting;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.security.SecureRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AutoFishingRod {
    private static final @NotNull String READY = "!!!";
    private static final @NotNull Matcher COUNTDOWN_MATCHER =
            Pattern.compile("(\\d+(\\.\\d+)?)").matcher("");
    private static final @NotNull SecureRandom SECURE_RANDOM = new SecureRandom();

    private static @Nullable WeakReference<ArmorStandEntity> countdownArmorStand;
    private static boolean hooking;

    private AutoFishingRod() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final void init() {
        // Runs tick method every client tick
        ClientTickEvents.END_CLIENT_TICK.register(AutoFishingRod::tick);

        // Resets cached armor stand + state when world changes
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register(AutoFishingRod::onWorldChange);

        // Resets state when a new bobber owned by us spawns
        ClientEntityEvents.ENTITY_LOAD.register(AutoFishingRod::onEntityJoinWorld);
    }

    private static final void onWorldChange(@NotNull final MinecraftClient client, @NotNull final ClientWorld world) {
        AutoFishingRod.resetState();
    }

    private static final void onEntityJoinWorld(@NotNull final Entity entity, @NotNull final ClientWorld world) {
        if (!DarkUtilsConfig.INSTANCE.autoFishing) {
            return;
        }

        final var client = MinecraftClient.getInstance();
        final var player = client.player;

        if (null != player && entity instanceof final FishingBobberEntity bobber && bobber.getOwner() == player) {
            AutoFishingRod.resetState();
        }
    }

    private static final boolean isCountdownArmorStand(@Nullable final Text customName) {
        return null != customName && AutoFishingRod.COUNTDOWN_MATCHER.reset(customName.getString()).matches() && ChatUtils.hasFormatting(customName, SimpleColor.YELLOW, SimpleFormatting.BOLD);
    }

    private static final boolean isNotHoldingRod(@Nullable final ClientPlayerEntity player) {
        return null == player
                || !player.getMainHandStack().isOf(Items.FISHING_ROD);
    }

    @Nullable
    private static final ArmorStandEntity findAndAssignCountdownArmorStand(@NotNull final MinecraftClient client) {
        final var world = client.world;
        if (null != world && AutoFishingRod.hasActiveBobber(client)) {
            for (final var entity : world.getEntities()) {
                if (entity instanceof final ArmorStandEntity stand && AutoFishingRod.isCountdownArmorStand(stand.getCustomName())) {
                    AutoFishingRod.countdownArmorStand = new WeakReference<>(stand);
                    return stand;
                }
            }
        }
        return null;
    }

    private static final @Nullable ArmorStandEntity getOrFindCountdownArmorStand(@NotNull final MinecraftClient client) {
        if (null == AutoFishingRod.countdownArmorStand) {
            return AutoFishingRod.findAndAssignCountdownArmorStand(client);
        }
        final var cached = AutoFishingRod.countdownArmorStand.get();
        return null == cached ? AutoFishingRod.findAndAssignCountdownArmorStand(client) : cached;
    }

    private static final boolean hasActiveBobber(final MinecraftClient client) {
        final var player = client.player;
        final var world = client.world;
        if (null == world || AutoFishingRod.isNotHoldingRod(player)) {
            return false;
        }

        for (final var entity : world.getEntities()) {
            if (entity instanceof final FishingBobberEntity bobber && bobber.getOwner() == player) {
                return true;
            }
        }
        return false;
    }

    private static final void hook(@NotNull final MinecraftClient client) {
        if (AutoFishingRod.hooking || null == client.player) {
            return;
        }

        AutoFishingRod.hooking = true;
        AutoFishingRod.hookAndReThrow();
    }

    private static final void hookAndReThrow() {
        AutoFishingRod.useRod(() -> {
            if (DarkUtilsConfig.INSTANCE.autoFishingRecast) {
                AutoFishingRod.useRod(AutoFishingRod::resetState);
            } else {
                AutoFishingRod.resetState();
            }
        });
    }

    private static final void useRod(@NotNull final Runnable continuation) {
        final var min = DarkUtilsConfig.INSTANCE.autoFishingStartingDelay;
        final var max = DarkUtilsConfig.INSTANCE.autoFishingMaximumDelay;

        final var delay = max > min
                ? AutoFishingRod.SECURE_RANDOM.nextInt(min, max + 1)
                : min;

        TickUtils.queueTickTask(() -> {
            final var mc = MinecraftClient.getInstance();
            if (DarkUtilsConfig.INSTANCE.autoFishingWorkThroughMenus || null == mc.currentScreen) {
                ((MinecraftClientAccessor) mc).callDoItemUse();
                continuation.run();
            }
        }, delay);
    }

    private static final void resetState() {
        if (null != AutoFishingRod.countdownArmorStand) {
            AutoFishingRod.countdownArmorStand.clear();
            AutoFishingRod.countdownArmorStand = null;
        }
        AutoFishingRod.hooking = false;
    }

    private static final void tick(@NotNull final MinecraftClient client) {
        if (!DarkUtilsConfig.INSTANCE.autoFishing) {
            AutoFishingRod.resetState();
            return;
        }

        final var player = client.player;

        if (null == client.world || null == player || AutoFishingRod.hooking || AutoFishingRod.isNotHoldingRod(player)) {
            return;
        }

        final var armorStand = AutoFishingRod.getOrFindCountdownArmorStand(client);
        if (null != armorStand) {
            final var customName = armorStand.getCustomName();
            if (null != customName && AutoFishingRod.READY.equals(customName.getString()) && ChatUtils.hasFormatting(customName, SimpleColor.RED, SimpleFormatting.BOLD)) {
                AutoFishingRod.hook(client);
            }
        }
    }
}
