package gg.darkutils.mixin.visuals;

import gg.darkutils.config.DarkUtilsConfig;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LightningEntityRenderer;
import net.minecraft.client.render.entity.state.LightningEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightningEntityRenderer.class)
final class LightningEntityRendererMixin {
    private LightningEntityRendererMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Inject(
            method = "render(Lnet/minecraft/client/render/entity/state/LightningEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private final void darkutils$skipLightningBoltRenderingIfEnabled(
            @NotNull final LightningEntityRenderState state,
            @NotNull final MatrixStack matrices,
            @NotNull final VertexConsumerProvider vertexConsumers,
            final int light,
            @NotNull final CallbackInfo ci
    ) {
        if (DarkUtilsConfig.INSTANCE.noLightningBolts) {
            ci.cancel(); // Suppress all lightning bolt rendering
        }
    }
}
