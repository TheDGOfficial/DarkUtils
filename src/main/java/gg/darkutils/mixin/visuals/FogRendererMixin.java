package gg.darkutils.mixin.visuals;

import gg.darkutils.config.DarkUtilsConfig;
import net.minecraft.client.render.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

@Mixin(FogRenderer.class)
final class FogRendererMixin {
    private FogRendererMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @ModifyExpressionValue(method = "getFogColor", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;hasStatusEffect(Lnet/minecraft/registry/entry/RegistryEntry;)Z", ordinal = 0))
    private final boolean darkutils$overrideNightVisionIfEnabled(final boolean original) {
        return original || DarkUtilsConfig.INSTANCE.nightVision;
    }
}
