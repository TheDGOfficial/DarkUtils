package gg.darkutils.mixin;

import gg.darkutils.config.DarkUtilsConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.PlayerListHud;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerListHud.class)
final class PlayerListHudMixin {
    private PlayerListHudMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V"
            )
    )
    private final void darkutils$transparentPlayerListIfEnabled(@NotNull final DrawContext ctx, final int x1, final int y1, final int x2, final int y2, final int color) {
        // do nothing -> no background drawn = transparent player list
        if (!DarkUtilsConfig.INSTANCE.transparentPlayerList) {
            ctx.fill(x1, y1, x2, y2, color);
        }
    }
}
