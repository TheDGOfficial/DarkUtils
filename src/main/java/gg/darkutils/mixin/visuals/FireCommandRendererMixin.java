package gg.darkutils.mixin.visuals;

import gg.darkutils.config.DarkUtilsConfig;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.FireCommandRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.texture.AtlasManager;
import net.minecraft.client.util.math.MatrixStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FireCommandRenderer.class)
final class FireCommandRendererMixin {
    private FireCommandRendererMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack$Entry;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/render/entity/state/EntityRenderState;Lorg/joml/Quaternionf;Lnet/minecraft/client/texture/AtlasManager;)V", at = @At("HEAD"), cancellable = true)
    private final void darkutils$skipRenderFireOnEntitiesIfEnabled(@NotNull final MatrixStack.Entry matricesEntry, @NotNull final VertexConsumerProvider vertexConsumers, @NotNull final EntityRenderState renderState, @NotNull final Quaternionf rotation, @NotNull final AtlasManager atlasManager, @NotNull final CallbackInfo ci) {
        if (DarkUtilsConfig.INSTANCE.noBurningEntities) {
            ci.cancel();
        }
    }
}
