package gg.darkutils.mixin.visuals;

import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.client.gui.Font;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.At;
import gg.darkutils.config.DarkUtilsConfig;

@Mixin(Font.class)
final class FontMixin {
    private FontMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @ModifyVariable(method = {"prepareText(Ljava/lang/String;FFIZI)Lnet/minecraft/client/gui/Font$PreparedText;", "prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;"}, at = @At("HEAD"), argsOnly = true, name = "drawShadow")
    private final boolean darkutils$noShadowInTextRenderingIfEnabled(final boolean drawShadow) {
        return !DarkUtilsConfig.INSTANCE.noShadowInTextRendering && drawShadow;
    }
}
