package gg.darkutils.mixin.misc;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.data.PersistentData;
import gg.darkutils.events.InteractEntityEvent;
import gg.darkutils.feat.farming.StickyFarmingKeys;
import gg.darkutils.feat.qol.AutoClicker;
import gg.darkutils.utils.Helpers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
final class MinecraftMixin {
    private MinecraftMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Redirect(at = @At(value = "INVOKE", target = "Ljava/lang/Thread;yield()V", remap = false), method = "runTick")
    private final void darkutils$skipYieldIfEnabled() {
        if (!DarkUtilsConfig.INSTANCE.disableYield) {
            Thread.yield();
        }
        // skip a yield call that reduces fps
        // the call was put to make sure rendering does not stall other threads such as chunk loading, but that's OS scheduler's job to handle,
        // the code should utilize maximum resources so this yield call is unnecessary.
    }

    @Inject(at = @At("HEAD"), method = "run")
    private final void darkutils$adjustPriorityIfEnabled(@NotNull final CallbackInfo ci) {
        if (DarkUtilsConfig.INSTANCE.alwaysPrioritizeRenderThread) {
            // vanilla game only sets priority to max for processors with 4 or more cores, but it is best to have max priority no matter the core count.
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private final void darkutils$onStartTick(@NotNull final CallbackInfo ci) {
        Helpers.resetHeldItemCache();
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;pick(F)V", shift = At.Shift.AFTER))
    private final void darkutils$afterCrosshairTargetUpdate(@NotNull final CallbackInfo ci) {
        Helpers.resetTargetCache();
    }

    @Inject(method = "handleKeybinds", at = @At("HEAD"))
    private final void darkutils$resetState(@NotNull final CallbackInfo ci) {
        StickyFarmingKeys.resetState();
        AutoClicker.resetState();
    }

    @Redirect(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;consumeClick()Z"))
    private final boolean darkutils$wasPressed$modifyReturnValueIfApplicable(@NotNull final KeyMapping keyBinding) {
        return AutoClicker.wasPressed(keyBinding, StickyFarmingKeys.wasPressed(keyBinding));
    }

    @Redirect(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;isDown()Z"))
    private final boolean darkutils$isPressed$modifyReturnValueIfApplicable(@NotNull final KeyMapping keyBinding) {
        return AutoClicker.isPressed(keyBinding, StickyFarmingKeys.isPressed(keyBinding, false));
    }

    @WrapOperation(method = "startUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;interactAt(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/EntityHitResult;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;"))
    private final @NotNull InteractionResult darkutils$onEntityInteract(@NotNull final MultiPlayerGameMode instance, @NotNull final Player player, @NotNull final Entity entity, @NotNull final EntityHitResult hitResult, @NotNull final InteractionHand hand, @NotNull final Operation<InteractionResult> original) {
        return new InteractEntityEvent(entity).triggerAndCancelled() ? InteractionResult.CONSUME : original.call(instance, player, entity, hitResult, hand);
    }

    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private final void darkutils$hasOutline$disableIfEnabled(@NotNull final CallbackInfoReturnable<Boolean> cir) {
        if (DarkUtilsConfig.INSTANCE.disableGlowing) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "destroy", at = @At("HEAD"))
    private final void darkutils$onQuitGame(@NotNull final CallbackInfo ci) {
        PersistentData.saveAtomicIfDirtyThreadSafe(true);
    }
}
