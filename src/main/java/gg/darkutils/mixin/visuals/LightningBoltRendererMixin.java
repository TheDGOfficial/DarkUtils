package gg.darkutils.mixin.visuals;

import com.mojang.blaze3d.vertex.PoseStack;
import gg.darkutils.config.DarkUtilsConfig;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LightningBoltRenderer;
import net.minecraft.client.renderer.entity.state.LightningBoltRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightningBoltRenderer.class)
final class LightningBoltRendererMixin {
    private LightningBoltRendererMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Inject(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/LightningBoltRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private final void darkutils$skipLightningBoltRenderingIfEnabled(
            @NotNull final LightningBoltRenderState state,
            @NotNull final PoseStack matrices,
            @NotNull final SubmitNodeCollector orderedRenderCommandQueue,
            @NotNull final CameraRenderState cameraRenderState,
            @NotNull final CallbackInfo ci
    ) {
        if (DarkUtilsConfig.INSTANCE.noLightningBolts) {
            ci.cancel(); // Suppress all lightning bolt rendering
        }
    }
}
