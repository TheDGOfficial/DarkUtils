package gg.darkutils.mixin.performance;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.renderpearl.backend.opengl.GlStateManager;
import gg.darkutils.config.DarkUtilsConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GlStateManager.class)
final class GlStateManagerMixin {
    private GlStateManagerMixin() {
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
    private static int darkutils$lastPolygonFace = -1;

    @Unique
    private static int darkutils$lastPolygonMode = -1;

    @WrapWithCondition(method = "_scissorBox", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL33C;glScissor(IIII)V", remap = false))
    private static final boolean darkutils$shouldSetScissorBox(final int x, final int y, final int width, final int height) {
        if (DarkUtilsConfig.INSTANCE.cacheGLCalls && GlStateManagerMixin.darkutils$lastScissorX == x && GlStateManagerMixin.darkutils$lastScissorY == y && GlStateManagerMixin.darkutils$lastScissorWidth == width && GlStateManagerMixin.darkutils$lastScissorHeight == height) {
            return false;
        }

        // Still update the values even if !DarkUtilsConfig.INSTANCE.cacheGLCalls to prevent desync when enabling it in runtime.
        GlStateManagerMixin.darkutils$lastScissorX = x;
        GlStateManagerMixin.darkutils$lastScissorY = y;
        GlStateManagerMixin.darkutils$lastScissorWidth = width;
        GlStateManagerMixin.darkutils$lastScissorHeight = height;

        return true;
    }

    @WrapWithCondition(method = "_polygonMode", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL33C;glPolygonMode(II)V", remap = false))
    private static final boolean darkutils$shouldSetPolygonMode(final int face, final int mode) {
        if (DarkUtilsConfig.INSTANCE.cacheGLCalls && GlStateManagerMixin.darkutils$lastPolygonFace == face && GlStateManagerMixin.darkutils$lastPolygonMode == mode) {
            return false;
        }

        // Still update the values even if !DarkUtilsConfig.INSTANCE.cacheGLCalls to prevent desync when enabling it in runtime.
        GlStateManagerMixin.darkutils$lastPolygonFace = face;
        GlStateManagerMixin.darkutils$lastPolygonMode = mode;
        return true;
    }
}
