package gg.darkutils.mixin.performance;

import com.llamalad7.mixinextras.sugar.Local;
import gg.darkutils.config.DarkUtilsConfig;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;
import java.util.List;

@Mixin(Level.class)
final class LevelMixin {
    @Final
    @Shadow
    @NotNull
    private List<TickingBlockEntity> blockEntityTickers;
    @Unique
    @Nullable
    private ReferenceOpenHashSet<TickingBlockEntity> darkutils$toRemove;

    private LevelMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Inject(method = "tickBlockEntities", at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;"))
    private final void darkutils$instantiateToRemove(@NotNull final CallbackInfo ci) {
        if (DarkUtilsConfig.INSTANCE.blockEntityUnloadLagFix) {
            final var toRemoveLocal = this.darkutils$toRemove = new ReferenceOpenHashSet<>();
            toRemoveLocal.add(null);
        }
    }

    @Redirect(method = "tickBlockEntities", at = @At(value = "INVOKE", target = "Ljava/util/Iterator;remove()V"))
    private final void darkutils$addToRemove(@NotNull final Iterator<TickingBlockEntity> instance, @Local @NotNull final TickingBlockEntity blockEntityTickInvoker) {
        if (DarkUtilsConfig.INSTANCE.blockEntityUnloadLagFix) {
            final var toRemoveLocal = this.darkutils$toRemove;
            if (null != toRemoveLocal) { // may happen if feature is toggled in between calls
                toRemoveLocal.add(blockEntityTickInvoker);
            }
        }
    }

    @Inject(method = "tickBlockEntities", at = @At("TAIL"))
    private final void darkutils$removeAll(@NotNull final CallbackInfo ci) {
        if (DarkUtilsConfig.INSTANCE.blockEntityUnloadLagFix) {
            final var toRemoveLocal = this.darkutils$toRemove;
            if (null != toRemoveLocal && !toRemoveLocal.isEmpty()) { // may happen if feature is toggled in between calls
                this.blockEntityTickers.removeAll(toRemoveLocal);
            }
        }
        this.darkutils$toRemove = null; // always null-out even if feature not enabled to not create memory leaks under race conditions
    }
}
