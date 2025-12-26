package gg.darkutils.mixin.bugfixes;

import gg.darkutils.config.DarkUtilsConfig;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

@Mixin(HandledScreen.class)
final class HandledScreenMixin {
    private HandledScreenMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @ModifyExpressionValue(method = "mouseClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isInCreativeMode()Z"))
    private final boolean darkutils$fixMiddleClick(final boolean original) {
        // Overriding isInCreativeMode here is fine because Hypixel allows and uses middle click as a feature to disable Witherborn ability of Wither armor sets.
        // In Vanilla 1.8, you could always middle click. In 1.21, mojang put this creative mode check. We simply override it to restore old behaviour so this is safe.
        return DarkUtilsConfig.INSTANCE.middleClickFix || original;
    }
}
