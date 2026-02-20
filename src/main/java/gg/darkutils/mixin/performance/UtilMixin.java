package gg.darkutils.mixin.performance;

import com.llamalad7.mixinextras.sugar.Local;
import gg.darkutils.DarkUtils;
import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.utils.TickUtils;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Mixin(Util.class)
final class UtilMixin {
    private UtilMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Unique
    private static boolean overrideSuccessfull;

    static {
        TickUtils.awaitLocalPlayer((player) -> { // Player will only be available after all Mixins are applied and executors are created.
            if (DarkUtilsConfig.INSTANCE.useVirtualThreadsForTextureDownloading && !UtilMixin.overrideSuccessfull) {
                DarkUtils.warn("@fileName@", "Overriding texture downloading executor from cached thread pool to virtual thread per task executor failed. Please notify developers to update the necessary mixin(s).");
            }
        });
    }

    /**
     * Redirects the call to Executors.newCachedThreadPool() in createIoWorker()
     * to use a virtual thread executor instead, if the namePrefix and daemon
     * conditions are met.
     */
    @Redirect(
            method = "createIoWorker",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/concurrent/Executors;newCachedThreadPool(Ljava/util/concurrent/ThreadFactory;)Ljava/util/concurrent/ExecutorService;",
                    remap = false
            )
    )
    @NotNull
    private static final ExecutorService darkutils$useVirtualThreadsIfEnabled(@NotNull final ThreadFactory threadFactory, @NotNull @Local(argsOnly = true) final String namePrefix, @Local(argsOnly = true) final boolean daemon) {
        // Only redirect "Download-" daemon threads and otherwise, preserve original behavior
        if (DarkUtilsConfig.INSTANCE.useVirtualThreadsForTextureDownloading && "Download-".equals(namePrefix) && daemon) {
            final var vtExecutor = Executors.newVirtualThreadPerTaskExecutor();

            DarkUtils.info("@fileName@", "Overriding texture downloading executor from cached thread pool to virtual thread per task executor");
            UtilMixin.overrideSuccessfull = true;

            return vtExecutor;
        }

        return Executors.newCachedThreadPool(threadFactory);
    }
}

