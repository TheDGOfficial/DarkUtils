package gg.darkutils.mixin.performance;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At;

import gg.darkutils.config.DarkUtilsConfig;

import com.mojang.blaze3d.opengl.GlStateManager;

@Mixin(targets = "com.mojang.blaze3d.opengl.GlCommandEncoder")
final class GlCommandEncoderMixin {
    private GlCommandEncoderMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Unique
    private static int darkutils$lastScissorX = -1;

    @Unique
    private static int darkutils$lastScissorY = -1;

    @Unique
    private static int darkutils$lastScissorWidth = -1;

    @Unique
    private static int darkutils$lastScissorHeight = -1;

    @Unique
    private static int darkutils$lastPolygonMode = -1;

    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/opengl/GlStateManager;_scissorBox(IIII)V"))
    private static final void darkutils$scissorBox$preventUnnecessaryCallsIfEnabled(final int x, final int y, final int width, final int height) {
        if (DarkUtilsConfig.INSTANCE.cacheGLCalls && GlCommandEncoderMixin.darkutils$lastScissorX == x && GlCommandEncoderMixin.darkutils$lastScissorY == y && GlCommandEncoderMixin.darkutils$lastScissorWidth == width && GlCommandEncoderMixin.darkutils$lastScissorHeight == height) {
            return;
        }

        // Still update the values even if !DarkUtilsConfig.INSTANCE.cacheGLCalls to prevent desync when enabling it in runtime.
        GlCommandEncoderMixin.darkutils$lastScissorX = x;
        GlCommandEncoderMixin.darkutils$lastScissorY = y;
        GlCommandEncoderMixin.darkutils$lastScissorWidth = width;
        GlCommandEncoderMixin.darkutils$lastScissorHeight = height;

        GlStateManager._scissorBox(x, y, width, height);
    }

    @Redirect(method = "applyPipelineState", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/opengl/GlStateManager;_polygonMode(II)V"))
    private static final void darkutils$polygonMode$preventUnnecessaryCallsIfEnabled(final int face, final int mode) {
        if (DarkUtilsConfig.INSTANCE.cacheGLCalls && GlCommandEncoderMixin.darkutils$lastPolygonMode == mode) {
            return;
        }

        // Still update the values even if !DarkUtilsConfig.INSTANCE.cacheGLCalls to prevent desync when enabling it in runtime.
        GlCommandEncoderMixin.darkutils$lastPolygonMode = mode;

        GlStateManager._polygonMode(face, mode);
    }
}

