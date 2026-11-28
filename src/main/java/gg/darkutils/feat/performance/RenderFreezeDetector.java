package gg.darkutils.feat.performance;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class RenderFreezeDetector {
    private static final DateTimeFormatter LOG_TIMESTAMP = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss", Locale.ROOT)
            .withZone(ZoneId.systemDefault());
    private static final long REPORT_COOLDOWN_NS = TimeUnit.SECONDS.toNanos(3);
    private static final long FREEZE_TIMEOUT_NS = TimeUnit.SECONDS.toNanos(60);

    private final Thread renderThread;
    private final Thread detectorThread;
    private final @Nullable Thread timeoutThread;
    private final Path logFile;
    private final int pollIntervalMs;
    private final long thresholdNanos;
    private volatile long lastFrameNanos = System.nanoTime();
    private volatile boolean stopped;
    private boolean logWriteFailed;

    public RenderFreezeDetector(@NotNull final Path gameDirectory, final int pollIntervalMs, final int thresholdMs, final boolean exitOnFreeze) {
        this.renderThread = Thread.currentThread();
        this.logFile = gameDirectory.resolve("logs").resolve("darkutils-render-freezes.log");
        this.pollIntervalMs = pollIntervalMs;
        this.thresholdNanos = TimeUnit.MILLISECONDS.toNanos(thresholdMs);
        this.detectorThread = Thread.ofPlatform().name("DarkUtils Render Freeze Detector")
                .daemon().priority(Thread.NORM_PRIORITY).inheritInheritableThreadLocals(false)
                .unstarted(this::watchFrames);

        // A HotSpot stack walk or file write can block. The optional cutoff has its own deadline.
        this.timeoutThread = exitOnFreeze ? Thread.ofPlatform().name("DarkUtils Render Freeze Timeout")
                .daemon().priority(Thread.NORM_PRIORITY).inheritInheritableThreadLocals(false)
                .unstarted(this::exitOnTimeout) : null;
    }

    public final void start() {
        this.frameCompleted();
        this.detectorThread.start();
        if (this.timeoutThread != null) {
            this.timeoutThread.start();
        }
    }

    public final void frameCompleted() {
        this.lastFrameNanos = System.nanoTime();
    }

    public final void stop() {
        this.stopped = true;
        this.detectorThread.interrupt();
        if (this.timeoutThread != null) {
            this.timeoutThread.interrupt();
        }
    }

    private final void watchFrames() {
        long lastReportedFrame = this.lastFrameNanos - 1;
        long nextReport = this.lastFrameNanos;
        try {
            while (!this.stopped && this.renderThread.isAlive()) {
                Thread.sleep(this.pollIntervalMs);
                final long frame = this.lastFrameNanos;
                final long now = System.nanoTime();
                final long elapsed = now - frame;
                if (elapsed >= this.thresholdNanos && frame != lastReportedFrame && now - nextReport >= 0
                        && !this.stopped && frame == this.lastFrameNanos) {
                    this.reportFreeze(frame, elapsed);
                    lastReportedFrame = frame;
                    nextReport = System.nanoTime() + REPORT_COOLDOWN_NS;
                }
            }
        } catch (final InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private final void reportFreeze(final long frame, final long elapsed) {
        final var detectedAt = Instant.now();
        final var state = this.renderThread.getState();
        final var trace = this.renderThread.getStackTrace();
        if (this.stopped) {
            return;
        }

        final var report = new StringBuilder().append('[').append(LOG_TIMESTAMP.format(detectedAt)).append("] No frame completed for ")
                .append(TimeUnit.NANOSECONDS.toMillis(elapsed)).append(" ms. \"")
                .append(this.renderThread.getName()).append("\" was ").append(state).append(" at detection.\n");
        if (frame != this.lastFrameNanos) {
            report.append("\tFrame progress resumed during stack capture.\n");
        } else if (trace.length == 0) {
            report.append("\tNo stack frames available.\n");
        } else {
            for (final var element : trace) {
                report.append("\tat ").append(element).append('\n');
            }
        }

        try {
            Files.createDirectories(this.logFile.getParent());
            Files.writeString(this.logFile, report.append('\n'), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (final IOException e) {
            if (!this.stopped && !this.logWriteFailed) {
                this.logWriteFailed = true;
                System.err.println("DarkUtils could not write the render freeze log at " + this.logFile + ": " + e);
            }
        }
    }

    private final void exitOnTimeout() {
        try {
            while (!this.stopped && this.renderThread.isAlive()) {
                final long frame = this.lastFrameNanos;
                final long remaining = FREEZE_TIMEOUT_NS - (System.nanoTime() - frame);
                if (remaining > 0) {
                    TimeUnit.NANOSECONDS.sleep(remaining);
                } else if (!this.stopped && frame == this.lastFrameNanos) {
                    // Shutdown hooks can block System.exit. Arm the fallback independently before starting shutdown.
                    Thread.ofPlatform().name("DarkUtils Shutdown Timeout").daemon()
                            .inheritInheritableThreadLocals(false).start(RenderFreezeDetector::haltAfterShutdownGracePeriod);
                    System.exit(1);
                    return;
                }
            }
        } catch (final InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private static final void haltAfterShutdownGracePeriod() {
        try {
            TimeUnit.SECONDS.sleep(15);
        } catch (final InterruptedException ignored) {
            Thread.currentThread().interrupt();
            return;
        }
        Runtime.getRuntime().halt(1);
    }
}
