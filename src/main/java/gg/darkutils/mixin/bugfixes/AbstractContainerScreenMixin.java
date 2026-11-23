package gg.darkutils.mixin.bugfixes;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.events.SlotClickEvent;
import gg.darkutils.feat.qol.BackpackPreview;
import gg.darkutils.utils.LocationUtils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
final class AbstractContainerScreenMixin {
    @Shadow
    @Nullable
    protected Slot hoveredSlot;

    private AbstractContainerScreenMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @ModifyExpressionValue(method = "mouseClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;hasInfiniteMaterials()Z"))
    private final boolean darkutils$fixMiddleClick(final boolean original) {
        // Overriding isInCreativeMode here is fine because Hypixel allows and uses middle click as a feature to disable Witherborn ability of Wither armor sets.
        // In Vanilla 1.8, you could always middle-click. In 1.21, Mojang put this creative mode check. We simply override it to restore old behavior so this is safe.
        return DarkUtilsConfig.INSTANCE.middleClickFix || original;
    }

    @Inject(method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V", at = @At("HEAD"), cancellable = true)
    private final void darkutils$cancelClickSlotIfApplicable(@Nullable final Slot slot, final int slotId, final int button, @NotNull final ContainerInput actionType, @NotNull final CallbackInfo ci) {
        if (null != slot && new SlotClickEvent((AbstractContainerScreen<?>) (Object) this, slotId, slot).triggerAndCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractTooltip", at = @At("HEAD"), cancellable = true)
    private final void darkutils$extractBackpackPreview(@NotNull final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, @NotNull final CallbackInfo ci) {
        if (DarkUtilsConfig.INSTANCE.backpackPreview && LocationUtils.isInSkyblock()
                && (Object) this instanceof final ContainerScreen screen
                && BackpackPreview.extract(screen, this.hoveredSlot, graphics, mouseX, mouseY)) {
            ci.cancel();
        }
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private final void darkutils$saveBackpackPreview(@NotNull final CallbackInfo ci) {
        if (DarkUtilsConfig.INSTANCE.backpackPreview && LocationUtils.isInSkyblock()
                && (Object) this instanceof final ContainerScreen screen) {
            BackpackPreview.onClose(screen);
        }
    }
}
