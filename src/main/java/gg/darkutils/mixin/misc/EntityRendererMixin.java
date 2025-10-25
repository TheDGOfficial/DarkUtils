package gg.darkutils.mixin.misc;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.events.RenderEntityEvent;
import gg.darkutils.events.base.EventRegistry;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
final class EntityRendererMixin<T extends Entity> {
    private EntityRendererMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private final void darkutils$skipRenderingArmorStandIfEnabled(@NotNull final T entity, @NotNull final Frustum frustum, final double x, final double y, final double z, @NotNull final CallbackInfoReturnable<Boolean> cir) {
        if (EventRegistry.centralRegistry().triggerEvent(new RenderEntityEvent(entity)).isCancelled()) {
            cir.setReturnValue(false);
        }
    }

    @Redirect(method = "renderLabelIfPresent", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/font/TextRenderer;draw(Lnet/minecraft/text/Text;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;II)V"))
    private final void darkutils$renderTransparentNametagIfEnabled(final TextRenderer instance, @NotNull final Text text, final float x, final float y, final int color, final boolean shadow, @NotNull final Matrix4f matrix, @NotNull final VertexConsumerProvider vertexConsumers, final TextRenderer.@NotNull TextLayerType layerType, final int backgroundColor, final int light) {
        instance.draw(text, x, y, color, shadow, matrix, vertexConsumers, layerType, DarkUtilsConfig.INSTANCE.transparentNametags ? 0x00FF_FFFF : backgroundColor, light);
    }
}
