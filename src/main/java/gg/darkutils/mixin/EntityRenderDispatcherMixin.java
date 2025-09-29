package gg.darkutils.mixin;

import gg.darkutils.config.DarkUtilsConfig;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
final class EntityRenderDispatcherMixin {
    private EntityRenderDispatcherMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Inject(method = "renderFire", at = @At("HEAD"), cancellable = true)
    private final void darkutils$skipRenderFireOnEntityIfEnabled(@NotNull final MatrixStack matrices, @NotNull final VertexConsumerProvider vertexConsumers, @NotNull final EntityRenderState renderState, @NotNull final Quaternionf rotation, @NotNull final CallbackInfo ci) {
        if (DarkUtilsConfig.INSTANCE.noBurningEntities) {
            ci.cancel();
        }
    }
}
