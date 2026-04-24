package gg.darkutils.mixin.visuals;

import com.mojang.blaze3d.vertex.PoseStack;
import gg.darkutils.config.DarkUtilsConfig;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
final class ScreenEffectRendererMixin {
    private ScreenEffectRendererMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Inject(method = "renderFire", at = @At("HEAD"), cancellable = true)
    private static final void darkutils$skipRenderingFireOverlayIfEnabled(@NotNull final PoseStack matrices, @NotNull final MultiBufferSource vertexConsumers, @NotNull final TextureAtlasSprite sprite, @NotNull final CallbackInfo ci) {
        if (DarkUtilsConfig.INSTANCE.hideFireOverlay) {
            ci.cancel();
        }
    }
}
