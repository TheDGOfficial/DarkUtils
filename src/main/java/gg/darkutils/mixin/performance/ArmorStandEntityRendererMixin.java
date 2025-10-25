package gg.darkutils.mixin.performance;

import gg.darkutils.events.RenderEntityEvent;
import gg.darkutils.events.base.EventRegistry;
import net.minecraft.client.render.entity.ArmorStandEntityRenderer;
import net.minecraft.entity.decoration.ArmorStandEntity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ArmorStandEntityRenderer.class)
final class ArmorStandEntityRendererMixin {
    private ArmorStandEntityRendererMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Redirect(method = "hasLabel", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/decoration/ArmorStandEntity;isCustomNameVisible()Z"))
    private final boolean darkutils$skipRenderingLabelIfEnabled(@NotNull final ArmorStandEntity armorStand) {
        return armorStand.isCustomNameVisible() && !EventRegistry.centralRegistry().triggerEvent(new RenderEntityEvent(armorStand)).isCancelled();
    }
}
