package gg.darkutils.mixin.visuals;

import gg.darkutils.config.DarkUtilsConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectsInInventory;
import net.minecraft.world.effect.MobEffectInstance;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

@Mixin(EffectsInInventory.class)
final class EffectsInInventoryMixin {
    private EffectsInInventoryMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
    private final void darkutils$cancelDrawIfEnabled(@NotNull final GuiGraphics context, @NotNull final Collection<MobEffectInstance> effects, final int x, final int height, final int mouseX, final int mouseY, final int width, @NotNull final CallbackInfo ci) {
        if (DarkUtilsConfig.INSTANCE.hideEffectsInInventory) {
            ci.cancel();
        }
    }
}
