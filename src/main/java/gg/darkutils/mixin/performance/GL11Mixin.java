package gg.darkutils.mixin.performance;

import gg.darkutils.config.DarkUtilsConfig;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.system.NativeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(GL11.class)
final class GL11Mixin {
    private GL11Mixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @NativeType("GLenum")
    @Overwrite
    public static final int glGetError() {
        if (DarkUtilsConfig.INSTANCE.disableErrorCheckingEntirely) {
            return GL11C.GL_NO_ERROR;
        }
        return GL11C.glGetError();
    }
}

