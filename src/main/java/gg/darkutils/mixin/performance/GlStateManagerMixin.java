package gg.darkutils.mixin.performance;

import gg.darkutils.config.DarkUtilsConfig;
import com.mojang.blaze3d.opengl.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

@Mixin(GlStateManager.class)
final class GlStateManagerMixin {
    private GlStateManagerMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Unique
    private static int viewportX = -1;

    @Unique
    private static int viewportY = -1;

    @Unique
    private static int viewportWidth = -1;

    @Unique
    private static int viewportHeight = -1;

    @Overwrite
    public static final void _viewport(final int x, final int y, final int width, final int height) {
        final var cachedX = GlStateManagerMixin.viewportX;
        final var cachedY = GlStateManagerMixin.viewportY;
        final var cachedWidth = GlStateManagerMixin.viewportWidth;
        final var cachedHeight = GlStateManagerMixin.viewportHeight;

        final var changed = -1 == cachedX || -1 == cachedY || -1 == cachedWidth || -1 == cachedHeight || x != cachedX || y != cachedY || width != cachedWidth || height != cachedHeight;

        if (!DarkUtilsConfig.INSTANCE.viewportCache || changed) {
            GL11.glViewport(x, y, width, height);
        }

        // Still update the tracked last viewport values in case user enables the feature in runtime to not cause undefined behaviour
        GlStateManagerMixin.viewportX = x;
        GlStateManagerMixin.viewportY = y;
        GlStateManagerMixin.viewportWidth = width;
        GlStateManagerMixin.viewportHeight = height;
    }
}

