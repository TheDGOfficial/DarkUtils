package gg.darkutils.feat.qol;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.utils.Helpers;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.jetbrains.annotations.NotNull;

public final class AutoClicker {
    private AutoClicker() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final void resetState() {
        for (final var key : AutoClicker.Key.VALUES) {
            key.resetState();
        }
    }

    public static final boolean isPressed(@NotNull final KeyBinding keyBinding) {
        final var actual = keyBinding.isPressed();

        if (!DarkUtilsConfig.INSTANCE.autoClicker || !actual) {
            return actual;
        }

        for (final var key : AutoClicker.Key.VALUES) {
            if (key.isForKeyBinding(keyBinding)) {
                return key.isPressed(true);
            }
        }

        return true;
    }

    public static final boolean wasPressed(@NotNull final KeyBinding keyBinding) {
        final var actual = keyBinding.wasPressed();

        if (!DarkUtilsConfig.INSTANCE.autoClicker) {
            return actual;
        }

        for (final var key : AutoClicker.Key.VALUES) {
            if (key.isForKeyBinding(keyBinding)) {
                return key.wasPressed(actual);
            }
        }

        return actual;
    }

    private enum Key {
        LEFT(MinecraftClient.getInstance().options.attackKey),
        RIGHT(MinecraftClient.getInstance().options.useKey);

        private static final AutoClicker.Key @NotNull [] VALUES = AutoClicker.Key.values();

        @NotNull
        private final KeyBinding keyBinding;
        private boolean clicking;

        private Key(@NotNull final KeyBinding keyBinding) {
            this.keyBinding = keyBinding;
        }

        private static final boolean isAutoClickNotAllowedForHeldItem(final boolean right) {
            return right ? AutoClicker.Key.isAutoClickNotAllowedForHeldItemRc() : AutoClicker.Key.isAutoClickNotAllowedForHeldItemLc();

        }

        private static final boolean isAutoClickNotAllowedForHeldItemRc() {
            return !Helpers.isHoldingARCMWeaponOrMatches(
                    name -> DarkUtilsConfig.INSTANCE.autoClickerWorkWithAOTV
                            && Helpers.matchHoldingAOTV().test(name)
            );
        }

        private static final boolean isAutoClickNotAllowedForHeldItemLc() {
            return !Helpers.isHoldingASwordHuntaxeOrSpade();
        }

        private final void resetState() {
            this.clicking = false;
        }

        private final boolean isForKeyBinding(@NotNull final KeyBinding keyBinding) {
            return this.keyBinding == keyBinding;
        }

        private final boolean isAutoClickNotAllowedForHeldItem() {
            return AutoClicker.Key.isAutoClickNotAllowedForHeldItem(AutoClicker.Key.RIGHT == this);
        }

        private final boolean isPressed(final boolean actual) {
            return actual && this.isAutoClickNotAllowedForHeldItem();
        }

        private final boolean wasPressed(final boolean actual) {
            if (actual || this.clicking) {
                return actual;
            }

            if (!this.keyBinding.isPressed()) {
                return false;
            }

            final var right = AutoClicker.Key.RIGHT == this;

            if (AutoClicker.Key.isAutoClickNotAllowedForHeldItem(right)) {
                return false;
            }

            this.clicking = true;

            var combined = Helpers.isButton().or(Helpers.isCommandBlock());

            if (!DarkUtilsConfig.INSTANCE.autoClickerWorkInLevers) {
                combined = combined.or(Helpers.isLever());
            }

            return !right
                    || !(Helpers.doesTargetedBlockMatch(combined)
                    || Helpers.isLookingAtATerminalEntity());
        }
    }
}
