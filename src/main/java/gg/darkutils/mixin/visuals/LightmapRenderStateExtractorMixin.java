package gg.darkutils.mixin.visuals;

import gg.darkutils.DarkUtils;
import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.utils.BasicTriState;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.state.LightmapRenderState;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.Holder;
import net.minecraft.world.level.dimension.DimensionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapRenderStateExtractor.class)
final class LightmapRenderStateExtractorMixin {
    @Shadow
    private boolean needsUpdate;
    @Unique
    @Nullable
    private BasicTriState darkutils$fullbrightAtLastUpdate;
    @Unique
    @Nullable
    private BasicTriState darkutils$nightVisionAtLastUpdate;

    private LightmapRenderStateExtractorMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Unique
    @NotNull
    private final BasicTriState darkutils$getFullbrightAtLastUpdate() {
        if (null == this.darkutils$fullbrightAtLastUpdate) {
            this.darkutils$fullbrightAtLastUpdate = BasicTriState.DEFAULT;
        }

        return this.darkutils$fullbrightAtLastUpdate;
    }

    @Unique
    @NotNull
    private final BasicTriState darkutils$getNightVisionAtLastUpdate() {
        if (null == this.darkutils$nightVisionAtLastUpdate) {
            this.darkutils$nightVisionAtLastUpdate = BasicTriState.DEFAULT;
        }

        return this.darkutils$nightVisionAtLastUpdate;
    }

    @Unique
    private final boolean darkutils$forceUpdate() {
        final var fb = DarkUtilsConfig.INSTANCE.fullbright;
        final var lastFb = this.darkutils$getFullbrightAtLastUpdate();

        if (BasicTriState.of(fb) != lastFb && BasicTriState.DEFAULT != lastFb) {
            return true;
        }

        final var nv = DarkUtilsConfig.INSTANCE.nightVision;
        final var lastNv = this.darkutils$getNightVisionAtLastUpdate();

        return BasicTriState.of(nv) != lastNv && BasicTriState.DEFAULT != lastNv;
    }

    @Inject(method = "extract", at = @At("HEAD"), cancellable = true)
    private final void darkutils$stopLightUpdatesIfEnabled(@NotNull final LightmapRenderState state, final float tickProgress, @NotNull final CallbackInfo ci) {
        final var stopLightUpdates = DarkUtilsConfig.INSTANCE.stopLightUpdates;
        final var forced = this.darkutils$forceUpdate();

        if (stopLightUpdates && !forced) {
            ci.cancel();
            return;
        }

        if (forced) {
            this.needsUpdate = true;
        }

        if (this.needsUpdate) {
            this.darkutils$fullbrightAtLastUpdate = BasicTriState.of(DarkUtilsConfig.INSTANCE.fullbright);
            this.darkutils$nightVisionAtLastUpdate = BasicTriState.of(DarkUtilsConfig.INSTANCE.nightVision);
        }
    }

    /*@Redirect(method = "extract", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/dimension/DimensionType;ambientLight()F"))
    private final float darkutils$getAmbientLight(@NotNull final DimensionType dimensionType) {
        return DarkUtilsConfig.INSTANCE.fullbright ? 1.0F : dimensionType.ambientLight();
    }*/

    @Redirect(method = "extract", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F", ordinal = 0))
    private final float darkutils$fullbrightIfEnabled(final float first, final float second) {
        return Math.max(first, DarkUtilsConfig.INSTANCE.fullbright ? 1_600.0F : second);
    }

    @Redirect(method = "extract", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;hasEffect(Lnet/minecraft/core/Holder;)Z", ordinal = 0))
    private final boolean darkutils$overrideNightVisionIfEnabled(@NotNull final LocalPlayer player, @NotNull final Holder<MobEffect> effect) {
        if (MobEffects.NIGHT_VISION == effect) {
            return DarkUtilsConfig.INSTANCE.nightVision || player.hasEffect(effect);
        }

        // Won't be the case unless vanilla moved hasStatusEffect around messing with our ordinal
        throw new IllegalStateException("@fileName@ needs updating, ordinal for hasStatusEffect did not match (" + DarkUtils.class.getSimpleName() + ')');
    }
}
