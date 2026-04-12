package gg.darkutils;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.config.DarkUtilsConfigScreen;
import gg.darkutils.events.ReceiveGameMessageEvent;
import gg.darkutils.feat.bugfixes.CursorFix;
import gg.darkutils.feat.dungeons.AlignmentTaskSolver;
import gg.darkutils.feat.dungeons.ArrowStackWaypoints;
import gg.darkutils.feat.dungeons.AutoCloseSecretChests;
import gg.darkutils.feat.dungeons.DialogueSkipTimer;
import gg.darkutils.feat.dungeons.DungeonTimer;
import gg.darkutils.feat.dungeons.ReplaceDiorite;
import gg.darkutils.feat.dungeons.SoloCrushTimer;
import gg.darkutils.feat.dungeons.SoloCrushWaypoint;
import gg.darkutils.feat.farming.EnforceZorrosCape;
import gg.darkutils.feat.farming.PestCooldownDisplay;
import gg.darkutils.feat.farming.StickyFarmingKeys;
import gg.darkutils.feat.foraging.TreeGiftConfirmation;
import gg.darkutils.feat.foraging.TreeGiftFeatures;
import gg.darkutils.feat.foraging.TreeGiftsPerHour;
import gg.darkutils.feat.mining.CorpsesPerShaftDisplay;
import gg.darkutils.feat.mining.MineshaftDisplay;
import gg.darkutils.feat.mining.MineshaftFeatures;
import gg.darkutils.feat.mining.WillOWispDisplay;
import gg.darkutils.feat.mining.LittlefootDisplay;
import gg.darkutils.feat.performance.LogCleaner;
import gg.darkutils.feat.performance.SoundLagFix;
import gg.darkutils.feat.performance.ThreadPriorityTweaker;
import gg.darkutils.feat.qol.AutoFishingRod;
import gg.darkutils.feat.qol.AutoTip;
import gg.darkutils.feat.qol.DisableCellsAlignment;
import gg.darkutils.feat.qol.GhostBlockKey;
import gg.darkutils.feat.qol.LaggyServerDetector;
import gg.darkutils.feat.qol.PreventUselessBlockHit;
import gg.darkutils.feat.qol.RejoinCooldownDisplay;
import gg.darkutils.feat.qol.ServerTPSCalculator;
import gg.darkutils.feat.qol.VanillaMode;
import gg.darkutils.update.UpdateChecker;
import gg.darkutils.utils.ActivityState;
import gg.darkutils.utils.LazyConstants;
import gg.darkutils.utils.LocationUtils;
import gg.darkutils.utils.LogLevel;
import gg.darkutils.utils.Pair;
import gg.darkutils.utils.TickUtils;
import gg.darkutils.utils.chat.ButtonData;
import gg.darkutils.utils.chat.ChatUtils;
import gg.darkutils.utils.chat.LinkData;
import gg.darkutils.utils.chat.SimpleFormatting;
import gg.darkutils.utils.chat.SimpleStyle;
import gg.darkutils.utils.chat.TextBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;

/*
import gg.darkutils.utils.chat.SimpleColor;

import java.util.stream.StreamSupport;

import java.util.WeakHashMap;
import java.util.Set;
import java.util.Collections;
import java.util.Objects;

import net.minecraft.client.world.ClientWorld;
*/

public final class DarkUtils implements ClientModInitializer {
    /**
     * Represents the modid. Used for the logger among other things.
     * Must and will be the same value specified in fabric.mod.json.
     * <p>
     * Can be used tu uniquely identify the mod from other mods.
     */
    public static final @NotNull String MOD_ID = "darkutils";

    /**
     * We must avoid loading some MC classes during test phase of Gradle build,
     * because they cause exceptions when loaded outside of runtime.
     * <p>
     * This system property is set to true from build.gradle when running tests.
     */
    public static final boolean INSIDE_JUNIT = "true".equals(System.getProperty("inside.junit")); // roughly same as Boolean#getBoolean but this is case-sensitive

    /**
     * This logger is used to write text to the console and the log file.
     * It is considered best practice to use your mod id as the logger's name.
     * That way, it's clear which mod wrote info, warnings, and errors.
     * <p>
     * This class has methods to use for logging so this is field is private.
     * The custom methods log errors in-game to the chat as well with a
     * user-friendly short string representation of the error.
     */
    private static final @NotNull Supplier<Logger> LOGGER = LazyConstants.lazyConstantOf(() -> LoggerFactory.getLogger(DarkUtils.MOD_ID));

    /**
     * Lazy-initialized non-changing constant-foldable (depending on LazyConstants
     * impl if the JEP is stabilized) value of the current Windowing platform.
     * <p>
     * This value will be used for some features like gui scale fix and auto
     * diagnostic features like disallowing setting cursor position in wayland.
     */
    private static final @NotNull Supplier<String> WINDOW_PLATFORM = LazyConstants.lazyConstantOf(Window::getPlatform);

    /**
     * Used for rate-limiting the update checker command so that user can't spam the GH API.
     * <p>
     * Not a definitive solution to server-side rate limit, just a safety against spamming.
     */
    private static final long ONE_MINUTE_NS = TimeUnit.MINUTES.toNanos(1L);
    /**
     * Color used for mod headers that take the full chat width.
     */
    @NotNull
    private static final String HEADER_FOOTER_COLOR = "#1e2124";
    /**
     * Color used for gradient start hex, which will transition to gradient end inside text slowly.
     */
    @NotNull
    private static final String GRADIENT_START = "#00C6FF";
    /**
     * Color used for gradient end hex, which will transition from gradient start inside text slowly.
     */
    @NotNull
    private static final String GRADIENT_END = "#0072FF";
    /**
     * Used for rate-limiting the update checker command so that user can't spam the GH API.
     * <p>
     * Not a definitive solution to server-side rate limit, just a safety against spamming.
     */
    private static long lastManualUpdateCheckTimeNs;

    public DarkUtils() {
        super();
    }

    public static final boolean isWindowPlatformWayland() {
        return "wayland".equals(DarkUtils.WINDOW_PLATFORM.get());
    }

    public static final void info(@NotNull final Class<?> source, @NotNull final String message) {
        DarkUtils.info(source, message, (Object[]) null);
    }

    public static final void info(@NotNull final Class<?> source, @NotNull final String message, @Nullable final Object @Nullable ... args) {
        DarkUtils.log(source, LogLevel.INFO, message, null, args);
    }

    public static final void warn(@NotNull final Class<?> source, @NotNull final String message) {
        DarkUtils.warn(source, message, (Object[]) null);
    }

    public static final void warn(@NotNull final Class<?> source, @NotNull final String message, @Nullable final Object @Nullable ... args) {
        DarkUtils.log(source, LogLevel.WARN, message, null, args);
    }

    public static final void error(@NotNull final Class<?> source, @NotNull final String message) {
        DarkUtils.error(source, message, null, (Object[]) null);
    }

    public static final void error(@NotNull final Class<?> source, @NotNull final String message, @Nullable final Object @Nullable ... args) {
        DarkUtils.error(source, message, null, args);
    }

    public static final void error(@NotNull final Class<?> source, @NotNull final String message, @Nullable final Throwable error) {
        DarkUtils.error(source, message, error, (Object[]) null);
    }

    public static final void info(@NotNull final String source, @NotNull final String message) {
        DarkUtils.info(source, message, (Object[]) null);
    }

    private static final void info(@NotNull final String source, @NotNull final String message, @Nullable final Object @Nullable ... args) {
        DarkUtils.log(source, LogLevel.INFO, message, null, args);
    }

    public static final void warn(@NotNull final String source, @NotNull final String message) {
        DarkUtils.warn(source, message, (Object[]) null);
    }

    private static final void warn(@NotNull final String source, @NotNull final String message, @Nullable final Object @Nullable ... args) {
        DarkUtils.log(source, LogLevel.WARN, message, null, args);
    }

    public static final void error(@NotNull final String source, @NotNull final String message) {
        DarkUtils.error(source, message, null, (Object[]) null);
    }

    public static final void error(@NotNull final String source, @NotNull final String message, @Nullable final Object @Nullable ... args) {
        DarkUtils.error(source, message, null, args);
    }

    public static final void error(@NotNull final String source, @NotNull final String message, @Nullable final Throwable error) {
        DarkUtils.error(source, message, error, (Object[]) null);
    }

    public static final void debug(@NotNull final Supplier<String> message) {
        if (DarkUtilsConfig.INSTANCE.debugMode) {
            DarkUtils.user(message.get(), DarkUtils.UserMessageLevel.USER_INFO);
        }
    }

    public static final void user(@NotNull final String message, @NotNull final DarkUtils.UserMessageLevel level) {
        DarkUtils.user(message, level, null);
    }

    private static final void user(@NotNull final String message, @NotNull final DarkUtils.UserMessageLevel level, @Nullable final LinkData link) {
        final var text = TextBuilder
                .empty();

        final var pendingAppend = List.of(
                Map.entry(DarkUtils.class.getSimpleName(), SimpleStyle.colored(ChatUtils.hexToRGB(DarkUtils.HEADER_FOOTER_COLOR))),
                Map.entry(" » ", SimpleStyle.colored(ChatUtils.hexToRGB("#7289da"))),
                Map.entry(message, SimpleStyle.colored(level.rgb))
        );

        pendingAppend.forEach(entry -> {
            if (null == link) {
                text.append(entry.getKey(), entry.getValue());
            } else {
                text.append(entry.getKey(), entry.getValue(), link);
            }
        });

        ChatUtils.sendMessageToLocalPlayer(text.build());
    }

    private static final void error(@NotNull final Class<?> source, @NotNull final String message, @Nullable final Throwable error, @Nullable final Object @Nullable ... args) {
        DarkUtils.log(source, LogLevel.ERROR, message, error, args);
    }

    private static final void error(@NotNull final String source, @NotNull final String message, @Nullable final Throwable error, @Nullable final Object @Nullable ... args) {
        DarkUtils.log(source, LogLevel.ERROR, message, error, args);
    }

    private static final void log(@NotNull final Class<?> source, @NotNull final LogLevel level, @NotNull final String message, @Nullable final Throwable error, @Nullable final Object @Nullable ... args) {
        DarkUtils.log(source.getSimpleName(), level, message, error, args);
    }

    private static final void log(@NotNull final String source, @NotNull final LogLevel level, @NotNull final String message, @Nullable final Throwable error, @Nullable final Object @Nullable ... args) {
        try {
            final var finalMessage = DarkUtils.addPrefixToLogEntry(source, message);
            var formattedMessage = null == args || 0 == args.length ? finalMessage : MessageFormatter.arrayFormat(finalMessage, args).getMessage();

            if (null == error) {
                DarkUtils.logMessage(level, formattedMessage);
            } else {
                if (LogLevel.ERROR != level) {
                    throw new IllegalArgumentException("tried to log an error at a log level of " + level.name());
                }

                // Append: error type, error reason (message), source file and line number to the message if provided.
                formattedMessage += ": " + DarkUtils.extractCodeDetails(error);

                DarkUtils.logError(formattedMessage, error);
            }

            DarkUtils.logInGame(level, formattedMessage);
        } catch (final Throwable e) {
            // Error when handling error; fallback to simple JDK printStackTrace as last resort
            e.printStackTrace();

            if (null != error) {
                error.printStackTrace();
            }
        }
    }

    private static final void logMessage(@NotNull final LogLevel level, @NotNull final String formattedMessage) {
        final var logger = DarkUtils.LOGGER.get();

        switch (level) {
            case INFO -> {
                if (logger.isInfoEnabled()) {
                    logger.info(formattedMessage);
                }
            }
            case WARN -> {
                if (logger.isWarnEnabled()) {
                    logger.warn(formattedMessage);
                }
            }
            case ERROR -> {
                if (logger.isErrorEnabled()) {
                    logger.error(formattedMessage);
                }
            }
        }
    }

    private static final void logError(@NotNull final String formattedMessage, @NotNull final Throwable error) {
        final var logger = DarkUtils.LOGGER.get();
        if (logger.isErrorEnabled()) {
            logger.error(formattedMessage, error);
        }
    }

    private static final void logInGame(@NotNull final LogLevel level, @NotNull final String message) {
        if (DarkUtils.INSIDE_JUNIT || level.ordinal() < DarkUtilsConfig.INSTANCE.ingameLogLevel.ordinal()) {
            // Too low of a verbosity to log in-game (user preference) -
            // by default only WARN and above are logged in-game.
            return;
        }

        // If logging before player joins a world/server/realm (e.g. in main menu),
        // we need to wait till player joins one so they can read chat.
        final var text = Component.literal(message);
        var style = Style.EMPTY;

        style = style.withColor(switch (level) {
            case INFO -> CommonColors.LIGHT_GRAY;
            case WARN -> CommonColors.SOFT_YELLOW;
            case ERROR -> CommonColors.SOFT_RED;
        });

        text.setStyle(style);

        ChatUtils.sendMessageToLocalPlayer(text);
    }

    @NotNull
    private static final String addPrefixToLogEntry(@NotNull final String source, @NotNull final String message) {
        return '[' + DarkUtils.class.getSimpleName() + "]: " + (DarkUtils.class.getSimpleName().equals(source) ? "" : '[' + source + "]: ") + message;
    }

    @NotNull
    private static final String extractCodeDetails(@NotNull final Throwable error) {
        final var type = error.getClass().getName();

        final var desc = error.getMessage();
        final var stack = error.getStackTrace();
        final var topOfStack = 0 < stack.length ? stack[0] : null;

        final var fileName = DarkUtils.getFileName(topOfStack);

        final var lineNumber = null == topOfStack ? -1 : topOfStack.getLineNumber();
        final var loc = null == fileName ? null : fileName + (0 <= lineNumber ? " line " + lineNumber : "");

        return type + (null == desc ? "" : ": " + desc) + (null == loc ? "" : " at " + loc);
    }

    private static final @Nullable String getFileName(@Nullable final StackTraceElement topOfStack) {
        @Nullable var fileName = null == topOfStack ? null : topOfStack.getFileName();

        if (null == fileName) {
            fileName = null == topOfStack ? null : topOfStack.getClassName();
            if (null != fileName) {
                // if no file information is available, assume .java
                // hopefully it's not kotlin, groovy or scala
                final var lastDot = fileName.lastIndexOf('.');
                return (0 <= lastDot ? fileName.substring(lastDot + 1) : fileName) + ".java";
            }
        }

        return fileName;
    }

    @NotNull
    private static final Throwable getRootError(@NotNull Throwable error, @NotNull final Predicate<Throwable> predicate) {
        for (Throwable parent; null != (parent = error.getCause()); ) {
            // We don't want the exceptions with no stack trace. Additionally, test for the predicate requested.
            if (0 == parent.getStackTrace().length || !predicate.test(parent)) {
                return error;
            }

            error = parent;
        }

        return error;
    }

    private static final void init(@NotNull final Runnable @NotNull ... initializers) {
        for (final var initializer : initializers) {
            try {
                initializer.run();
            } catch (final Throwable error) {
                DarkUtils.handleInitError(error);
            }
        }
    }

    private static final void handleInitError(@NotNull final Throwable error) {
        try {
            final var rootError = DarkUtils.getRootError(error, DarkUtils::isOurModuleFrame);

            final var ste = DarkUtils.findRelevantStackTrace(rootError.getStackTrace());
            final var featureName = DarkUtils.deriveFeatureName(rootError.getStackTrace());

            var details = "";
            final var msg = rootError.getMessage();
            if (null != msg && !msg.isEmpty()) {
                details = " Details: " + msg;
            }

            DarkUtils.error(
                    DarkUtils.class,
                    "Encountered " + rootError.getClass().getSimpleName()
                            + " at " + ste.getFileName()
                            + " in method " + ste.getMethodName()
                            + " (line " + ste.getLineNumber() + ')'
                            + " while initializing feature " + featureName + '.'
                            + details,
                    rootError
            );
        } catch (final Throwable e) {
            // Error when handling error; fallback to simple JDK printStackTrace as last resort
            e.printStackTrace();
            error.printStackTrace();
        }
    }

    private static final boolean isOurModuleFrame(@NotNull final Throwable err) {
        for (final var ste : err.getStackTrace()) {
            if (ste.getClassName().contains(DarkUtils.MOD_ID)) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    private static final StackTraceElement findRelevantStackTrace(final @NotNull StackTraceElement... stack) {
        final Predicate<String> isOurModule = cls -> cls.contains(DarkUtils.MOD_ID);

        for (final var ste : stack) {
            if (isOurModule.test(ste.getClassName()) && !ste.getClassName().contains("$$Lambda$") && 1 != ste.getLineNumber()) {
                return ste;
            }
        }

        // fallback to top-of-stack
        return 0 < stack.length ? stack[0] : new StackTraceElement("?", "?", "SourceFile.java", 0);
    }

    @NotNull
    private static final String deriveFeatureName(final @NotNull StackTraceElement @NotNull ... stack) {
        for (int i = 0, len = stack.length; i < len; ++i) {
            final var ste = stack[i];
            if (ste.getClassName().contains("DarkUtils") && "init".equals(ste.getMethodName()) && 0 < i) {
                final var below = stack[i - 1];
                return below.getClassName().substring(below.getClassName().lastIndexOf('.') + 1);
            }
            if (ste.getClassName().contains("feat")) {
                return ste.getClassName().substring(ste.getClassName().lastIndexOf('.') + 1);
            }
        }
        return "UnknownFeature";
    }

    /**
     * Due to <a href="https://github.com/Mojang/brigadier/issues/46">a brigadier issue</a> we can't use redirect and must use a workaround for each alias separately.
     * <p>
     * This version always returns {@link Command#SINGLE_SUCCESS} and accepts a Runnable instead of Command, so you don't have to accept context and can use method reference.
     * We don't really care about the command sender as the output would always go on the local player because the mod is client-environment only (no running commands from console or other players), and it does not make sense to run the commands from another source in single player like a command block.
     * <p>
     * The commands should catch their own exceptions and output messages on error conditions, so we always return success.
     */
    private static final void registerCommandWithAliases(@NotNull final String command, @NotNull final Runnable onExecute, final String... aliases) {
        DarkUtils.registerCommandWithAliases(command, ctx -> {
            onExecute.run();

            return Command.SINGLE_SUCCESS;
        }, aliases);
    }

    /**
     * Due to <a href="https://github.com/Mojang/brigadier/issues/46">a brigadier issue</a> we can't use redirect and must use a workaround for each alias separately.
     */
    private static final void registerCommandWithAliases(@NotNull final String command, @NotNull final Command<FabricClientCommandSource> onExecute, final String... aliases) {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            // canonical command
            final var canonical = DarkUtils.registerCommand(dispatcher, command, onExecute);

            // aliases
            for (final var alias : aliases) {
                dispatcher.getRoot().addChild(DarkUtils.buildRedirect(alias, canonical));
            }
        });
    }

    /**
     * Clone a command node to serve as an alias, based on Velocity’s workaround for Brigadier issue <a href="https://github.com/Mojang/brigadier/issues/46">#46</a>.
     */
    private static final @NotNull LiteralCommandNode<FabricClientCommandSource> buildRedirect(@NotNull final String alias, @NotNull final LiteralCommandNode<FabricClientCommandSource> destination) {
        final var builder = LiteralArgumentBuilder
                .<FabricClientCommandSource>literal(alias.toLowerCase(Locale.ROOT))
                .requires(destination.getRequirement())
                .forward(destination.getRedirect(), destination.getRedirectModifier(), destination.isFork())
                .executes(destination.getCommand());

        for (final var child : destination.getChildren()) {
            builder.then(child);
        }

        return builder.build();
    }

    private static final @NotNull LiteralCommandNode<FabricClientCommandSource> registerCommand(@NotNull final CommandDispatcher<FabricClientCommandSource> dispatcher, @NotNull final String command, @NotNull final Command<FabricClientCommandSource> onExecute) {
        return dispatcher.register(ClientCommandManager.literal(command)
                .executes(onExecute));
    }

    private static final void openConfig() {
        final var mc = Minecraft.getInstance();
        mc.schedule(() -> mc.setScreen(DarkUtilsConfigScreen.create(null)));
    }

    public static final @NotNull String getVersion() {
        final var container = FabricLoader.getInstance().getModContainer(DarkUtils.MOD_ID);
        if (container.isPresent()) {
            final var meta = container.get().getMetadata();
            return meta.getVersion().getFriendlyString(); // e.g. "1.3.0+1.21.10"
        }
        return "unknown";
    }

    @NotNull
    public static final Pair<String, String> getModAndMcVersion() {
        final var split = DarkUtils.getVersion().split("\\+");

        return new Pair<>(split[0], split[1]); // 0 = mod version, 1 = mc version
    }

    @NotNull
    private static final Pair<String, String> cutInHalf(@NotNull final String text) {
        final var mid = text.length() >> 1;

        return new Pair<>(text.substring(0, mid), text.substring(mid));
    }

    private static final void checkUpdates() {
        DarkUtils.checkUpdatesAndGreet(false, false); // fancy welcome message is special for join time, when called externally always default to one-liner feedback
    }

    private static final void checkUpdatesAndGreet() {
        DarkUtils.checkUpdatesAndGreet(!DarkUtilsConfig.INSTANCE.disableWelcomeMessage, DarkUtilsConfig.INSTANCE.disableUpdateChecker);
    }

    private static final void checkUpdatesAndGreet(final boolean fancyGreet, final boolean noCheck) {
        if (noCheck && fancyGreet) {
            // User choose to disable update checker but keep welcome message, treat as if they had latest version.
            DarkUtils.queueWelcomeMessageIfEnabled();
            return;
        }

        if (noCheck) {
            // Both the update checker and welcome message is disabled, don't do anything.
            return;
        }

        // User has update checker enabled and potentially the welcome message.
        UpdateChecker.checkUpdateAndRunCallbackOnRenderThread((result, release) -> DarkUtils.notifyUpdateCheckerResult(fancyGreet, result, release.orElse(null)));
    }

    private static final void notifyUpdateCheckerResult(final boolean fancyGreet, @NotNull final UpdateChecker.UpdateCheckerResult result, @Nullable final UpdateChecker.GitHubRelease release) {
        // 60 ticks = 3 seconds delay so that the message is sent after guild motd and other stuff for more visibility
        final DarkUtils.UserMessageMethod user = (message, level) -> TickUtils.queueTickTask(() -> DarkUtils.user(message, level), 60);
        final DarkUtils.UserMessageMethodWithLink userWithLink = (message, level, linkData) -> TickUtils.queueTickTask(() -> DarkUtils.user(message, level, linkData), 60);

        final var latestReleaseLink = null != release && null != release.html_url() ? new LinkData("Click to open latest release in browser", release.html_url()) : null;

        // If welcome message is enabled, we embed the update result into it, otherwise send a separate (simple) message.
        final Runnable feedback = switch (result) {
            case UP_TO_DATE_STABLE ->
                    fancyGreet ? () -> DarkUtils.queueWelcomeMessageIfEnabled("This the latest stable version.") : () -> user.accept("You are using the latest version of the mod.", DarkUtils.UserMessageLevel.USER_INFO);
            case UP_TO_DATE_PRE ->
                    fancyGreet ? () -> DarkUtils.queueWelcomeMessageIfEnabled("This the latest pre-release version.") : () -> user.accept("You are using the latest pre-release version of the mod.", DarkUtils.UserMessageLevel.USER_INFO);
            case OUT_OF_DATE ->
                    fancyGreet ? () -> DarkUtils.queueWelcomeMessageIfEnabled("!! This an outdated version !!", latestReleaseLink) : () -> userWithLink.accept("You are using an !! outdated !! version of the mod - please update!", DarkUtils.UserMessageLevel.USER_WARN, latestReleaseLink);
            case IN_DEVELOPMENT_VERSION ->
                    fancyGreet ? () -> DarkUtils.queueWelcomeMessageIfEnabled("In-development version - Update frequently!") : () -> user.accept("You are using an in-development version of the mod! Expect bugs and update frequently!", DarkUtils.UserMessageLevel.USER_WARN);
            case COULD_NOT_CHECK ->
                    fancyGreet ? () -> DarkUtils.queueWelcomeMessageIfEnabled("We couldn't check if this the latest version, please see if there is any errors above!") : () -> user.accept("Could not check for mod updates!", DarkUtils.UserMessageLevel.USER_ERROR);
        };

        feedback.run();
    }

    private static final void queueWelcomeMessageIfEnabled() {
        DarkUtils.queueWelcomeMessageIfEnabled(null);
    }

    private static final void queueWelcomeMessageIfEnabled(@Nullable final String extra) {
        DarkUtils.queueWelcomeMessageIfEnabled(extra, null);
    }

    private static final void queueWelcomeMessageIfEnabled(@Nullable final String extra, @Nullable final LinkData link) {
        if (DarkUtilsConfig.INSTANCE.disableWelcomeMessage) {
            return;
        }

        TickUtils.awaitLocalPlayer(player -> {
            final var headerFooterColor = ChatUtils.hexToRGB(DarkUtils.HEADER_FOOTER_COLOR);
            final var headerFooterStyle = SimpleStyle.colored(headerFooterColor).also(SimpleStyle.formatted(SimpleFormatting.BOLD));

            final var header = DarkUtils.cutInHalf(ChatUtils.fillRemainingOf('▬', true, ' ' + DarkUtils.class.getSimpleName() + ' ').replace(' ' + DarkUtils.class.getSimpleName() + ' ', ""));
            final var footer = ChatUtils.fill('▬', true);

            final var version = DarkUtils.getModAndMcVersion();

            final var text = TextBuilder
                    .withInitial(header.first(), headerFooterStyle)
                    .appendSpace()
                    .appendGradientText(DarkUtils.GRADIENT_START, DarkUtils.GRADIENT_END, DarkUtils.class.getSimpleName(), SimpleStyle.inherited())
                    .appendSpace()
                    .append(header.second(), headerFooterStyle)
                    .appendDoubleNewLine()
                    .appendGradientText(DarkUtils.GRADIENT_START, DarkUtils.GRADIENT_END, "Welcome to " + DarkUtils.class.getSimpleName() + " v" + version.first() + " for " + version.second() + '!', SimpleStyle
                            .centered()
                            .also(SimpleStyle.formatted(SimpleFormatting.BOLD))
                    );

            final var hasExtra = null != extra;

            if (!hasExtra && null != link) {
                throw new IllegalArgumentException("Can only have external link on external extra text");
            }

            if (hasExtra) {
                text.appendNewLine()
                        .appendGradientText(DarkUtils.GRADIENT_START, DarkUtils.GRADIENT_END, extra, SimpleStyle
                                        .centered()
                                        .also(SimpleStyle.formatted(SimpleFormatting.BOLD)),
                                link
                        );
            }

            text
                    .appendDoubleNewLine()
                    .appendGradientButton(DarkUtils.GRADIENT_START, DarkUtils.GRADIENT_END, new ButtonData("Open Settings", "Click to open mod settings!", '/' + DarkUtils.MOD_ID), SimpleStyle
                            .centered()
                            .also(SimpleStyle.formatted(SimpleFormatting.BOLD))
                    )
                    .appendDoubleNewLine()
                    .append(footer, headerFooterStyle);

            TickUtils.queueTickTask(() -> ChatUtils.sendMessageToLocalPlayer(text.build()), 60); // 3 seconds delay so that the message is sent after guild motd and other stuff for more visibility
        });
    }

    private static final void initEvents() {
        DarkUtils.init(
                ReceiveGameMessageEvent::init
        );
    }

    private static final void initUtils() {
        DarkUtils.init(
                LocationUtils::init,
                ChatUtils::init
        );
    }

    private static final void initFeatureDependencies() {
        DarkUtils.init(
                TreeGiftFeatures::init,
                DungeonTimer::init,
                ServerTPSCalculator::init,
                ActivityState::init,
                MineshaftFeatures::init
        );
    }

    private static final void initFeatures() {
        DarkUtils.init(
                AutoFishingRod::init,
                TreeGiftConfirmation::init,
                TreeGiftsPerHour::init,
                PestCooldownDisplay::init,
                StickyFarmingKeys::init,
                EnforceZorrosCape::init,
                CorpsesPerShaftDisplay::init,
                MineshaftDisplay::init,
                WillOWispDisplay::init,
                LittlefootDisplay::init,
                LogCleaner::init,
                AutoCloseSecretChests::init,
                DialogueSkipTimer::init,
                SoloCrushTimer::init,
                SoloCrushWaypoint::init,
                GhostBlockKey::init,
                ReplaceDiorite::init,
                AutoTip::init,
                AlignmentTaskSolver::init,
                DisableCellsAlignment::init,
                PreventUselessBlockHit::init,
                RejoinCooldownDisplay::init,
                LaggyServerDetector::init,
                SoundLagFix::init,
                ThreadPriorityTweaker::init,
                ArrowStackWaypoints::init,
                VanillaMode::init,
                CursorFix::init
        );
    }

    @NotNull
    private static final List<String> collectDebugInformation() {
        return List.of(
                "Is in Hypixel: " + LocationUtils.isInHypixel(),
                "Is in Skyblock: " + LocationUtils.isInSkyblock(),
                "Is in Singleplayer: " + LocationUtils.isInSingleplayer(),
                "Is in Galatea: " + LocationUtils.isInGalatea(),
                "Is in Dungeons: " + LocationUtils.isInDungeons(),
                "Dungeon floor: " + DungeonTimer.getDungeonFloor()
        );
    }

    private static final void debugState() {
        final var headerFooterColor = ChatUtils.hexToRGB(DarkUtils.HEADER_FOOTER_COLOR);
        final var headerFooterStyle = SimpleStyle.colored(headerFooterColor).also(SimpleStyle.formatted(SimpleFormatting.BOLD));

        final var header = DarkUtils.cutInHalf(ChatUtils.fillRemainingOf('▬', true, " Debug State ").replace(" Debug State ", ""));
        final var footer = ChatUtils.fill('▬', true);

        final var textBuilder = TextBuilder
                .withInitial(header.first(), headerFooterStyle)
                .appendSpace()
                .appendGradientText(DarkUtils.GRADIENT_START, DarkUtils.GRADIENT_END, "Debug State", SimpleStyle.inherited())
                .appendSpace()
                .append(header.second(), headerFooterStyle)
                .appendNewLine();

        for (final var line : DarkUtils.collectDebugInformation()) {
            textBuilder
                    .appendNewLine()
                    .appendGradientText(DarkUtils.GRADIENT_START, DarkUtils.GRADIENT_END, line, SimpleStyle.centered().also(SimpleStyle.formatted(SimpleFormatting.BOLD)))
            ;
        }

        textBuilder
                .appendDoubleNewLine()
                .append(footer, headerFooterStyle)
        ;

        final var text = textBuilder.build();

        ChatUtils.sendMessageToLocalPlayer(text);
    }

    private static final void runRateLimitedManualUpdateCheck() {
        final var now = System.nanoTime();
        final var last = DarkUtils.lastManualUpdateCheckTimeNs;

        if (0L != last && now - last < DarkUtils.ONE_MINUTE_NS) {
            DarkUtils.user("Please wait a minute before running the update check again.", DarkUtils.UserMessageLevel.USER_ERROR);
            return;
        }

        DarkUtils.lastManualUpdateCheckTimeNs = now;

        // Will still check even if disabled on config since user explicitly entered the command.
        DarkUtils.checkUpdates();
    }

    /**
     * This entrypoint is suitable for setting up client-specific logic, such as rendering.
     */
    @Override
    public final void onInitializeClient() {
        try {
            //TickUtils.queueRepeatingTickTask(DarkUtils::onTick, 1);

            // Register mod commands
            DarkUtils.registerCommandWithAliases(DarkUtils.MOD_ID, DarkUtils::openConfig, "darkutil", "du");
            DarkUtils.registerCommandWithAliases("darkutilsdebug", DarkUtils::debugState, "darkutildebug", "dudbg");
            DarkUtils.registerCommandWithAliases("darkutilscheckupdates", DarkUtils::runRateLimitedManualUpdateCheck, "darkutilscheckupdate", "duupdate");
            /*DarkUtils.registerCommandWithAliases("darkutilstestupdateoutput", () -> {
                // A dummy latest release that is newer than the current release for testing purposes.
                // Tag name and HTML url required for the link to appear, rest can be null and false.
                final var mockRelease = new UpdateChecker.GitHubRelease("v999.9.9", null, null, false, false, "https://github.com/TheDGOfficial/DarkUtils/releases/tag/v999.9.9", null, null);
                final boolean[] booleans = { true, false };
                for (final var fancy : booleans) {
                    for (final var res : UpdateChecker.UpdateCheckerResult.values()) {
                        DarkUtils.notifyUpdateCheckerResult(fancy, res, mockRelease);
                    }
                }
            });*/
            //DarkUtils.registerCommandWithAliases("dumpleaks", DarkUtils::dumpLeaks);

            // Init custom events that wrap fabric events, other things depend on them
            DarkUtils.initEvents();

            // Init utils, features may depend on those so they should be init before features
            DarkUtils.initUtils();

            // Init feature dependencies, features will depend on those so they should be init before features
            DarkUtils.initFeatureDependencies();

            // Init features
            DarkUtils.initFeatures();

            // Run update checker and greet user depending on version
            DarkUtils.checkUpdatesAndGreet();
        } catch (final Throwable error) {
            DarkUtils.error(DarkUtils.class, "Error during mod initialization", error);
        }
    }

    public enum UserMessageLevel {
        USER_INFO("#36393e"),
        USER_WARN("#F4E051"),
        USER_ERROR("#FF3434");

        private final int rgb;

        private UserMessageLevel(@NotNull final String hex) {
            this.rgb = ChatUtils.hexToRGB(hex);
        }
    }

    /*@NotNull
    private static final Set<ClientWorld> oldWorlds = Collections.newSetFromMap(new WeakHashMap<>(64));

    @Nullable
    private static ClientWorld previous;
    
    private static final void onTick() {
        final var curr = MinecraftClient.getInstance().world;
        final var prev = previous;

        if (null != prev && curr != prev) {
            // World changed
            oldWorlds.add(prev);
        }

        DarkUtils.previous = curr;
    }

    private static final void dumpLeaks() {
        final var headerFooterColor = ChatUtils.hexToRGB(DarkUtils.HEADER_FOOTER_COLOR);
        final var headerFooterStyle = SimpleStyle.colored(headerFooterColor).also(SimpleStyle.formatted(SimpleFormatting.BOLD));

        final var header = DarkUtils.cutInHalf(ChatUtils.fillRemainingOf('▬', true, " Leak Statistics ").replace(" Leak Statistics ", ""));
        final var footer = ChatUtils.fill('▬', true);

        final var text = TextBuilder
                .withInitial(header.first(), headerFooterStyle)
                .appendSpace()
                .appendGradientText(DarkUtils.GRADIENT_START, DarkUtils.GRADIENT_END, "Leak Statistics", SimpleStyle.inherited())
                .appendSpace()
                .append(header.second(), headerFooterStyle)
                .appendDoubleNewLine()
                .appendGradientText(DarkUtils.GRADIENT_START, DarkUtils.GRADIENT_END, "Worlds leaked or pending GC: " + StreamSupport.stream(oldWorlds.spliterator(), false)
                            .filter(Objects::nonNull)
                            .filter(world -> world != MinecraftClient.getInstance().world)
                            .count()
                        , SimpleStyle
                        .centered()
                        .also(SimpleStyle.formatted(SimpleFormatting.BOLD))
                )
                .appendDoubleNewLine()
                .append(footer, headerFooterStyle)
                .build();

        ChatUtils.sendMessageToLocalPlayer(text);
    }*/

    @FunctionalInterface
    private interface UserMessageMethod {
        void accept(@NotNull final String message, @NotNull final DarkUtils.UserMessageLevel level);
    }

    @FunctionalInterface
    private interface UserMessageMethodWithLink {
        void accept(@NotNull final String message, @NotNull final DarkUtils.UserMessageLevel level, @Nullable final LinkData link);
    }
}
