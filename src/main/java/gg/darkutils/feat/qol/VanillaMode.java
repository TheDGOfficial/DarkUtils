package gg.darkutils.feat.qol;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.events.ConfigSaveFinishEvent;
import gg.darkutils.events.ConfigSaveStartEvent;
import gg.darkutils.events.ConfigScreenOpenEvent;
import gg.darkutils.events.base.EventRegistry;
import gg.darkutils.utils.LocationUtils;
import gg.darkutils.utils.TickUtils;
import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.function.UnaryOperator;

public final class VanillaMode {
    @NotNull
    private static final Set<UnaryOperator<VanillaMode.ConfigValueState>> changers = Set.of(
            // Vanilla singleplayer usually needs to be able to see which places
            // are dark to be able to know where to place torches to prevent mob
            // spawns for example, or to know which places to not go such as deep
            // caves without a proper light source, so those need disabling.
            state -> VanillaMode.ConfigValueState.of(DarkUtilsConfig.INSTANCE.stopLightUpdates, DarkUtilsConfig.INSTANCE.stopLightUpdates = state.currentOrNewState()),
            state -> VanillaMode.ConfigValueState.of(DarkUtilsConfig.INSTANCE.fullbright, DarkUtilsConfig.INSTANCE.fullbright = state.currentOrNewState()),
            state -> VanillaMode.ConfigValueState.of(DarkUtilsConfig.INSTANCE.nightVision, DarkUtilsConfig.INSTANCE.nightVision = state.currentOrNewState()),

            // Need to be able to see effects
            state -> VanillaMode.ConfigValueState.of(DarkUtilsConfig.INSTANCE.hideEffectsHud, DarkUtilsConfig.INSTANCE.hideEffectsHud = state.currentOrNewState()),
            state -> VanillaMode.ConfigValueState.of(DarkUtilsConfig.INSTANCE.hideEffectsInInventory, DarkUtilsConfig.INSTANCE.hideEffectsInInventory = state.currentOrNewState()),
            state -> VanillaMode.ConfigValueState.of(DarkUtilsConfig.INSTANCE.noWitherHearts, DarkUtilsConfig.INSTANCE.noWitherHearts = state.currentOrNewState()),

            // Need to be able to see food and armor points, along with mount health
            state -> VanillaMode.ConfigValueState.of(DarkUtilsConfig.INSTANCE.hideArmorAndFood, DarkUtilsConfig.INSTANCE.hideArmorAndFood = state.currentOrNewState()),
            state -> VanillaMode.ConfigValueState.of(DarkUtilsConfig.INSTANCE.hideMountHealth, DarkUtilsConfig.INSTANCE.hideMountHealth = state.currentOrNewState()),

            // Need to be aware of environmental events
            state -> VanillaMode.ConfigValueState.of(DarkUtilsConfig.INSTANCE.noLightningBolts, DarkUtilsConfig.INSTANCE.noLightningBolts = state.currentOrNewState()),

            // Need to be aware of fire and lava
            state -> VanillaMode.ConfigValueState.of(DarkUtilsConfig.INSTANCE.noBurningEntities, DarkUtilsConfig.INSTANCE.noBurningEntities = state.currentOrNewState()),
            state -> VanillaMode.ConfigValueState.of(DarkUtilsConfig.INSTANCE.hideFireOverlay, DarkUtilsConfig.INSTANCE.hideFireOverlay = state.currentOrNewState())
    );

    @Nullable
    private static Set<Runnable> reverters;

    private VanillaMode() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    private static final void registerAwaitJoin() {
        TickUtils.awaitCondition(LocationUtils::isInSingleplayer, VanillaMode::onJoinSingleplayer);
    }

    private static final void registerAwaitLeave() {
        TickUtils.awaitNegatedCondition(LocationUtils::isInSingleplayer, VanillaMode::onLeaveSingleplayer);
    }

    public static final void init() {
        EventRegistry.centralRegistry().addListener(VanillaMode::onConfigSaveStart);
        EventRegistry.centralRegistry().addListener(VanillaMode::onConfigSaveFinish);
        EventRegistry.centralRegistry().addListener(VanillaMode::onConfigScreenOpen);

        VanillaMode.registerAwaitJoin();
    }

    private static final void onConfigSaveStart(@NotNull final ConfigSaveStartEvent event) {
        // Ensure the temporarily changed values not get saved to config in disk.
        VanillaMode.onLeaveSingleplayer(false);
    }

    private static final void onConfigSaveFinish(@NotNull final ConfigSaveFinishEvent event) {
        // After the config gets saved, enable temporary changes again if the player is in singleplayer.
        VanillaMode.registerAwaitJoin();
    }

    private static final void onConfigScreenOpen(@NotNull final ConfigScreenOpenEvent event) {
        // Ensure the user does not see the temporarily changed values, and can change their original settings freely and correctly.
        VanillaMode.onLeaveSingleplayer(false);

        // Re-apply the temporary changes when the user closes the config screen.

        // queueTickTask with 1 tick delay is necessary as awaitCondition runs the action instantly if the condition is true to start with.
        // The condition is true in current tick because the event triggers before opening the config screen, not after, but will be false
        // in the next tick till the user closes the menu. Otherwise, onJoinSingleplayer would run instantly causing the temporary values to show.
        TickUtils.queueTickTask(() -> TickUtils.awaitCondition(VanillaMode::isNoScreenOpen, VanillaMode::registerAwaitJoin), 1);
    }

    private static final boolean isNoScreenOpen() {
        return null == MinecraftClient.getInstance().currentScreen;
    }

    private static final void onJoinSingleplayer() {
        if (!DarkUtilsConfig.INSTANCE.vanillaMode || null != VanillaMode.reverters) {
            return;
        }

        final var changersLocal = VanillaMode.changers;
        final var revertersNew = HashSet.<Runnable>newHashSet(changersLocal.size());

        for (final var changer : changersLocal) {
            final var wasEnabled = changer.apply(VanillaMode.ConfigValueState.of(false)).oldState();
            if (wasEnabled) {
                revertersNew.add(() -> changer.apply(VanillaMode.ConfigValueState.of(true)));
            }
        }

        VanillaMode.reverters = Set.copyOf(revertersNew);
        VanillaMode.registerAwaitLeave();
    }

    private static final void onLeaveSingleplayer() {
        VanillaMode.onLeaveSingleplayer(true);
    }

    private static final void onLeaveSingleplayer(final boolean registerAwaitJoin) {
        if (!DarkUtilsConfig.INSTANCE.vanillaMode) {
            return;
        }

        final var revertersLocal = VanillaMode.reverters;

        if (null == revertersLocal) {
            return;
        }

        VanillaMode.reverters = null;

        for (final var reverter : revertersLocal) {
            reverter.run();
        }

        if (registerAwaitJoin) {
            VanillaMode.registerAwaitJoin();
        }
    }

    private sealed interface ConfigValueState permits VanillaMode.CurrentConfigValueState, VanillaMode.OldNewConfigValueState {
        @NotNull
        private static VanillaMode.ConfigValueState of(final boolean oldState, final boolean currentOrNewState) {
            return new VanillaMode.OldNewConfigValueState(oldState, currentOrNewState);
        }

        @NotNull
        private static VanillaMode.ConfigValueState of(final boolean currentOrNewState) {
            return new VanillaMode.CurrentConfigValueState(currentOrNewState);
        }

        boolean oldState();

        boolean currentOrNewState();
    }

    private static record CurrentConfigValueState(boolean currentOrNewState) implements VanillaMode.ConfigValueState {
        @Override
        public final boolean oldState() {
            throw new UnsupportedOperationException("oldState() call in a CurrentConfigValueState is not supported");
        }
    }

    private static record OldNewConfigValueState(boolean oldState,
                                                 boolean currentOrNewState) implements VanillaMode.ConfigValueState {
    }
}
