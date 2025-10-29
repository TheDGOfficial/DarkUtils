package gg.darkutils.mixin.qol;

import gg.darkutils.events.UseItemEvent;
import gg.darkutils.events.base.EventRegistry;
import gg.darkutils.utils.Helpers;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
final class ClientPlayerInteractionManagerMixin {
    private ClientPlayerInteractionManagerMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Shadow
    private final void syncSelectedSlot() {
        throw new IllegalStateException("shadow failed");
    }

    @Inject(method = "interactItem(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;",
            at = @At("HEAD"), cancellable = true)
    private final void darkutils$onInteractItem$cancelIfEnabled(@NotNull final PlayerEntity player, @NotNull final Hand hand, @NotNull final CallbackInfoReturnable<ActionResult> cir) {
        this.syncSelectedSlot();

        final var stack = player.getStackInHand(hand);

        if (EventRegistry.centralRegistry().triggerEvent(new UseItemEvent(stack)).isCancelled()) {
            cir.setReturnValue(ActionResult.PASS);
        }
    }

    @Inject(method = "interactBlock(Lnet/minecraft/client/network/ClientPlayerEntity;Lnet/minecraft/util/Hand;Lnet/minecraft/util/hit/BlockHitResult;)Lnet/minecraft/util/ActionResult;",
            at = @At("HEAD"), cancellable = true)
    private final void darkutils$onInteractBlock$cancelIfEnabled(@NotNull final ClientPlayerEntity player, @NotNull final Hand hand, @NotNull final BlockHitResult blockHitResult, @NotNull final CallbackInfoReturnable<ActionResult> cir) {
        this.syncSelectedSlot();

        final var stack = player.getStackInHand(hand);

        // Treat looking at a regular block the same as clicking in air, so cancelling is allowed.
        // When looking at a block entity such as beacon, chest, command block, etc., we don't want to cancel the click.

        // This ensures we only cancel the item use itself and do not prevent interacting with block entities, such as opening a chest while holding the item.
        // Additionally, we do not cancel if looking at a button, lever or crafting table. Those are not classified as block entities but still have interactions.
        // We also do not cancel when looking at a mushroom. There is a 1x1 room where you get teleported under the room for a secret chest in Hypixel SkyBlock Dungeons when you right-click to a mushroom.
        // There is also 2 1x1 rooms that require you to place a skull to a redstone block so we also do not cancel if holding a player skull and looking at a redstone block.
        if (!player.getWorld().getBlockState(blockHitResult.getBlockPos()).hasBlockEntity() && !Helpers.isLookingAtAButton() && !Helpers.isLookingAtALever() && !Helpers.isLookingAtACraftingTable() && !Helpers.isLookingAtAMushroom() && !Helpers.isLookingAtARedstoneBlockWithSkull() && EventRegistry.centralRegistry().triggerEvent(new UseItemEvent(stack)).isCancelled()) {
            cir.setReturnValue(ActionResult.PASS);
        }
    }
}
