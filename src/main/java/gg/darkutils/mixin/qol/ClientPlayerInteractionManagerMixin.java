package gg.darkutils.mixin.qol;

import gg.darkutils.events.ItemUseEvent;
import gg.darkutils.events.base.EventRegistry;
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

        if (EventRegistry.centralRegistry().triggerEvent(new ItemUseEvent(stack)).isCancelled()) {
            cir.setReturnValue(ActionResult.PASS);
        }
    }

    @Inject(method = "interactBlock(Lnet/minecraft/client/network/ClientPlayerEntity;Lnet/minecraft/util/Hand;Lnet/minecraft/util/hit/BlockHitResult;)Lnet/minecraft/util/ActionResult;",
            at = @At("HEAD"), cancellable = true)
    private final void darkutils$onInteractBlock$cancelIfEnabled(@NotNull final ClientPlayerEntity player, @NotNull final Hand hand, @NotNull final BlockHitResult blockHitResult, @NotNull final CallbackInfoReturnable<ActionResult> cir) {
        this.syncSelectedSlot();

        final var stack = player.getStackInHand(hand);

        // Treat looking at a regular block the same as clicking in air, so cancelling is allowed.
        // When looking at a block entity such as beacon, chest, lever, command block, etc., we don't want to cancel the click.

        // If player.shouldCancelInteraction() returns true, which it does when sneaking, those blocks won't be interacted with if the click goes through,
        // so treat it the same as clicking in air, cancelling being allowed.

        // This ensures we only cancel the item use itself and do not prevent interacting with block entities, such as opening a chest while holding the item.
        if ((player.shouldCancelInteraction() || !player.getWorld().getBlockState(blockHitResult.getBlockPos()).hasBlockEntity()) && EventRegistry.centralRegistry().triggerEvent(new ItemUseEvent(stack)).isCancelled()) {
            cir.setReturnValue(ActionResult.PASS);
        }
    }
}
