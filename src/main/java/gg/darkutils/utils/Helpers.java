package gg.darkutils.utils;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

public final class Helpers {
    private Helpers() {
        super();

        throw new UnsupportedOperationException("static utility class");
    }

    private static final boolean doesTargetedBlockMatch(@NotNull final Predicate<BlockState> matcher) {
        final var mc = MinecraftClient.getInstance();
        final var world = mc.world;
        if (null == world || !(mc.crosshairTarget instanceof final BlockHitResult blockHitResult)) {
            return false;
        }
        final var state = world.getBlockState(blockHitResult.getBlockPos());
        return matcher.test(state);
    }

    public static final boolean isLookingAtAButton() {
        return Helpers.doesTargetedBlockMatch(state -> state.isIn(BlockTags.BUTTONS));
    }

    public static final boolean isLookingAtALever() {
        return Helpers.doesTargetedBlockMatch(state -> state.isOf(Blocks.LEVER));
    }

    public static final boolean isLookingAtACraftingTable() {
        return Helpers.doesTargetedBlockMatch(state -> state.isOf(Blocks.CRAFTING_TABLE));
    }

    public static final boolean isLookingAtAMushroom() {
        return Helpers.doesTargetedBlockMatch(state -> state.isOf(Blocks.RED_MUSHROOM) || state.isOf(Blocks.BROWN_MUSHROOM));
    }

    public static final boolean isLookingAtARedstoneBlock() {
        return Helpers.doesTargetedBlockMatch(state -> state.isOf(Blocks.REDSTONE_BLOCK));
    }

    public static final boolean isLookingAtACommandBlock() {
        return Helpers.doesTargetedBlockMatch(state -> state.isOf(Blocks.COMMAND_BLOCK));
    }

    @NotNull
    private static final ItemStack getItemStackInHand() {
        final var player = MinecraftClient.getInstance().player;
        return null == player ? ItemStack.EMPTY : player.getStackInHand(Hand.MAIN_HAND);
    }

    private static final boolean doesHeldItemMatch(@NotNull final Predicate<ItemStack> matcher) {
        final var stack = Helpers.getItemStackInHand();
        return matcher.test(stack);
    }

    private static final boolean doesHeldItemNameMatch(@NotNull final Predicate<String> matcher) {
        final var stack = Helpers.getItemStackInHand();
        final var customName = stack.getCustomName();
        if (null == customName) {
            return false;
        }
        final var plain = customName.getString();
        return matcher.test(plain);
    }

    public static final boolean isHoldingASword() {
        return Helpers.doesHeldItemMatch(stack -> stack.isIn(ItemTags.SWORDS));
    }

    public static final boolean isHoldingARCMWeapon() {
        return Helpers.doesHeldItemNameMatch(name -> name.contains("Hyperion") || name.contains("Astraea"));
    }

    public static final void displayCountdownTitles(@NotNull final String color, @NotNull final String finalText, final int seconds) {
        // Show the first number immediately
        Helpers.notify(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, color + seconds);

        // Queue the rest
        for (var i = seconds - 1; 0 < i; --i) {
            final var value = i;
            final var delay = 20 * (seconds - i);
            TickUtils.queueTickTask(
                    () -> Helpers.notify(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, color + value),
                    delay
            );
        }

        // Queue the final text
        TickUtils.queueTickTask(
                () -> Helpers.notify(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), color + finalText),
                20 * seconds
        );
    }

    private static final void notify(@NotNull final SoundEvent sound, @NotNull final String text) {
        final var client = MinecraftClient.getInstance();

        Helpers.playSound(sound, 1.0F, 1.0F);
        client.inGameHud.setTitle(Text.of(text));
        client.inGameHud.setTitleTicks(0, 20, 0);
    }

    private static final void playSound(@NotNull final SoundEvent sound, final float volume, final float pitch) {
        final var player = MinecraftClient.getInstance().player;

        if (null != player) {
            player.playSound(sound, volume, pitch);
        }
    }
}
