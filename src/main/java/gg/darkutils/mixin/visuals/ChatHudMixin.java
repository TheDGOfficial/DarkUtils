package gg.darkutils.mixin.visuals;

import gg.darkutils.config.DarkUtilsConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(ChatHud.class)
final class ChatHudMixin {
    private ChatHudMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    /*@Redirect(
            method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;IIIZZ)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V"
            ),
            slice = @Slice(
                    from = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/hud/ChatHud;scrolledLines:I")
                    // no "to" means slice extends to end of method
            )
    )
    private final void darkutils$removeChatScrollbarIfEnabled(@NotNull final DrawContext context, final int x1, final int y1, final int x2, final int y2, final int color) {
        if (!DarkUtilsConfig.INSTANCE.removeChatScrollbar) {
            context.fill(x1, y1, x2, y2, color);
        }
        // prevent the scrollbar fill(...) call from drawing
    }*/
}
