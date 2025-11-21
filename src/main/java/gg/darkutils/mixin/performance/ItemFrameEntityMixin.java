package gg.darkutils.mixin.performance;

import gg.darkutils.config.DarkUtilsConfig;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import com.llamalad7.mixinextras.sugar.Local;

@Mixin(ItemFrameEntity.class)
public final class ItemFrameEntityMixin {
    private ItemFrameEntityMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Redirect(at = @At(target = "Lnet/minecraft/item/ItemStack;isEmpty()Z", value = "INVOKE", ordinal = 1), method = "setHeldItemStack(Lnet/minecraft/item/ItemStack;Z)V")
    private final boolean darkutils$shouldPlaySound(@NotNull final ItemStack value, @Local(argsOnly = true) final boolean update) {
        return value.isEmpty() || (!update && !DarkUtilsConfig.INSTANCE.itemFrameSoundFix);
    }
}
