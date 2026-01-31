package gg.darkutils.mixin.performance;

import gg.darkutils.feat.performance.ArmorStandOptimizer;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.ArmorStandEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.state.ArmorStandEntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ArmorStandEntityRenderer.class)
abstract class ArmorStandEntityRendererMixin<T extends ArmorStandEntity, S extends ArmorStandEntityRenderState> extends EntityRenderer<T, S> {
    private ArmorStandEntityRendererMixin(@NotNull final EntityRendererFactory.Context context) {
        super(context);

        throw new UnsupportedOperationException("mixin class");
    }

    @Redirect(method = "hasLabel", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/decoration/ArmorStandEntity;isCustomNameVisible()Z"))
    private final boolean darkutils$skipRenderingLabelIfEnabled(@NotNull final ArmorStandEntity armorStand) {
        return armorStand.isCustomNameVisible() && !ArmorStandOptimizer.shouldSkipRenderArmorStand(armorStand);
    }

    @Override
    @SuppressWarnings("unchecked")
    public final boolean shouldRender(@NotNull final T entity, @NotNull final Frustum frustum, final double x, final double y, final double z) {
        if (ArmorStandOptimizer.shouldSkipRenderArmorStand(entity)) {
            return false;
        }

        return super.shouldRender(entity, frustum, x, y, z);
    }
}
