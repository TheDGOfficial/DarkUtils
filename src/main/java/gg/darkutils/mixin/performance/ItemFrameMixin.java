package gg.darkutils.mixin.performance;

import com.llamalad7.mixinextras.sugar.Local;
import gg.darkutils.config.DarkUtilsConfig;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemFrame.class)
final class ItemFrameMixin {
    private ItemFrameMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Redirect(at = @At(target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z", value = "INVOKE", ordinal = 1), method = "setItem(Lnet/minecraft/world/item/ItemStack;Z)V")
    private final boolean darkutils$shouldPlaySound(@NotNull final ItemStack value, @Local(argsOnly = true) final boolean update) {
        return value.isEmpty() || !update && !DarkUtilsConfig.INSTANCE.itemFrameSoundFix;
    }
}
