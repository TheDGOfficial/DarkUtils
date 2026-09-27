package gg.darkutils.mixin.performance;

import gg.darkutils.config.DarkUtilsConfig;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(targets = "com.mojang.blaze3d.Blaze3D")
final class Blaze3DMixin {
    private Blaze3DMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Overwrite
    public static final double getTime() {
        return DarkUtilsConfig.INSTANCE.optimizeClocksource ? System.nanoTime() / 1_000_000_000.0 : GLFW.glfwGetTime();
    }
}
