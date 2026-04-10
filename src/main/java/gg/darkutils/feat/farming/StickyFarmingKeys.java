package gg.darkutils.feat.farming;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.utils.ActivityState;
import gg.darkutils.utils.Helpers;
import gg.darkutils.utils.LocationUtils;
import gg.darkutils.utils.TickUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.world.ClientWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class StickyFarmingKeys {
    @Nullable
    private static Screen previousScreen;

    private static int previousSelectedSlot = -1;

    private StickyFarmingKeys() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final void init() {
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register(StickyFarmingKeys::onWorldChange);
        TickUtils.queueRepeatingTickTask(StickyFarmingKeys::onTick, 1);
    }

    private static final void onWorldChange(@NotNull final MinecraftClient client, @Nullable final ClientWorld world) {
        StickyFarmingKeys.resetToggledState();
    }

    private static final void onTick() {
        final var mc = MinecraftClient.getInstance();

        final var screen = mc.currentScreen;
        final var previousScreen = StickyFarmingKeys.previousScreen;

        var reset = false;

        if ((null != screen) == (null == previousScreen)) {
            // A screen was opened or closed. Reset toggled state.
            StickyFarmingKeys.resetToggledState();
            reset = true;
        }

        StickyFarmingKeys.previousScreen = screen;

        final var player = mc.player;

        if (null == player) {
            if (!reset) {
                StickyFarmingKeys.resetToggledState();
            }
        } else {
            final var currentSlot = player.getInventory().getSelectedSlot();
            final var previousSlot = StickyFarmingKeys.previousSelectedSlot;

            if (!reset && -1 != previousSlot && previousSlot != currentSlot) {
                StickyFarmingKeys.resetToggledState();
            }

            StickyFarmingKeys.previousSelectedSlot = currentSlot;
        }
    }

    private static final void resetToggledState() {
        for (final var key : StickyFarmingKeys.Key.VALUES) {
            key.setToggled(false);
        }

        for (final var key : StickyFarmingKeys.MovementKey.VALUES) {
            key.setToggled(false);
        }
    }

    public static final void resetState() {
        for (final var key : StickyFarmingKeys.Key.VALUES) {
            key.resetState();
        }

        for (final var key : StickyFarmingKeys.MovementKey.VALUES) {
            key.resetState();
        }
    }

    public static final boolean isPressed(@NotNull final KeyBinding keyBinding, final boolean playerInput) {
        final var actual = keyBinding.isPressed();

        StickyFarmingKeys.MovementKey movementKey = null;

        if (playerInput) {
            for (final var key : StickyFarmingKeys.MovementKey.VALUES) {
                if (key.isForKeyBinding(keyBinding)) {
                    movementKey = key;
                    key.updateToggle(actual);
                    break;
                }
            }
        }

        if (!DarkUtilsConfig.INSTANCE.stickyFarmingKeys || actual) {
            return actual;
        }

        if (playerInput) {
            if (null != movementKey) {
                return movementKey.isPressed(false);
            }
            // jump, sneak and sprint falls-thru to return false
        } else {
            for (final var key : StickyFarmingKeys.Key.VALUES) {
                if (key.isForKeyBinding(keyBinding)) {
                    return key.isPressed(false);
                }
            }
        }

        return false;
    }

    public static final boolean wasPressed(@NotNull final KeyBinding keyBinding) {
        final var actual = keyBinding.wasPressed();

        if (!DarkUtilsConfig.INSTANCE.stickyFarmingKeys || !actual) {
            return actual;
        }

        for (final var key : StickyFarmingKeys.Key.VALUES) {
            if (key.isForKeyBinding(keyBinding)) {
                return key.wasPressed(true);
            }
        }

        return true;
    }

    private enum MovementKey implements StickyFarmingKeys.CommonKey {
        Forward(MinecraftClient.getInstance().options.forwardKey), // Typically W
        Back(MinecraftClient.getInstance().options.backKey), // Typically S
        Left(MinecraftClient.getInstance().options.leftKey), // Typically A
        Right(MinecraftClient.getInstance().options.rightKey); // Typically D

        private static final StickyFarmingKeys.MovementKey @NotNull [] VALUES = StickyFarmingKeys.MovementKey.values();

        @NotNull
        private final KeyBinding keyBinding;

        private boolean toggled;
        private boolean clicking;

        private boolean lastPhysicalPressed;

        private MovementKey(@NotNull final KeyBinding keyBinding) {
            this.keyBinding = keyBinding;
        }

        private final boolean isStickyEnabled() {
            final var config = DarkUtilsConfig.INSTANCE;

            return switch (this) {
                case Forward -> config.stickyForward;
                case Back -> config.stickyBackward;
                case Left -> config.stickyLeft;
                case Right -> config.stickyRight;
            };
        }

        private final void updateToggle(final boolean physicalPressed) {
            if (!this.lastPhysicalPressed && physicalPressed) {
                // always clear others so the player regains control
                for (final var key : StickyFarmingKeys.MovementKey.VALUES) {
                    if (key != this) {
                        key.toggled = false;
                    }
                }

                // only toggle if this sticky key is enabled in config
                this.toggled = this.isStickyEnabled() && !this.toggled;
            }

            this.lastPhysicalPressed = physicalPressed;
        }

        @Override
        public final void resetState() {
            this.lastPhysicalPressed = false;

            StickyFarmingKeys.CommonKey.super.resetState();
        }

        @Override
        public final boolean isPressed(final boolean actual) {
            if (!this.isStickyEnabled()) {
                this.toggled = false;
                return actual;
            }

            return StickyFarmingKeys.CommonKey.super.isPressed(actual);
        }

        @NotNull
        @Override
        public final KeyBinding getKeyBinding() {
            return this.keyBinding;
        }

        @Override
        public final boolean isToggled() {
            return this.toggled;
        }

        @Override
        public final void setToggled(final boolean toggled) {
            this.toggled = toggled;
        }

        @Override
        public final boolean isClicking() {
            return this.clicking;
        }

        @Override
        public final void setClicking(final boolean clicking) {
            this.clicking = clicking;
        }
    }

    private enum Key implements StickyFarmingKeys.CommonKey {
        LMB(MinecraftClient.getInstance().options.attackKey);

        private static final StickyFarmingKeys.Key @NotNull [] VALUES = StickyFarmingKeys.Key.values();

        @NotNull
        private final KeyBinding keyBinding;

        private boolean toggled;
        private boolean clicking;

        private Key(@NotNull final KeyBinding keyBinding) {
            this.keyBinding = keyBinding;
        }

        @NotNull
        @Override
        public final KeyBinding getKeyBinding() {
            return this.keyBinding;
        }

        @Override
        public final boolean isToggled() {
            return this.toggled;
        }

        @Override
        public final void setToggled(final boolean toggled) {
            this.toggled = toggled;
        }

        @Override
        public final boolean isClicking() {
            return this.clicking;
        }

        @Override
        public final void setClicking(final boolean clicking) {
            this.clicking = clicking;
        }
    }

    private interface CommonKey {
        @NotNull
        KeyBinding getKeyBinding();

        boolean isToggled();

        void setToggled(final boolean toggled);

        boolean isClicking();

        void setClicking(final boolean clicking);

        default boolean isForKeyBinding(@NotNull final KeyBinding keyBinding) {
            return this.getKeyBinding() == keyBinding;
        }

        default void resetState() {
            this.setClicking(false);
        }

        default boolean isPressed(final boolean actual) {
            if (actual) {
                return true;
            }

            if (!ActivityState.isActivelyFarming() || !LocationUtils.isInGarden() || !Helpers.isHoldingADiamondHoeAxeOrSword()) {
                this.setToggled(false);
                return false;
            }

            if (this.isClicking()) {
                return false;
            }

            this.setClicking(true);

            return this.isToggled();
        }

        default boolean wasPressed(final boolean actual) {
            if (!ActivityState.isActivelyFarming() || !LocationUtils.isInGarden() || !Helpers.isHoldingADiamondHoeAxeOrSword()) {
                this.setToggled(false);
                return actual;
            }

            if (!actual) {
                return false;
            }

            this.setToggled(!this.isToggled());
            return true;
        }
    }
}
