package gg.darkutils.mixin.performance;

import gg.darkutils.config.DarkUtilsConfig;
import net.minecraft.client.renderer.LevelRenderer;
import com.mojang.blaze3d.textures.FilterMode;
import org.objectweb.asm.Opcodes;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
final class LevelRendererMixin {
    private LevelRendererMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Inject(method = "doEntityOutline", at = @At("HEAD"), cancellable = true)
    private final void darkutils$drawEntityOutlinesFramebuffer$disableIfEnabled(@NotNull final CallbackInfo ci) {
        if (DarkUtilsConfig.INSTANCE.disableGlowing) {
            ci.cancel();
        }
    }

    @Redirect(method = "method_62214", at = @At(value = "FIELD", target = "Lcom/mojang/blaze3d/textures/FilterMode;LINEAR:Lcom/mojang/blaze3d/textures/FilterMode;", opcode = Opcodes.GETSTATIC))
    @NotNull
    private final FilterMode darkutils$getFilterMode() {
        return DarkUtilsConfig.INSTANCE.nearestFilteringMode ? FilterMode.NEAREST : FilterMode.LINEAR;
    }
}
