package gg.darkutils.mixin.performance;

import com.llamalad7.mixinextras.sugar.Local;
import gg.darkutils.config.DarkUtilsConfig;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.world.World;
import net.minecraft.world.chunk.BlockEntityTickInvoker;
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

@Mixin(World.class)
public final class WorldMixin {
    @Final
    @Shadow
    @NotNull
    private List<BlockEntityTickInvoker> blockEntityTickers;
    @Unique
    @Nullable
    private ReferenceOpenHashSet<BlockEntityTickInvoker> toRemove;

    private WorldMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Inject(at = @At(target = "Ljava/util/List;iterator()Ljava/util/Iterator;", value = "INVOKE"), method = "tickBlockEntities")
    private final void darkutils$instantiateToRemove(@NotNull final CallbackInfo ci) {
        if (DarkUtilsConfig.INSTANCE.blockEntityUnloadLagFix) {
            final var toRemoveLocal = this.toRemove = new ReferenceOpenHashSet<>();
            toRemoveLocal.add(null);
        }
    }

    @Redirect(at = @At(target = "Ljava/util/Iterator;remove()V", value = "INVOKE"), method = "tickBlockEntities")
    private final void darkutils$addToRemove(@NotNull final Iterator<BlockEntityTickInvoker> instance, @Local @NotNull final BlockEntityTickInvoker blockEntityTickInvoker) {
        if (DarkUtilsConfig.INSTANCE.blockEntityUnloadLagFix) {
            final var toRemoveLocal = this.toRemove;
            if (null != toRemoveLocal) { // may happen if feature is toggled in between calls
                toRemoveLocal.add(blockEntityTickInvoker);
            }
        }
    }

    @Inject(at = @At("TAIL"), method = "tickBlockEntities")
    private final void darkutils$removeAll(@NotNull final CallbackInfo ci) {
        if (DarkUtilsConfig.INSTANCE.blockEntityUnloadLagFix) {
            final var toRemoveLocal = this.toRemove;
            if (null != toRemoveLocal) { // may happen if feature is toggled in between calls
                this.blockEntityTickers.removeAll(toRemoveLocal);
            }
        }
        this.toRemove = null; // always null-out even if feature not enabled to not create memory leaks under race conditions
    }
}
