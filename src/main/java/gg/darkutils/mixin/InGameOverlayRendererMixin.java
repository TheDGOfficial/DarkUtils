package gg.darkutils.mixin;

import gg.darkutils.config.DarkUtilsConfig;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameOverlayRenderer.class)
final class InGameOverlayRendererMixin {
    private InGameOverlayRendererMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true)
    private static final void darkutils$skipRenderingFireOverlayIfEnabled(@NotNull final MatrixStack matrices, @NotNull final VertexConsumerProvider vertexConsumers, @NotNull final CallbackInfo ci) {
        if (DarkUtilsConfig.INSTANCE.hideFireOverlay) {
            ci.cancel();
        }
    }
}
