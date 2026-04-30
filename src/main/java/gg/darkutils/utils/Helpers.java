package gg.darkutils.utils;

import gg.darkutils.utils.chat.ChatUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.ObjIntConsumer;
import java.util.function.Predicate;

public final class Helpers {
    @NotNull
    private static final BlockState AIR_STATE = Blocks.AIR.defaultBlockState();
    @Nullable
    private static BlockState targetedBlock;
    @Nullable
    private static Optional<Entity> targetedEntity;
    @Nullable
    private static String targetedEntityName;
    @Nullable
    private static ItemStack mainHandHeldItemStack;
    @Nullable
    private static ItemStack offHandHeldItemStack;
    @Nullable
    private static String mainHandHeldItemStackName;

    private Helpers() {
        super();

        throw new UnsupportedOperationException("static utility class");
    }

    public static final void resetHeldItemCache() {
        Helpers.mainHandHeldItemStack = null;
        Helpers.mainHandHeldItemStackName = null;

        Helpers.offHandHeldItemStack = null;
    }

    public static final void resetTargetCache() {
        Helpers.targetedBlock = null;

        Helpers.targetedEntity = null;
        Helpers.targetedEntityName = null;
    }

    @NotNull
    public static final BlockState getTargetedBlock() {
        final var cached = Helpers.targetedBlock;

        if (null != cached) {
            return cached;
        }

        final var mc = Minecraft.getInstance();
        final var world = mc.level;
        return Helpers.targetedBlock = null == world || !(mc.hitResult instanceof final BlockHitResult blockHitResult) ? Helpers.AIR_STATE : world.getBlockState(blockHitResult.getBlockPos());
    }

    public static final boolean doesTargetedBlockMatch(@NotNull final Predicate<BlockState> matcher) {
        final var blockState = Helpers.getTargetedBlock();
        return Helpers.AIR_STATE != blockState && matcher.test(blockState);
    }

    @NotNull
    public static final Predicate<BlockState> isButton() {
        return state -> state.is(BlockTags.BUTTONS);
    }

    @NotNull
    public static final Predicate<BlockState> isLever() {
        return state -> state.is(Blocks.LEVER);
    }

    @NotNull
    public static final Predicate<BlockState> isCraftingTable() {
        return state -> state.is(Blocks.CRAFTING_TABLE);
    }

    @NotNull
    public static final Predicate<BlockState> isMushroom() {
        return state -> state.is(Blocks.RED_MUSHROOM) || state.is(Blocks.BROWN_MUSHROOM);
    }

    @NotNull
    public static final Predicate<BlockState> isRedstoneBlock() {
        return state -> state.is(Blocks.REDSTONE_BLOCK);
    }

    @NotNull
    public static final Predicate<BlockState> isCommandBlock() {
        return state -> state.is(Blocks.COMMAND_BLOCK);
    }

    @NotNull
    public static final Predicate<BlockState> isWoodenDoor() {
        return state -> state.is(BlockTags.WOODEN_DOORS);
    }

    private static final boolean doesTargetedEntityMatch(@NotNull final Predicate<Entity> matcher) {
        final var cached = Helpers.targetedEntity;

        if (null != cached) {
            final var et = cached.orElse(null);
            return null != et && matcher.test(et);
        }

        final var mc = Minecraft.getInstance();
        final var world = mc.level;

        if (null == world || !(mc.hitResult instanceof final EntityHitResult entityHitResult)) {
            Helpers.targetedEntity = Optional.empty();
            return false;
        }

        final var entity = entityHitResult.getEntity();
        Helpers.targetedEntity = Optional.of(entity);

        return matcher.test(entity);
    }

    public static final boolean isLookingAtATerminalEntity() {
        return Helpers.doesTargetedEntityMatch(entity -> {
            if (entity instanceof ArmorStand) {
                final String name;

                final var cached = Helpers.targetedEntityName;
                if (null == cached) {
                    final var customName = entity.getCustomName();
                    Helpers.targetedEntityName = name = null == customName ? "" : ChatUtils.removeControlCodes(customName.getString());
                } else {
                    name = cached;
                }

                return "Inactive Terminal".equals(name) || "CLICK HERE".equals(name);
            }
            return false;
        });
    }

    @NotNull
    public static final ItemStack getItemStackInHand(@NotNull final InteractionHand hand) {
        final var main = InteractionHand.MAIN_HAND == hand;

        // Very unlikely check, branch predictor or even the C2 will realize the if is never true unless Mojang actually adds another enum value to Hand enum.
        if (!main && InteractionHand.OFF_HAND != hand) {
            // We must be in the future and 3-handed player exists
            throw new UnsupportedOperationException("Helpers cache needs updating for new possible hand " + hand.name() + " (" + Helpers.class.getName() + ')');
        }

        return Helpers.getItemStackInHand(main);
    }

    @NotNull
    public static final ItemStack getItemStackInMainHand() {
        return Helpers.getItemStackInHand(true);
    }

    @NotNull
    private static final ItemStack getItemStackInHand(final boolean main) {
        final var cached = main ? Helpers.mainHandHeldItemStack : Helpers.offHandHeldItemStack;

        if (null != cached) {
            return cached;
        }

        final var player = Minecraft.getInstance().player;
        final var item = null == player ? ItemStack.EMPTY : player.getItemInHand(main ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);

        if (main) {
            Helpers.mainHandHeldItemStack = item;
        } else {
            Helpers.offHandHeldItemStack = item;
        }

        return item;
    }

    private static final boolean doesHeldItemMatch(@NotNull final Predicate<ItemStack> matcher) {
        final var stack = Helpers.getItemStackInMainHand();
        return matcher.test(stack);
    }

    private static final boolean doesHeldItemNameMatch(@NotNull final Predicate<String> matcher) {
        return Helpers.doesHeldItemNameMatch(Helpers.getItemStackInMainHand(), matcher);
    }

    private static final boolean doesHeldItemNameMatch(@NotNull final ItemStack stack, @NotNull final Predicate<String> matcher) {
        final var cached = Helpers.mainHandHeldItemStackName;

        if (null != cached) {
            return matcher.test(cached);
        }

        final var customName = stack.getCustomName();

        if (null == customName) {
            Helpers.mainHandHeldItemStackName = "";
            return false;
        }

        final var plain = ChatUtils.removeControlCodes(customName.getString());
        Helpers.mainHandHeldItemStackName = plain;

        return matcher.test(plain);
    }

    public static final boolean isHoldingADiamondHoeAxeOrSword() {
        return Helpers.doesHeldItemMatch(stack -> (stack.is(Items.DIAMOND_HOE) || stack.is(Items.DIAMOND_AXE)) || (stack.is(ItemTags.SWORDS) && Helpers.doesHeldItemNameMatch(stack, name -> name.contains("Cactus Knife"))));
    }

    public static final boolean isHoldingASwordHuntaxeOrSpade() {
        return Helpers.doesHeldItemMatch(stack -> (stack.is(ItemTags.SWORDS) && !Helpers.doesHeldItemNameMatch(stack, name -> name.contains("Cactus Knife"))) || Helpers.doesHeldItemNameMatch(stack, name -> name.contains("Huntaxe") || name.contains("Spade")));
    }

    public static final boolean isHoldingARCMWeaponOrMatches(@NotNull final Predicate<String> matcher) {
        return Helpers.doesHeldItemNameMatch(name -> name.contains("Hyperion") || name.contains("Astraea") || matcher.test(name));
    }

    public static final boolean isHoldingAGyrokineticWand() {
        return Helpers.doesHeldItemNameMatch("Gyrokinetic Wand"::equals);
    }

    @NotNull
    public static final Predicate<String> matchHoldingAOTV() {
        return name -> name.contains("Aspect of the Void");
    }

    public static final @Nullable String extractNumbers(@NotNull final String input) {
        return Helpers.extractNumbers(input, false);
    }

    private static final @Nullable String extractNumbers(@NotNull final String input, final boolean includeSuffix) {
        final var length = input.length();

        var start = -1;
        var end = -1;

        for (var i = 0; i < length; ++i) {
            final var c = input.charAt(i);

            if (Character.isDigit(c)) {
                if (-1 == start) {
                    start = i;

                    if (includeSuffix) {
                        return input.substring(start);
                    }
                }

                end = i + 1;
            } else if (-1 != start) {
                break;
            }
        }

        return -1 == start ? null : input.substring(start, end);
    }

    public static final void displayCountdownTitles(@NotNull final String color, @NotNull final String finalText, final int seconds, @NotNull final BooleanSupplier precondition) {
        Helpers.displayCountdownTitlesInternal(color, finalText, seconds, TickUtils::queueTickTask, precondition);
    }

    public static final void displayCountdownTitlesInServerTicks(@NotNull final String color, @NotNull final String finalText, final int seconds) {
        Helpers.displayCountdownTitlesInServerTicks(color, finalText, seconds, () -> true);
    }

    private static final void displayCountdownTitlesInServerTicks(@NotNull final String color, @NotNull final String finalText, final int seconds, @NotNull final BooleanSupplier precondition) {
        Helpers.displayCountdownTitlesInternal(color, finalText, seconds, TickUtils::queueServerTickTask, precondition);
    }

    private static final void displayCountdownTitlesInternal(@NotNull final String color, @NotNull final String finalText, final int seconds, @NotNull final ObjIntConsumer<TickUtils.TaskAction> queueMethod, @NotNull final BooleanSupplier precondition) {
        // Show the first number immediately
        Helpers.notify(SoundEvents.EXPERIENCE_ORB_PICKUP, color + seconds);

        // Queue the rest
        for (var i = seconds - 1; 0 < i; --i) {
            final var value = i;
            final var delay = 20 * (seconds - i);
            queueMethod.accept(
                    () -> {
                        if (precondition.getAsBoolean()) {
                            Helpers.notify(SoundEvents.EXPERIENCE_ORB_PICKUP, color + value);
                        }
                    },
                    delay
            );
        }

        // Queue the final text
        queueMethod.accept(
                () -> {
                    if (precondition.getAsBoolean()) {
                        Helpers.notify(SoundEvents.NOTE_BLOCK_PLING.value(), color + finalText);
                    }
                },
                20 * seconds
        );
    }

    private static final void notify(@NotNull final SoundEvent sound, @NotNull final String text) {
        Helpers.notify(sound, text, 20);
    }

    public static final void notify(@NotNull final SoundEvent sound, @NotNull final String text, final int ticks) {
        final var client = Minecraft.getInstance();

        Helpers.playSound(sound, 1.0F, 1.0F);
        client.gui.setTitle(Component.nullToEmpty(text));
        client.gui.setTimes(0, ticks, 0);
    }

    public static final void notifyForServerTicks(@NotNull final SoundEvent sound, @NotNull final String text, final int serverTicks) {
        Helpers.notifyForServerTicks(sound, text, serverTicks, () -> {
        });
    }

    private static final void notifyForServerTicks(@NotNull final SoundEvent sound, @NotNull final String text, final int serverTicks, @NotNull final Runnable afterDismissHook) {
        final var client = Minecraft.getInstance();

        Helpers.playSound(sound, 1.0F, 1.0F);
        client.gui.setTitle(Component.nullToEmpty(text));

        // Hacky way to simulate server tick dismissal of the title
        client.gui.setTimes(0, Integer.MAX_VALUE, 0);
        TickUtils.queueServerTickTask(() -> {
            Helpers.clearTitle();
            afterDismissHook.run();
        }, serverTicks);
    }

    private static final void clearTitle() {
        Minecraft.getInstance().gui.clearTitles();
    }

    private static final void playSound(@NotNull final SoundEvent sound, final float volume, final float pitch) {
        final var player = Minecraft.getInstance().player;

        if (null != player) {
            player.playSound(sound, volume, pitch);
        }
    }
}
