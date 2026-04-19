package gg.darkutils.mixin.visuals;

import gg.darkutils.config.DarkUtilsConfig;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.world.level.dimension.DimensionType;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Lightmap.class)
final class LightmapMixin {
    private LightmapMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Redirect(method = "getBrightness", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/dimension/DimensionType;ambientLight()F"))
    private static final float darkutils$getAmbientLight(@NotNull final DimensionType dimensionType) {
        return DarkUtilsConfig.INSTANCE.fullbright ? 1.0F : dimensionType.ambientLight();
    }
}
