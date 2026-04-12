package gg.darkutils.mixin.qol;

import gg.darkutils.events.UseItemEvent;
import gg.darkutils.utils.Helpers;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
final class MultiPlayerGameModeMixin {
    private MultiPlayerGameModeMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Inject(method = "useItem(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
            at = @At("HEAD"), cancellable = true)
    private final void darkutils$onInteractItem$cancelIfEnabled(@NotNull final Player player, @NotNull final InteractionHand hand, @NotNull final CallbackInfoReturnable<InteractionResult> cir) {
        final var stack = Helpers.getItemStackInHand(hand);

        if (new UseItemEvent(stack, hand).triggerAndCancelled()) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }

    @Inject(method = "useItemOn(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;",
            at = @At("HEAD"), cancellable = true)
    private final void darkutils$onInteractBlock$cancelIfEnabled(@NotNull final LocalPlayer player, @NotNull final InteractionHand hand, @NotNull final BlockHitResult blockHitResult, @NotNull final CallbackInfoReturnable<InteractionResult> cir) {
        final var stack = Helpers.getItemStackInHand(hand);

        // Treat looking at a regular block the same as clicking in air, so cancelling is allowed.
        // When looking at a block entity such as beacon, chest, command block, etc., we don't want to cancel the click.

        // This ensures we only cancel the item use itself and do not prevent interacting with block entities, such as opening a chest while holding the item.
        // Additionally, we do not cancel if looking at a button, lever or crafting table. Those are not classified as block entities but still have interactions.
        // We also do not cancel when looking at a mushroom. There is a 1x1 room where you get teleported under the room for a secret chest in Hypixel SkyBlock Dungeons when you right-click to a mushroom.
        // There is also 2 1x1 rooms that require you to place a skull to a redstone block so we also do not cancel if looking at a redstone block.
        // We also do not cancel when looking at a wooden door, as that makes the door open/close.
        final var blockState = Helpers.getTargetedBlock();
        if (!blockState.hasBlockEntity() && !Helpers.doesTargetedBlockMatch(
                Helpers
                        .isButton()
                        .or(Helpers.isLever())
                        .or(Helpers.isCraftingTable())
                        .or(Helpers.isMushroom())
                        .or(Helpers.isRedstoneBlock())
                        .or(Helpers.isWoodenDoor())
        ) && new UseItemEvent(stack, hand).triggerAndCancelled()) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
