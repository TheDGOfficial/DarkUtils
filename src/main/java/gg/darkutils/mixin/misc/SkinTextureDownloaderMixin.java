package gg.darkutils.mixin.misc;

import gg.darkutils.config.DarkUtilsConfig;
import net.minecraft.client.renderer.texture.SkinTextureDownloader;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(SkinTextureDownloader.class)
final class SkinTextureDownloaderMixin {
    private SkinTextureDownloaderMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @NotNull
    @ModifyVariable(method = "downloadAndRegisterSkin", at = @At("HEAD"), argsOnly = true, name = "url")
    private final String darkutils$useHttpsUrl(@NotNull final String url) {
        return DarkUtilsConfig.INSTANCE.useHttpsForSkins ? url.replace("http://", "https://") : url;
    }
}
