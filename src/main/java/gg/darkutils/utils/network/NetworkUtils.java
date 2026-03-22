package gg.darkutils.utils.network;

import java.io.IOException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

public final class NetworkUtils {
    @NotNull
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private NetworkUtils() {
        super();

        throw new UnsupportedOperationException("static utility class");
    }

    @NotNull
    public static final SendableHttpRequest newGetRequest(@NotNull final String url, @NotNull final List<Map.Entry<String, String>> headers) {
        final var builder = HttpRequest.newBuilder().uri(URI.create(url));

        NetworkExtras.applyExtraSettings(builder);
        headers.forEach(e -> builder.header(e.getKey(), e.getValue()));

        return new SendableHttpRequest(
            builder
                .GET()
                .build()
        );
    }

    public record SendableHttpRequest(@NotNull HttpRequest request) {
        @NotNull
        public HttpResponse<String> sendTextual() throws IOException, InterruptedException {
            return NetworkUtils.HTTP_CLIENT.send(this.request, HttpResponse.BodyHandlers.ofString());
        }
    }
}
