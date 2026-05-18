package gg.darkutils.mixin.visuals;

import com.mojang.blaze3d.vertex.PoseStack;
import gg.darkutils.config.DarkUtilsConfig;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.FlameFeatureRenderer;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FlameFeatureRenderer.class)
final class FlameFeatureRendererMixin {
    private FlameFeatureRendererMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Inject(method = "renderFlame(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lorg/joml/Quaternionf;Lnet/minecraft/client/resources/model/sprite/AtlasManager;)V", at = @At("HEAD"), cancellable = true)
    private final void darkutils$skipRenderFireOnEntitiesIfEnabled(@NotNull final PoseStack.Pose matricesEntry, @NotNull final MultiBufferSource vertexConsumers, @NotNull final EntityRenderState renderState, @NotNull final Quaternionf rotation, @NotNull final AtlasManager atlasManager, @NotNull final CallbackInfo ci) {
        if (DarkUtilsConfig.INSTANCE.noBurningEntities) {
            ci.cancel();
        }
    }
}
