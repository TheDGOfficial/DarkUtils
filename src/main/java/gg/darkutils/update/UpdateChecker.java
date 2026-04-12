package gg.darkutils.update;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import gg.darkutils.DarkUtils;
import gg.darkutils.utils.JavaUtils;
import gg.darkutils.utils.LazyConstants;
import gg.darkutils.utils.Pair;
import gg.darkutils.utils.RenderUtils;
import gg.darkutils.utils.TickUtils;
import gg.darkutils.utils.network.NetworkUtils;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public final class UpdateChecker {
    @NotNull
    private static final String OWNER = "TheDGOfficial";

    @NotNull
    private static final String REPO = "DarkUtils";

    @NotNull
    private static final String API_URL = "https://api.github.com/repos/" + UpdateChecker.OWNER + '/' + UpdateChecker.REPO + "/releases/latest";

    @NotNull
    private static final String USER_AGENT = "DarkUtils-UpdateChecker";

    @NotNull
    private static final Gson GSON = new Gson();

    @NotNull
    private static final Supplier<Executor> UPDATE_CHECKER_EXECUTOR = LazyConstants.lazyConstantOf(() -> Executors.newSingleThreadExecutor(r -> Thread.ofPlatform()
            .name("DarkUtils Update Checker Thread")
            .daemon(true)
            .unstarted(r)));

    private UpdateChecker() {
        super();

        throw new UnsupportedOperationException("static utility class");
    }

    /**
     * Runs update check in background on a separate thread to not block render thread, then runs the callback back in render thread with the result once done.
     * <p>
     * The callback must not be null.
     * <p>
     * Throws {@link IllegalStateException} if not called from render thread initially. This method will return instantly, continue from the callback if you
     * want to inspect the result or otherwise act on the result.
     */
    public static final void checkUpdateAndRunCallbackOnRenderThread(final @NotNull BiConsumer<UpdateChecker.@NotNull UpdateCheckerResult, @NotNull Optional<UpdateChecker.GitHubRelease>> callback) {
        Objects.requireNonNull(callback, "callback");
        RenderUtils.validateRenderThread(); // will throw if not called from render thread

        UpdateChecker.checkUpdateAndRunCallback((result, release) -> TickUtils.runImmediatelyOrNextTick(() -> callback.accept(result, release))); // checks in background, then runs callback on render thread
    }

    /**
     * Runs update check in background on a separate thread to not block render thread, then runs the callback on the same thread (not render thread).
     * <p>
     * The callback must not be null.
     */
    private static final void checkUpdateAndRunCallback(final @NotNull BiConsumer<UpdateChecker.@NotNull UpdateCheckerResult, @NotNull Optional<UpdateChecker.GitHubRelease>> callback) {
        Objects.requireNonNull(callback, "callback");

        // runs callback on update checker thread
        UpdateChecker.UPDATE_CHECKER_EXECUTOR.get().execute(() -> {
            final var res = UpdateChecker.checkUpdates();
            callback.accept(res.first(), res.second());
        });
    }

    /**
     * Blocking update check. Should NOT be called on main thread.
     */
    private static final @NotNull Pair<UpdateChecker.@NotNull UpdateCheckerResult, @NotNull Optional<UpdateChecker.GitHubRelease>> checkUpdates() {
        final var currentVersionRaw = DarkUtils.getVersion();

        if ("unknown".equals(currentVersionRaw)) {
            return new Pair<>(UpdateChecker.UpdateCheckerResult.COULD_NOT_CHECK, Optional.empty());
        }

        final var currentVersion = UpdateChecker.normalizeVersion(currentVersionRaw);

        try {
            final var release = UpdateChecker.fetchLatestRelease();

            if (null == release) {
                return new Pair<>(UpdateChecker.UpdateCheckerResult.COULD_NOT_CHECK, Optional.empty());
            }

            return new Pair<>(null == release.tag_name ? UpdateCheckerResult.COULD_NOT_CHECK : UpdateChecker.evaluateReleaseAgainstCurrent(currentVersion, release).first(), Optional.of(release));
        } catch (final InterruptedException ie) {
            Thread.currentThread().interrupt();

            throw JavaUtils.sneakyThrow(ie);
        } catch (final IOException ioe) {
            DarkUtils.error(UpdateChecker.class, "IO error whilst checking for mod updates over GitHub, current version is \"" + currentVersion + '"', ioe);

            return new Pair<>(UpdateChecker.UpdateCheckerResult.COULD_NOT_CHECK, Optional.empty());
        } catch (final Throwable tw) {
            DarkUtils.error(UpdateChecker.class, "Unexpected error whilst checking for mod updates over GitHub, current version is \"" + currentVersion + '"', tw);

            return new Pair<>(UpdateChecker.UpdateCheckerResult.COULD_NOT_CHECK, Optional.empty());
        }
    }

    @Nullable
    private static final UpdateChecker.GitHubRelease fetchLatestRelease() throws IOException, InterruptedException {
        final var request = NetworkUtils.newGetRequest(UpdateChecker.API_URL, List.of(
                Map.entry("Accept", "application/vnd.github+json"),
                Map.entry("User-Agent", UpdateChecker.USER_AGENT)
        ));

        final var response = request.sendTextual();
        final var statusCode = response.statusCode();

        if (200 > statusCode || 300 <= statusCode) {
            return null;
        }

        final var responseBody = response.body();

        try {
            final var release = UpdateChecker.GSON.fromJson(responseBody, UpdateChecker.GitHubRelease.class);

            if (null == release || null == release.tag_name) {
                DarkUtils.error(UpdateChecker.class, "GitHub release API returned JSON with missing expected fields (status=" + statusCode + ",responseBody=" + responseBody + ')');
            }

            return release;
        } catch (final JsonSyntaxException jse) {
            DarkUtils.error(UpdateChecker.class, "GitHub release API returned invalid JSON (status=" + statusCode + ",responseBody=" + responseBody + ')', jse);

            return null;
        }
    }

    private static final @NotNull Pair<UpdateChecker.@NotNull UpdateCheckerResult, UpdateChecker.@Nullable GitHubRelease> evaluateReleaseAgainstCurrent(@NotNull final String currentVersion, @NotNull final UpdateChecker.GitHubRelease release) {
        final var tagName = release.tag_name;

        if (null == tagName) {
            return new Pair<>(UpdateChecker.UpdateCheckerResult.COULD_NOT_CHECK, release);
        }

        final var latestVersion = UpdateChecker.normalizeVersion(tagName);

        final var result = UpdateChecker.compareVersions(currentVersion, latestVersion)
                .stream()
                .mapToObj(cmp -> switch (cmp) {
                    case 0 -> release.prerelease
                            ? UpdateChecker.UpdateCheckerResult.UP_TO_DATE_PRE
                            : UpdateChecker.UpdateCheckerResult.UP_TO_DATE_STABLE;

                    default -> 0 < cmp
                            ? UpdateChecker.UpdateCheckerResult.IN_DEVELOPMENT_VERSION
                            : UpdateChecker.UpdateCheckerResult.OUT_OF_DATE;
                })
                .findFirst()
                .orElse(UpdateChecker.UpdateCheckerResult.COULD_NOT_CHECK);

        return new Pair<>(result, release);
    }

    /**
     * Strips metadata like "+1.21.10" and leading 'v'.
     */
    @NotNull
    private static final String normalizeVersion(@NotNull final String version) {
        final var plusIndex = version.indexOf('+');
        final var base = 0 < plusIndex ? version.substring(0, plusIndex) : version;

        return !base.isEmpty() && 'v' == base.charAt(0) ? base.substring(1) : base;
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

    private static final @NonNull OptionalInt compareVersions(@NotNull final String current, @NotNull final String latest) {
        final var currentVersion = UpdateChecker.parseVersionOrNull(current, "current version");
        final var latestVersion = UpdateChecker.parseVersionOrNull(latest, "latest version");

        return null == currentVersion || null == latestVersion ? OptionalInt.empty() : OptionalInt.of(currentVersion.compareTo(latestVersion));

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
     * Includes extra fields for future use (changelog UI, etc.,).
     * <p>
     * name is the release title while body is description.
     */
    public record GitHubRelease(@Nullable String tag_name, @Nullable String name, @Nullable String body,
                                boolean prerelease, boolean draft, @Nullable String html_url,
                                @Nullable String published_at, @Nullable String created_at) {
        // prerelease and draft will always be false because we use the releases/latest endpoint
    }
}
