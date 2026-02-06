package gg.darkutils.mixin.performance;

import gg.darkutils.feat.performance.ArmorStandCustomRenderState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import net.minecraft.entity.decoration.ArmorStandEntity;

@Mixin(ArmorStandEntity.class)
public final class ArmorStandEntityMixin implements ArmorStandCustomRenderState {
    private ArmorStandEntityMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Unique
    private boolean darkutils$shouldSkipRender;

    @Override
    public final boolean darkutils$shouldSkipRender() {
        return this.darkutils$shouldSkipRender;
    }

    @Override
    public final void darkutils$setShouldSkipRender(final boolean shouldSkipRender) {
        this.darkutils$shouldSkipRender = shouldSkipRender;
    }
}

