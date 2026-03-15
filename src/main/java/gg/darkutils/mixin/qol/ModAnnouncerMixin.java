package gg.darkutils.mixin.qol;

import gg.darkutils.config.DarkUtilsConfig;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Coerce;

import org.jetbrains.annotations.NotNull;

@Pseudo
@Mixin(targets = "moe.nea.firmament.features.misc.ModAnnouncer")
final class ModAnnouncerMixin {
    private ModAnnouncerMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @WrapMethod(method = "onServerJoin")
    private final void onServerJoin(@NotNull @Coerce final Object event, @NotNull final Operation<Void> original) {
        if (DarkUtilsConfig.INSTANCE.enableModAnnouncer) {
            original.call();
        }
    }
}

