package gg.darkutils.update;

import gg.darkutils.DarkUtils;
import gg.darkutils.utils.RenderUtils;
import gg.darkutils.utils.TickUtils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class UpdateChecker {
    @NotNull
    private static final String OWNER = "TheDGOfficial";

    @NotNull
    private static final String REPO = "DarkUtils";

    @NotNull
    private static final String API_URL = "https://api.github.com/repos/" + UpdateChecker.OWNER + "/" + UpdateChecker.REPO + "/releases/latest";

    @NotNull
    private static final String USER_AGENT = "DarkUtils-UpdateChecker";

    @NotNull
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    @NotNull
    private static final Gson GSON = new Gson();

    @NotNull
    private static final Executor UPDATE_CHECKER_EXECUTOR = Executors.newSingleThreadExecutor(r -> Thread.ofPlatform()
            .name("DarkUtils Update Checker Thread")
            .daemon(true)
            .unstarted(r));

    private UpdateChecker() {
        super();

        throw new UnsupportedOperationException("static utility class");
    }

    public enum UpdateCheckerResult {
        UP_TO_DATE_STABLE,
        UP_TO_DATE_PRE,
        OUT_OF_DATE,
        IN_DEVELOPMENT_VERSION,
        COULD_NOT_CHECK
    }

    /**
     * Minimal DTO for GitHub release API response.
     * Includes extra fields for future use (changelog UI, etc).
     * <p>
     * name is the release title while body is description.
     */
    public record GitHubRelease(@Nullable String tag_name, @Nullable String name, @Nullable String body, boolean prerelease, boolean draft, @Nullable String html_url, @Nullable String published_at, @Nullable String created_at) {
        // prerelease and draft will always be false because we use the releases/latest endpoint
    }

    /**
     * Runs update check in background on a seperate thread to not block render thread, then runs the callback back in render thread with the result once done.
     * <p>
     * The callback must not be null.
     * <p>
     * Throws {@link IllegalStateException} if not called from render thread initially. This method will return instantly, continue from the callback consumer
     * if you want to inspect the result or otherwise act on the result.
     */
    public static final void checkUpdateAndRunCallbackOnRenderThread(@NotNull final Consumer<UpdateCheckerResult> callback) {
        Objects.requireNonNull(callback, "callback");
        RenderUtils.validateRenderThread(); // will throw if not called from render thread

        UpdateChecker.checkUpdateAndRunCallback(result -> TickUtils.runImmediatelyOrNextTick(() -> callback.accept(result))); // checks in background, then runs callback on render thread
    }

    /**
     * Runs update check in background on a seperate thread to not block render thread, then runs the callback on the same thread (not render thread).
     * <p>
     * The callback must not be null.
     */
    private static final void checkUpdateAndRunCallback(@NotNull final Consumer<UpdateCheckerResult> callback) {
        Objects.requireNonNull(callback, "callback");

        UpdateChecker.UPDATE_CHECKER_EXECUTOR.execute(() -> callback.accept(UpdateChecker.checkUpdates())); // runs callback on update checker thread
    }

    /**
     * Blocking update check. Should NOT be called on main thread.
     */
    @NotNull
    private static final UpdateCheckerResult checkUpdates() {
        final var currentVersionRaw = DarkUtils.getVersion();

        if ("unknown".equals(currentVersionRaw)) {
            return UpdateCheckerResult.COULD_NOT_CHECK; // shouldn't happen unless user modified fabric.mod.json manually (unsupported)
        }

        final var currentVersion = UpdateChecker.normalizeVersion(currentVersionRaw);

        try {
            final var request = HttpRequest.newBuilder()
                    .uri(URI.create(UpdateChecker.API_URL))
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", UpdateChecker.USER_AGENT)
                    .GET()
                    .build();

            final var response = UpdateChecker.HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            final var statusCode = response.statusCode();

            if (statusCode < 200 || statusCode >= 300) {
                return UpdateCheckerResult.COULD_NOT_CHECK;
            }

            final var responseBody = response.body();
            final GitHubRelease release;

            try {
                release = UpdateChecker.GSON.fromJson(responseBody, GitHubRelease.class);
            } catch (final JsonSyntaxException jse) {
                DarkUtils.error(UpdateChecker.class, "GitHub release API returned invalid JSON (status=" + statusCode + ",responseBody=" + responseBody + ')');

                return UpdateChecker.UpdateCheckerResult.COULD_NOT_CHECK;
            }

            if (null == release || null == release.tag_name) {
                DarkUtils.error(UpdateChecker.class, "GitHub release API returned JSON with missing expected fields (status=" + statusCode + ",responseBody=" + responseBody + ')');

                return UpdateChecker.UpdateCheckerResult.COULD_NOT_CHECK;
            }

            final var latestVersion = UpdateChecker.normalizeVersion(release.tag_name);

            return UpdateChecker.compareVersions(currentVersion, latestVersion)
                    .stream()
                    .mapToObj(cmp -> switch (cmp) {
                        case 0 -> release.prerelease
                                ? UpdateCheckerResult.UP_TO_DATE_PRE
                                : UpdateCheckerResult.UP_TO_DATE_STABLE;

                        default -> cmp > 0
                                ? UpdateCheckerResult.IN_DEVELOPMENT_VERSION
                                : UpdateCheckerResult.OUT_OF_DATE;
                    })
                    .findFirst()
                    .orElse(UpdateCheckerResult.COULD_NOT_CHECK);
        } catch (final InterruptedException ie) {
            Thread.currentThread().interrupt(); // re-set the interrupted flag

            return UpdateChecker.UpdateCheckerResult.COULD_NOT_CHECK;
        } catch (final IOException ioe) {
            DarkUtils.error(UpdateChecker.class, "IO error whilst checking for mod updates over GitHub, current version is \"" + currentVersion + "\"", ioe);

            return UpdateChecker.UpdateCheckerResult.COULD_NOT_CHECK;
        } catch (final Throwable tw) {
            DarkUtils.error(UpdateChecker.class, "Unexpected error whilst checking for mod updates over GitHub, current version is \"" + currentVersion + "\"", tw);

            return UpdateChecker.UpdateCheckerResult.COULD_NOT_CHECK;
        }
    }

    /**
     * Strips metadata like "+1.21.10" and leading 'v'.
     */
    @NotNull
    private static final String normalizeVersion(@NotNull final String version) {
        final var plusIndex = version.indexOf('+');

        return plusIndex > 0 ? version.substring(0, plusIndex) : version;
    }

    @Nullable
    private static final Version parseVersionOrNull(@NotNull final String version, @NotNull final String label) {
        try {
            return Version.parse(version);
        } catch (final VersionParsingException vpe) {
            DarkUtils.error(UpdateChecker.class, "Version parse failed for " + label + ": \"" + version + '"', vpe);

            return null;
        }
    }

    private static final OptionalInt compareVersions(@NotNull final String current, @NotNull final String latest) {
        final var currentVersion = UpdateChecker.parseVersionOrNull(current, "current version");
        final var latestVersion = UpdateChecker.parseVersionOrNull(latest, "latest version");

        if (null == currentVersion || null == latestVersion) {
            return OptionalInt.empty();
        }

        return OptionalInt.of(currentVersion.compareTo(latestVersion));
    }
}
