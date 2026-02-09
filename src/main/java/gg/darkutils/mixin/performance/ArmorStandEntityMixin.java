package gg.darkutils.mixin.performance;

import gg.darkutils.feat.performance.ArmorStandCustomRenderState;
import net.minecraft.entity.decoration.ArmorStandEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ArmorStandEntity.class)
public final class ArmorStandEntityMixin implements ArmorStandCustomRenderState {
    @Unique
    private boolean darkutils$shouldSkipRender;

    private ArmorStandEntityMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Override
    public final boolean darkutils$shouldSkipRender() {
        return this.darkutils$shouldSkipRender;
    }

    @Override
    public final void darkutils$setShouldSkipRender(final boolean shouldSkipRender) {
        this.darkutils$shouldSkipRender = shouldSkipRender;
    }
}

