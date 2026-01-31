package gg.darkutils.mixin.accessors;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.EntityEquipment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@FunctionalInterface
@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Accessor
    EntityEquipment getEquipment();
}
