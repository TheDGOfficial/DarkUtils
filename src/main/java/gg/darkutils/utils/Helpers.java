package gg.darkutils.utils;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import org.jetbrains.annotations.NotNull;

import java.util.function.ObjIntConsumer;
import java.util.function.Predicate;

public final class Helpers {
    private Helpers() {
        super();

        throw new UnsupportedOperationException("static utility class");
    }

    public static final boolean doesTargetedBlockMatch(@NotNull final Predicate<BlockState> matcher) {
        final var mc = MinecraftClient.getInstance();
        final var world = mc.world;
        if (null == world || !(mc.crosshairTarget instanceof final BlockHitResult blockHitResult)) {
            return false;
        }
        final var state = world.getBlockState(blockHitResult.getBlockPos());
        return matcher.test(state);
    }

    @NotNull
    public static final Predicate<BlockState> isButton() {
        return state -> state.isIn(BlockTags.BUTTONS);
    }

    @NotNull
    public static final Predicate<BlockState> isLever() {
        return state -> state.isOf(Blocks.LEVER);
    }

    @NotNull
    public static final Predicate<BlockState> isCraftingTable() {
        return state -> state.isOf(Blocks.CRAFTING_TABLE);
    }

    @NotNull
    public static final Predicate<BlockState> isMushroom() {
        return state -> state.isOf(Blocks.RED_MUSHROOM) || state.isOf(Blocks.BROWN_MUSHROOM);
    }

    @NotNull
    public static final Predicate<BlockState> isRedstoneBlock() {
        return state -> state.isOf(Blocks.REDSTONE_BLOCK);
    }

    @NotNull
    public static final Predicate<BlockState> isCommandBlock() {
        return state -> state.isOf(Blocks.COMMAND_BLOCK);
    }

    @NotNull
    public static final Predicate<BlockState> isDoor() {
        return state -> state.isIn(BlockTags.DOORS);
    }

    private static final boolean doesTargetedEntityMatch(@NotNull final Predicate<Entity> matcher) {
        final var mc = MinecraftClient.getInstance();
        final var world = mc.world;
        if (null == world || !(mc.crosshairTarget instanceof final EntityHitResult entityHitResult)) {
            return false;
        }
        final var entity = entityHitResult.getEntity();
        return matcher.test(entity);
    }

    public static final boolean isLookingAtATerminalEntity() {
        return Helpers.doesTargetedEntityMatch(entity -> {
            if (entity instanceof ArmorStandEntity) {
                final var customName = entity.getCustomName();
                if (null != customName) {
                    final var name = customName.getString();
                    return "Inactive Terminal".equals(name) || "CLICK HERE".equals(name);
                }
            }
            return false;
        });
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
        return Helpers.doesHeldItemNameMatch(Helpers.getItemStackInHand(), matcher);
    }

    private static final boolean doesHeldItemNameMatch(@NotNull final ItemStack stack, @NotNull final Predicate<String> matcher) {
        final var customName = stack.getCustomName();
        if (null == customName) {
            return false;
        }
        final var plain = customName.getString();
        return matcher.test(plain);
    }

    public static final boolean isHoldingASwordHuntaxeOrSpade() {
        return Helpers.doesHeldItemMatch(stack -> stack.isIn(ItemTags.SWORDS) || Helpers.doesHeldItemNameMatch(stack, name -> name.contains("Huntaxe") || name.contains("Spade")));
    }

    public static final boolean isHoldingARCMWeaponOrMatches(@NotNull final Predicate<String> matcher) {
        return Helpers.doesHeldItemNameMatch(name -> name.contains("Hyperion") || name.contains("Astraea") || matcher.test(name));
    }

    @NotNull
    public static final Predicate<String> matchHoldingAOTV() {
        return name -> name.contains("Aspect of the Void");
    }

    public static final void displayCountdownTitles(@NotNull final String color, @NotNull final String finalText, final int seconds) {
        Helpers.displayCountdownTitlesInternal(color, finalText, seconds, TickUtils::queueTickTask);
    }

    public static final void displayCountdownTitlesInServerTicks(@NotNull final String color, @NotNull final String finalText, final int seconds) {
        Helpers.displayCountdownTitlesInternal(color, finalText, seconds, TickUtils::queueServerTickTask);
    }

    private static final void displayCountdownTitlesInternal(@NotNull final String color, @NotNull final String finalText, final int seconds, @NotNull final ObjIntConsumer<Runnable> queueMethod) {
        // Show the first number immediately
        Helpers.notify(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, color + seconds);

        // Queue the rest
        for (var i = seconds - 1; 0 < i; --i) {
            final var value = i;
            final var delay = 20 * (seconds - i);
            queueMethod.accept(
                    () -> Helpers.notify(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, color + value),
                    delay
            );
        }

        // Queue the final text
        queueMethod.accept(
                () -> Helpers.notify(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), color + finalText),
                20 * seconds
        );
    }

    private static final void notify(@NotNull final SoundEvent sound, @NotNull final String text) {
        Helpers.notify(sound, text, 20);
    }

    public static final void notify(@NotNull final SoundEvent sound, @NotNull final String text, final int ticks) {
        final var client = MinecraftClient.getInstance();

        Helpers.playSound(sound, 1.0F, 1.0F);
        client.inGameHud.setTitle(Text.of(text));
        client.inGameHud.setTitleTicks(0, ticks, 0);
    }

    public static final void notifyForServerTicks(@NotNull final SoundEvent sound, @NotNull final String text, final int serverTicks) {
        Helpers.notifyForServerTicks(sound, text, serverTicks, () -> {
        });
    }

    public static final void notifyForServerTicks(@NotNull final SoundEvent sound, @NotNull final String text, final int serverTicks, @NotNull final Runnable afterDismissHook) {
        final var client = MinecraftClient.getInstance();

        Helpers.playSound(sound, 1.0F, 1.0F);
        client.inGameHud.setTitle(Text.of(text));

        // Hacky way to simulate server tick dismissal of the title
        client.inGameHud.setTitleTicks(0, Integer.MAX_VALUE, 0);
        TickUtils.queueServerTickTask(() -> {
            Helpers.clearTitle();
            afterDismissHook.run();
        }, serverTicks);
    }

    private static final void clearTitle() {
        final var client = MinecraftClient.getInstance();

        client.inGameHud.setTitle(null);
        client.inGameHud.setTitleTicks(0, Integer.MAX_VALUE, 0);
    }

    private static final void playSound(@NotNull final SoundEvent sound, final float volume, final float pitch) {
        final var player = MinecraftClient.getInstance().player;

        if (null != player) {
            player.playSound(sound, volume, pitch);
        }
    }
}
