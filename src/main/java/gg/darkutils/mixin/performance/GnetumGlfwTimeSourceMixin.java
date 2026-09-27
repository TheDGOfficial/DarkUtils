package gg.darkutils.mixin.performance;

import gg.darkutils.config.DarkUtilsConfig;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Overwrite;

@Pseudo
@Mixin(targets = "me.decce.gnetum.time.GlfwTimeSource")
final class GnetumGlfwTimeSourceMixin {
    private GnetumGlfwTimeSourceMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Overwrite
    public final double get() {
        return DarkUtilsConfig.INSTANCE.optimizeClocksource ? System.nanoTime() / 1_000_000_000.0 : GLFW.glfwGetTime();
    }

    @Overwrite
    public final long nanos() {
        return DarkUtilsConfig.INSTANCE.optimizeClocksource ? System.nanoTime() : (long) (this.get() * 1_000_000_000L);
    }
}
