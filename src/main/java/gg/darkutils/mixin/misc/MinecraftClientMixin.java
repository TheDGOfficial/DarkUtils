package gg.darkutils.mixin.misc;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.events.InteractEntityEvent;
import gg.darkutils.feat.qol.AutoClicker;
import gg.darkutils.utils.Helpers;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
final class MinecraftClientMixin {
    private MinecraftClientMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Redirect(at = @At(value = "INVOKE", target = "Ljava/lang/Thread;yield()V", remap = false), method = "render")
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

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;updateCrosshairTarget(F)V", shift = At.Shift.AFTER))
    private final void darkutils$afterCrosshairTargetUpdate(@NotNull final CallbackInfo ci) {
        Helpers.resetTargetCache();
    }

    @Inject(method = "handleInputEvents", at = @At("HEAD"))
    private final void darkutils$resetState(@NotNull final CallbackInfo ci) {
        AutoClicker.resetState();
    }

    @Redirect(method = "handleInputEvents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;wasPressed()Z"))
    private final boolean darkutils$wasPressed$modifyReturnValueIfApplicable(@NotNull final KeyBinding keyBinding) {
        return AutoClicker.wasPressed(keyBinding);
    }

    @Redirect(method = "handleInputEvents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;isPressed()Z"))
    private final boolean darkutils$isPressed$modifyReturnValueIfApplicable(@NotNull final KeyBinding keyBinding) {
        return AutoClicker.isPressed(keyBinding);
    }

    @WrapOperation(method = "doItemUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;interactEntityAtLocation(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/entity/Entity;Lnet/minecraft/util/hit/EntityHitResult;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;"))
    private final @NotNull ActionResult darkutils$onEntityInteract(@NotNull final ClientPlayerInteractionManager instance, @NotNull final PlayerEntity player, @NotNull final Entity entity, @NotNull final EntityHitResult hitResult, @NotNull final Hand hand, @NotNull final Operation<ActionResult> original) {
        return new InteractEntityEvent(entity).triggerAndCancelled() ? ActionResult.CONSUME : original.call(instance, player, entity, hitResult, hand);
    }

    @Inject(method = "hasOutline", at = @At("HEAD"), cancellable = true)
    private final void darkutils$hasOutline$disableIfEnabled(@NotNull final CallbackInfoReturnable<Boolean> cir) {
        if (DarkUtilsConfig.INSTANCE.disableGlowing) {
            cir.setReturnValue(false);
        }
    }
}
