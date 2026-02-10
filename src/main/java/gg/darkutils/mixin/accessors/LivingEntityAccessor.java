package gg.darkutils.mixin.accessors;

import net.minecraft.entity.EntityEquipment;
import net.minecraft.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@FunctionalInterface
@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Accessor
    @NotNull
    EntityEquipment getEquipment();
}
