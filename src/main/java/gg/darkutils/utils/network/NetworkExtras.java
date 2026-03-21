package gg.darkutils.utils.network;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;

import org.jetbrains.annotations.NotNull;

public final class NetworkExtras {
    private static final HttpClient.Version OPTIMAL_HTTP_VERSION = NetworkExtras.detectOptimalHttpVersion();

    @NotNull
    private static final HttpClient.Version detectOptimalHttpVersion() {
        try {
            return HttpClient.Version.valueOf("HTTP_3");
        } catch (final IllegalArgumentException ignored) {
            return HttpClient.Version.HTTP_2;
        }
    }

    public static final void applyExtraSettings(@NotNull final HttpRequest.Builder builder) {
        builder.version(NetworkExtras.OPTIMAL_HTTP_VERSION);
    }
}
