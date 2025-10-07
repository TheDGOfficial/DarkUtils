package gg.darkutils.feat.qol;

import gg.darkutils.config.DarkUtilsConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import org.jetbrains.annotations.NotNull;

public final class AutoClicker {
    private AutoClicker() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final void resetState() {
        for (final var key : AutoClicker.Key.values()) {
            key.state = true;
        }
    }

    public static final boolean isPressed(@NotNull final KeyBinding keyBinding) {
        final var actual = keyBinding.isPressed();

        if (!DarkUtilsConfig.INSTANCE.autoClicker || !actual) {
            return actual;
        }

        for (final var key : AutoClicker.Key.VALUES) {
            if (key.key == keyBinding) {
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
            if (key.key == keyBinding) {
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
        private final KeyBinding key;
        private boolean state = true;

        private Key(@NotNull final KeyBinding key) {
            this.key = key;
        }

        @NotNull
        private static final ItemStack getItemStackInHand() {
            return MinecraftClient.getInstance().player.getStackInHand(Hand.MAIN_HAND);
        }

        private static final boolean isHoldingASword() {
            return AutoClicker.Key.getItemStackInHand()
                    .isIn(ItemTags.SWORDS);
        }

        private static final boolean isHoldingRCMWeapon() {
            final var customName = AutoClicker.Key.getItemStackInHand().getCustomName();
            if (null != customName) {
                final var plain = customName.getString();
                return plain.contains("Hyperion") || plain.contains("Astraea");
            }
            return false;
        }

        private static final boolean isLookingAtAButton() {
            final var mc = MinecraftClient.getInstance();
            return mc.crosshairTarget instanceof final BlockHitResult blockHitResult && mc.world.getBlockState(blockHitResult.getBlockPos()).isIn(BlockTags.BUTTONS);
        }

        private final boolean isPressed(final boolean actual) {
            return (AutoClicker.Key.RIGHT == this ? !AutoClicker.Key.isHoldingRCMWeapon() : !AutoClicker.Key.isHoldingASword()) && actual;
        }

        private final boolean wasPressed(final boolean actual) {
            final var right = AutoClicker.Key.RIGHT == this;
            if (!actual && this.state && (right ? AutoClicker.Key.isHoldingRCMWeapon() : AutoClicker.Key.isHoldingASword())) {
                final var held = this.key.isPressed();

                if (held) {
                    this.state = false;
                    return !right || !AutoClicker.Key.isLookingAtAButton();
                }
            }
            return actual;
        }
    }
}
