package gg.darkutils.mixin.performance;

import gg.darkutils.config.DarkUtilsConfig;
import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
final class MinecraftClientMixin {
    private MinecraftClientMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Redirect(at = @At(value = "INVOKE", target = "Ljava/lang/Thread;yield()V", remap = false), method = "render")
    private final void darkutils$skipYieldIfEnabled() {
        if (!DarkUtilsConfig.INSTANCE.disableYield) {
            Thread.yield();
        }
        // skip a yield call that reduces fps, the call was put to make sure rendering does not stall other threads such as chunk loading, but that's OS scheduler's job to handle, the code should utilize maximum resources so this yield call is unnecessary.
    }

    @Inject(at = @At("HEAD"), method = "run")
    private final void darkutils$adjustPriorityIfEnabled(@NotNull final CallbackInfo ci) {
        if (DarkUtilsConfig.INSTANCE.alwaysPrioritizeRenderThread) {
            // vanilla game only sets priority to max for processors with 4 or more cores, but it is best to have max priority no matter the core count.
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        }
    }
}
