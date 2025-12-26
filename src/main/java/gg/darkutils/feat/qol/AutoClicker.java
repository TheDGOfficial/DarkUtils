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
        private boolean state = true;

        private Key(@NotNull final KeyBinding keyBinding) {
            this.keyBinding = keyBinding;
        }

        private final void resetState() {
            this.state = true;
        }

        private final boolean isForKeyBinding(@NotNull final KeyBinding keyBinding) {
            return this.keyBinding == keyBinding;
        }

        private final boolean isPressed(final boolean actual) {
            return (AutoClicker.Key.RIGHT == this ? !Helpers.isHoldingARCMWeaponOrMatches(name -> DarkUtilsConfig.INSTANCE.autoClickerWorkWithAOTV && Helpers.matchHoldingAOTV().test(name)) : !Helpers.isHoldingASwordHuntaxeOrSpade()) && actual;
        }

        private final boolean wasPressed(final boolean actual) {
            final var right = AutoClicker.Key.RIGHT == this;
            if (!actual && this.state && (right ? Helpers.isHoldingARCMWeaponOrMatches(name -> DarkUtilsConfig.INSTANCE.autoClickerWorkWithAOTV && Helpers.matchHoldingAOTV().test(name)) : Helpers.isHoldingASwordHuntaxeOrSpade())) {
                final var held = this.keyBinding.isPressed();

                if (held) {
                    this.state = false;
                    return !right || !Helpers.isLookingAtAButton() && (DarkUtilsConfig.INSTANCE.autoClickerWorkInLevers || !Helpers.isLookingAtALever()) && !Helpers.isLookingAtATerminal();
                }
            }
            return actual;
        }
    }
}
