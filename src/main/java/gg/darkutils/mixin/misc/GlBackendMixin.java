package gg.darkutils.mixin.misc;

import com.mojang.renderpearl.api.device.GpuDebugOptions;
import com.mojang.renderpearl.backend.opengl.GlBackend;
import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.feat.performance.OpenGLVersionOverride;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.sdl.SDLVideo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GlBackend.class)
final class GlBackendMixin {
    private GlBackendMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Redirect(method = "createWindow", at = @At(value = "INVOKE", target = "Lorg/lwjgl/sdl/SDLVideo;SDL_GL_SetAttribute(II)Z", remap = false))
    private final boolean darkutils$modifyOpenGLVersion(final int attribute, final int value) {
        final var version = switch (attribute) {
            case SDLVideo.SDL_GL_CONTEXT_MAJOR_VERSION -> OpenGLVersionOverride.getGLMajorVersion(value);
            case SDLVideo.SDL_GL_CONTEXT_MINOR_VERSION -> OpenGLVersionOverride.getGLMinorVersion(value);
            default -> value;
        };
        return SDLVideo.SDL_GL_SetAttribute(attribute, version);
    }

    @ModifyVariable(method = "createDevice", at = @At("HEAD"), argsOnly = true)
    @NotNull
    private final GpuDebugOptions darkutils$disableGlDebugIfEnabled(@NotNull final GpuDebugOptions options) {
        return DarkUtilsConfig.INSTANCE.disableGlDebug ? new GpuDebugOptions(0, false, false, false) : options;
    }
}
