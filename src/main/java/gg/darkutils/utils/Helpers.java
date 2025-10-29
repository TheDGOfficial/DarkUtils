package gg.darkutils.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import org.jetbrains.annotations.NotNull;

public final class Helpers {
    private Helpers() {
        super();

        throw new UnsupportedOperationException("static utility class");
    }

    public static final boolean isLookingAtAButton() {
        final var mc = MinecraftClient.getInstance();
        final var world = mc.world;
        return null != world && mc.crosshairTarget instanceof final BlockHitResult blockHitResult && world.getBlockState(blockHitResult.getBlockPos()).isIn(BlockTags.BUTTONS);
    }

    public static final boolean isLookingAtALever() {
        final var mc = MinecraftClient.getInstance();
        final var world = mc.world;
        return null != world && mc.crosshairTarget instanceof final BlockHitResult blockHitResult && mc.world.getBlockState(blockHitResult.getBlockPos()).isOf(Blocks.LEVER);
    }

    public static final boolean isLookingAtACraftingTable() {
        final var mc = MinecraftClient.getInstance();
        final var world = mc.world;
        return null != world && mc.crosshairTarget instanceof final BlockHitResult blockHitResult && mc.world.getBlockState(blockHitResult.getBlockPos()).isOf(Blocks.CRAFTING_TABLE);
    }

    public static final boolean isLookingAtAMushroom() {
        final var mc = MinecraftClient.getInstance();
        final var world = mc.world;
        if (null != world && mc.crosshairTarget instanceof final BlockHitResult blockHitResult)  {
            final var state = mc.world.getBlockState(blockHitResult.getBlockPos());
            return state.isOf(Blocks.RED_MUSHROOM) || state.isOf(Blocks.BROWN_MUSHROOM);
        }
        return false;
    }

    public static final boolean isLookingAtARedstoneBlockWithSkull() {
        final var mc = MinecraftClient.getInstance();
        final var world = mc.world;
        return null != world && mc.crosshairTarget instanceof final BlockHitResult blockHitResult && mc.world.getBlockState(blockHitResult.getBlockPos()).isOf(Blocks.REDSTONE_BLOCK) && Helpers.isHoldingAPlayerSkull();
    }

    @NotNull
    private static final ItemStack getItemStackInHand() {
        final var player = MinecraftClient.getInstance().player;
        return null == player ? ItemStack.EMPTY : player.getStackInHand(Hand.MAIN_HAND);
    }

    public static final boolean isHoldingASword() {
        return Helpers.getItemStackInHand()
                .isIn(ItemTags.SWORDS);
    }

    private static final boolean isHoldingAPlayerSkull() {
        return Helpers.getItemStackInHand().isOf(Items.PLAYER_HEAD);
    }

    public static final boolean isHoldingRCMWeapon() {
        final var customName = Helpers.getItemStackInHand().getCustomName();
        if (null != customName) {
            final var plain = customName.getString();
            return plain.contains("Hyperion") || plain.contains("Astraea");
        }
        return false;
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
