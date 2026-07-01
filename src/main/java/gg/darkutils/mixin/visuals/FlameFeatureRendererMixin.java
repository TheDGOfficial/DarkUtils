package gg.darkutils.mixin.visuals;

import gg.darkutils.config.DarkUtilsConfig;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.FlameFeatureRenderer;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;

@Mixin(FlameFeatureRenderer.class)
final class FlameFeatureRendererMixin {
    private FlameFeatureRendererMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Inject(method = "buildGroup", at = @At("HEAD"), cancellable = true)
    private final void darkutils$skipRenderFireOnEntitiesIfEnabled(@NotNull final FeatureFrameContext context, @NotNull final List<FlameFeatureRenderer.Submit> submits, @NotNull final CallbackInfo ci) {
        if (DarkUtilsConfig.INSTANCE.noBurningEntities) {
            ci.cancel();
        }
    }
}
