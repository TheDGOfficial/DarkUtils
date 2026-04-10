package gg.darkutils.mixin.bugfixes;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.events.SlotClickEvent;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
final class HandledScreenMixin {
    private HandledScreenMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @ModifyExpressionValue(method = "mouseClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isInCreativeMode()Z"))
    private final boolean darkutils$fixMiddleClick(final boolean original) {
        // Overriding isInCreativeMode here is fine because Hypixel allows and uses middle click as a feature to disable Witherborn ability of Wither armor sets.
        // In Vanilla 1.8, you could always middle-click. In 1.21, Mojang put this creative mode check. We simply override it to restore old behavior so this is safe.
        return DarkUtilsConfig.INSTANCE.middleClickFix || original;
    }

    @Inject(method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V", at = @At("HEAD"), cancellable = true)
    private final void darkutils$cancelClickSlotIfApplicable(@Nullable final Slot slot, final int slotId, final int button, @NotNull final SlotActionType actionType, @NotNull final CallbackInfo ci) {
        if (null != slot && new SlotClickEvent((HandledScreen<?>) (Object) this, slotId, slot).triggerAndCancelled()) {
            ci.cancel();
        }
    }
}
