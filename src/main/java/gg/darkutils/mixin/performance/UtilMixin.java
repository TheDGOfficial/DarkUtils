package gg.darkutils.mixin.performance;

import com.llamalad7.mixinextras.sugar.Local;
import gg.darkutils.DarkUtils;
import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.utils.TickUtils;
import net.minecraft.util.Util;
import net.minecraft.util.TimeSource;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Mixin(Util.class)
final class UtilMixin {
    @Shadow
    private static TimeSource.NanoTimeSource timeSource;
    @Unique
    private static boolean darkutils$overrideSuccessful;

    private UtilMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static final void darkutils$postclinit(@NotNull final CallbackInfo ci) {
        try {
            TickUtils.awaitLocalPlayer(player -> { // Player will only be available after all Mixins are applied and executors are created.
                if (DarkUtilsConfig.INSTANCE.useVirtualThreadsForTextureDownloading && !UtilMixin.darkutils$overrideSuccessful) {
                    DarkUtils.warn("@fileName@", "Overriding texture downloading executor from cached thread pool to virtual thread per task executor failed. Please notify developers to update the necessary mixin(s).");
                }
            });
        } catch (final Throwable error) {
            // We must catch all errors so that we do not fail loading of the vanilla util class and hard crash the game if any of our code above throws
            DarkUtils.error("@fileName@", "Error during post-<clinit> code", error);
        }
    }

    /**
     * Redirects the call to Executors.newCachedThreadPool() in createIoWorker()
     * to use a virtual thread executor instead, if the namePrefix and daemon
     * conditions are met.
     */
    @Redirect(
            method = "makeIoExecutor",
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
            UtilMixin.darkutils$overrideSuccessful = true;

            return vtExecutor;
        }

        return Executors.newCachedThreadPool(threadFactory);
    }

    /**
     * The timeSource field defaults to System#nanoTime in Minecraft's Util class, but it's later overridden to call glfwGetTime in Minecraft's constructor.
     * The glfwGetTime essentially reads the same vDSO clock from the kernel with clock_gettime, but is slower due to JDK special-casing it's own method: https://github.com/openjdk/jdk/blob/c539fc775115beb0e6dd4648f0c077e6f898f18e/src/hotspot/share/opto/library_call.cpp#L3358-L3372 - the JDK delibaretely inlines it's own nanoTime, whereas it knows nothing about an external, regular native method and so can not inline it.
     *
     * Furthermore, the LWJGL bindings default to using the new FFM API to call this method when running on Java 25, which is slower than the traditional JNI - confirmed by issues in the LWJGL repository, such as https://github.com/LWJGL/lwjgl3/issues/1111#issuecomment-4597101927 - that's more for memory access and not just a method call, but profiler results confirm that LWJGL call incurs a longer stack:
     *
     * org.lwjgl.glfw.GLFW.glfwGetTime()0.00%
     *    org.lwjgl.system.JNI.invokeD()0.00%
     *        org.lwjgl.system.JNIBindingsImpl.0x000000004c208000.invokeD()0.00%
     *            java.lang.invoke.LambdaForm$MH.0x000000004c72a800.invokeExact_MT()0.00%
     *                java.lang.invoke.LambdaForm$MH.0x000000004c868800.invoke()0.00%
     *                    java.lang.invoke.LambdaForm$DMH.0x000000004c728c00.invokeStatic()0.00%
     *                        jdk.internal.foreign.abi.DowncallStub.0x000000004c728800.invoke()0.00%
     *                            java.lang.invoke.LambdaForm$MH.0x000000004c72b000.invokeExact_MT()0.00%
     *                                java.lang.invoke.LambdaForm$MH.0x000000004c868c00.invoke()0.00%
     *                                    nep_invoker_blob (native)0.00%
     *                                        libglfw.so.glfwGetTime()0.00%
     *                                            libglfw.so._glfwPlatformGetTimerValue()0.00%
     *                                                libc.so.6.clock_gettime()0.00%
     *                                                    vdso.clock_gettime()0.00%
     *
     * compared to calling nanoTime(), which doesn't even show up here because it is inlined to call libjvm.so's function directly (special-case optimization by the JVM at https://github.com/openjdk/jdk/blob/c539fc775115beb0e6dd4648f0c077e6f898f18e/src/hotspot/share/opto/library_call.cpp#L3358-L3372):
     *
     * libjvm.so.os::javaTimeNanos()0.00%
     *     libc.so.6.clock_gettime()0.00%
     *         vdso.clock_gettime()0.00%
     *
     * Both calls ultimately depend on the speed of sytem clock - if it's using HPET or ACPI instead of TSC for example, it will be ultimately slow, and otherwise fast. It is just that the JDK's own method is very slightly faster and results in less non-inlined method calls/less methods on the stack. So this is a very tiny, but real optimization.
     */
    @Overwrite
    public static final long getNanos() {
        return DarkUtilsConfig.INSTANCE.optimizeClocksource ? System.nanoTime() : timeSource.getAsLong();
    }
}

