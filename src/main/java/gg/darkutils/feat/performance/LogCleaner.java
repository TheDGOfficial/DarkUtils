package gg.darkutils.feat.performance;

import gg.darkutils.DarkUtils;
import gg.darkutils.config.DarkUtilsConfig;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;

public final class LogCleaner {
    private LogCleaner() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final void init() {
        if (!DarkUtilsConfig.INSTANCE.logCleaner) {
            return;
        }

        final var logsDir = Path.of("logs");

        if (!Files.isDirectory(logsDir)) {
            throw new IllegalArgumentException("Not a directory: " + logsDir);
        }

        final var gzipFiles = logsDir.toFile().listFiles((file, name) -> name.endsWith(".gz"));

        if (null == gzipFiles) {
            throw new IllegalStateException("Failed to list directory: " + logsDir);
        }

        if (30 <= gzipFiles.length) {
            Arrays.sort(gzipFiles, Comparator.comparingLong(File::lastModified));

            // keep the newest 29, delete the oldest
            final var filesToDelete = gzipFiles.length - 29;

            for (var i = 0; i < filesToDelete; ++i) {
                final var gzipFile = gzipFiles[i];

                try {
                    Files.delete(gzipFile.toPath());
                    DarkUtils.LOGGER.info(DarkUtils.MOD_ID + ": {}: Deleted log file: {}", LogCleaner.class.getSimpleName(), gzipFile.getName());
                } catch (final IOException ioException) {
                    DarkUtils.logError(LogCleaner.class, "Failed to delete log file: " + gzipFile.getName(), ioException);
                }
            }
        } else {
            DarkUtils.LOGGER.info(DarkUtils.MOD_ID + ": {}: Nothing to delete, only {} log files.", LogCleaner.class.getSimpleName(), gzipFiles.length);
        }
    }
}
