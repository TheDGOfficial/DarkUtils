package gg.darkutils.utils.network;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;

final class NetworkExtras {
    private static final HttpClient.@NonNull Version OPTIMAL_HTTP_VERSION = NetworkExtras.detectOptimalHttpVersion();

    private NetworkExtras() {
        super();

        throw new UnsupportedOperationException("static utility class");
    }

    @NotNull
    private static final HttpClient.Version detectOptimalHttpVersion() {
        try {
            return HttpClient.Version.valueOf("HTTP_3"); // Available since Java 26
        } catch (final IllegalArgumentException ignored) {
            return HttpClient.Version.HTTP_2; // Fallback to baseline of HTTP/2
        }
    }

    public static final void applyExtraSettings(@NotNull final HttpRequest.Builder builder) {
        builder.version(NetworkExtras.OPTIMAL_HTTP_VERSION);
    }
}
