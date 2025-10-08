package gg.darkutils.mixin.performance;

import com.llamalad7.mixinextras.sugar.Local;
import gg.darkutils.config.DarkUtilsConfig;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
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
        return DarkUtilsConfig.INSTANCE.useVirtualThreadsForTextureDownloading && "Download-".equals(namePrefix) && daemon ? Executors.newVirtualThreadPerTaskExecutor() : Executors.newCachedThreadPool(threadFactory);
    }
}
