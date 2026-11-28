package gg.darkutils.mixin.visuals;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.utils.LocationUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemStack.class)
final class ItemStackMixin {
    private ItemStackMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @ModifyExpressionValue(method = "addToTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/component/TooltipDisplay;shows(Lnet/minecraft/core/component/DataComponentType;)Z"))
    private final boolean darkutils$showTooltipComponent(final boolean visible, @Local @NotNull final TooltipProvider component) {
        return visible && !(component instanceof ItemEnchantments
                && DarkUtilsConfig.INSTANCE.hideVanillaEnchantTooltip && LocationUtils.isInSkyblock());
    }
}
