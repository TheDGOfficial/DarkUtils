package gg.darkutils.utils;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public final class PrettyUtils {
    private static final long SECONDS_PER_DAY = TimeUnit.DAYS.toSeconds(1L);
    private static final long SECONDS_PER_HOUR = TimeUnit.HOURS.toSeconds(1L);
    private static final long SECONDS_PER_MINUTE = TimeUnit.MINUTES.toSeconds(1L);

    private static final long HUNDRED_MS_AS_NANOS = TimeUnit.MILLISECONDS.toNanos(100L);

    private static final long ONE_MINUTE_IN_NS = TimeUnit.MINUTES.toNanos(1L);

    private PrettyUtils() {
        super();

        throw new UnsupportedOperationException("static utility class");
    }

    @NotNull
    public static final String formatPercentage(final double value) {
        // truncate to 1 decimal place without rounding
        final var scaled = Math.floor(value * 10.0) / 10.0;

        // if the result is effectively integer, drop the decimal
        final var dropped = (long) scaled;
        return (MathUtils.isNearEqual(scaled, dropped) ? dropped : scaled) + "%";
    }

    @NotNull
    public static final String formatSeconds(final long seconds) {
        if (60L > seconds) {
            return seconds + "s";
        }

        var remainingSeconds = seconds;

        final var days = remainingSeconds / PrettyUtils.SECONDS_PER_DAY;
        remainingSeconds -= days * PrettyUtils.SECONDS_PER_DAY;

        final var hours = remainingSeconds / PrettyUtils.SECONDS_PER_HOUR;
        remainingSeconds -= hours * PrettyUtils.SECONDS_PER_HOUR;

        final var minutes = remainingSeconds / PrettyUtils.SECONDS_PER_MINUTE;
        remainingSeconds -= minutes * PrettyUtils.SECONDS_PER_MINUTE;

        final var builder = new StringBuilder(8);
        var needsSpace = false;

        if (0L < days) {
            builder.append(days).append('d');
            needsSpace = true;
        }

        if (0L < hours) {
            if (needsSpace) {
                builder.append(' ');
            }
            builder.append(hours).append('h');
            needsSpace = true;
        }

        if (0L < minutes) {
            if (needsSpace) {
                builder.append(' ');
            }
            builder.append(minutes).append('m');
            needsSpace = true;
        }

        if (0L < remainingSeconds || !needsSpace) {
            if (needsSpace) {
                builder.append(' ');
            }
            builder.append(remainingSeconds).append('s');
        }

        return builder.toString();
    }

    @NotNull
    public static final String prettifyNanosToSeconds(final long nanos) {
        return nanos < PrettyUtils.ONE_MINUTE_IN_NS ? TimeUnit.NANOSECONDS.toSeconds(nanos) + "s" : PrettyUtils.formatNanosAsSeconds(nanos);
    }

    @NotNull
    public static final String formatNanosAsSeconds(final long nanos) {
        if (0L >= nanos) {
            return "0s";
        }

        // truncate to 1 decimal (no rounding up - monotonic-safe)
        final var secondsTimes10 = nanos / PrettyUtils.HUNDRED_MS_AS_NANOS;

        final var wholeSeconds = secondsTimes10 / 10L;

        if (60L > wholeSeconds) {
            final var decimal = secondsTimes10 % 10L;
            return 0L == decimal
                    ? wholeSeconds + "s"
                    : wholeSeconds + "." + decimal + 's';
        }

        // fall back to existing formatter for large values
        return PrettyUtils.formatSeconds(wholeSeconds);
    }
}

