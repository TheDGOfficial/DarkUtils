package gg.darkutils.mixin.misc;

import gg.darkutils.events.RenderEntityEvents;
import gg.darkutils.events.base.EventRegistry;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
final class EntityRendererMixin<T extends Entity> {
    private EntityRendererMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private final void darkutils$skipRenderingArmorStandIfEnabled(@NotNull final T entity, @NotNull final Frustum frustum, final double x, final double y, final double z, @NotNull final CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof final ArmorStandEntity armorStand && EventRegistry.centralRegistry().triggerEvent(new RenderEntityEvents.ArmorStandRenderEvent(armorStand)).isCancelled()) {
            cir.setReturnValue(false);
        }
    }
}
