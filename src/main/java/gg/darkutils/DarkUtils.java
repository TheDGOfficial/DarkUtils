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
import gg.darkutils.feat.foraging.TreeGiftConfirmation;
import gg.darkutils.feat.foraging.TreeGiftFeatures;
import gg.darkutils.feat.foraging.TreeGiftsPerHour;
import gg.darkutils.feat.performance.ArmorStandOptimizer;
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
import gg.darkutils.utils.LocationUtils;
import gg.darkutils.utils.LogLevel;
import gg.darkutils.utils.Pair;
import gg.darkutils.utils.chat.ButtonData;
import gg.darkutils.utils.chat.ChatUtils;
import gg.darkutils.utils.chat.SimpleFormatting;
import gg.darkutils.utils.chat.SimpleStyle;
import gg.darkutils.utils.chat.TextBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.concurrent.CancellationException;

public final class DarkUtils implements ClientModInitializer {
    public static final @NotNull String MOD_ID = "darkutils";

    /**
     * This logger is used to write text to the console and the log file.
     * It is considered best practice to use your mod id as the logger's name.
     * That way, it's clear which mod wrote info, warnings, and errors.
     */
    private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(DarkUtils.MOD_ID);

    public DarkUtils() {
        super();
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

    private static final void error(@NotNull final Class<?> source, @NotNull final String message, @Nullable final Throwable error, @Nullable final Object @Nullable ... args) {
        DarkUtils.log(source, LogLevel.ERROR, message, error, args);
    }

    private static final void log(@NotNull final Class<?> source, @NotNull final LogLevel level, @NotNull final String message, @Nullable final Throwable error, @Nullable final Object @Nullable ... args) {
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
    }

    private static final void logMessage(@NotNull final LogLevel level, @NotNull final String formattedMessage) {
        final Pair<BooleanSupplier, Consumer<String>> loggingFunction = switch (level) {
            case INFO -> new Pair<>(DarkUtils.LOGGER::isInfoEnabled, DarkUtils.LOGGER::info);
            case WARN -> new Pair<>(DarkUtils.LOGGER::isWarnEnabled, DarkUtils.LOGGER::warn);
            case ERROR -> new Pair<>(DarkUtils.LOGGER::isErrorEnabled, DarkUtils.LOGGER::error);
        };

        if (loggingFunction.first().getAsBoolean()) {
            loggingFunction.second().accept(formattedMessage);
        }
    }

    private static final void logError(@NotNull final String formattedMessage, @NotNull final Throwable error) {
        if (DarkUtils.LOGGER.isErrorEnabled()) {
            DarkUtils.LOGGER.error(formattedMessage, error);
        }
    }

    private static final void logInGame(@NotNull final LogLevel level, @NotNull final String message) {
        if (level.ordinal() < DarkUtilsConfig.INSTANCE.ingameLogLevel.ordinal()) {
            // Too low of a verbosity to log in-game (user preference) -
            // by default only WARN and above are logged in-game.
            return;
        }

        // If logging before player joins a world/server/realm (e.g. in main menu),
        // we need to wait till player joins one so they can read chat.
        final var text = Text.literal(message);
        var style = Style.EMPTY;

        style = style.withColor(switch (level) {
            case INFO -> Colors.LIGHT_GRAY;
            case WARN -> Colors.LIGHT_YELLOW;
            case ERROR -> Colors.LIGHT_RED;
        });

        text.setStyle(style);

        ChatUtils.sendMessageToLocalPlayer(text);
    }

    @NotNull
    private static final String addPrefixToLogEntry(@NotNull final Class<?> source, @NotNull final String message) {
        return '[' + DarkUtils.class.getSimpleName() + "]: " + (DarkUtils.class == source ? "" : '[' + source.getSimpleName() + "]: ") + message;
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
        Throwable parent;

        while (null != (parent = error.getCause())) {
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

    private static final int openConfig() {
        final var mc = MinecraftClient.getInstance();
        mc.send(() -> mc.setScreen(DarkUtilsConfigScreen.create(null)));
        return Command.SINGLE_SUCCESS;
    }

    public static final @NotNull String getVersion() {
        final var container = FabricLoader.getInstance().getModContainer(DarkUtils.MOD_ID);
        if (container.isPresent()) {
            final var meta = container.get().getMetadata();
            return meta.getVersion().getFriendlyString(); // e.g. "1.2.3"
        }
        return "unknown";
    }

    @NotNull
    private static final Pair<String, String> cutInHalf(@NotNull final String text) {
        final var mid = text.length() >> 1;

        return new Pair<>(text.substring(0, mid), text.substring(mid));
    }

    private static final void queueWelcomeMessageIfEnabled() {
        if (!DarkUtilsConfig.INSTANCE.welcomeMessage) {
            return;
        }

        final var headerFooterColor = ChatUtils.hexToRGB("#4ffd7c");
        final var headerFooterStyle = SimpleStyle.colored(headerFooterColor).also(SimpleStyle.formatted(SimpleFormatting.BOLD));

        final var header = DarkUtils.cutInHalf(ChatUtils.fillRemainingOf('▬', true, ' ' + DarkUtils.class.getSimpleName() + ' ').replace(' ' + DarkUtils.class.getSimpleName() + ' ', ""));
        final var footer = ChatUtils.fill('▬', true);

        final var gradientStart = "#54daf4";
        final var gradientEnd = "#545eb6";

        final var text = TextBuilder
                .withInitial(header.first(), headerFooterStyle)
                .appendSpace()
                .appendGradientText(gradientStart, gradientEnd, DarkUtils.class.getSimpleName(), SimpleStyle.inherited())
                .appendSpace()
                .append(header.second(), headerFooterStyle)
                .appendNewLine()
                .appendNewLine()
                .appendGradientText(gradientStart, gradientEnd, "Welcome to " + DarkUtils.class.getSimpleName() + " v" + DarkUtils.getVersion() + '!', SimpleStyle
                        .centered()
                        .also(SimpleStyle.formatted(SimpleFormatting.BOLD))
                )
                .appendNewLine()
                .appendNewLine()
                .appendGradientButton(gradientStart, gradientEnd, new ButtonData("Open Settings", "Click to open mod settings!", '/' + DarkUtils.MOD_ID), SimpleStyle
                        .centered()
                        .also(SimpleStyle.formatted(SimpleFormatting.BOLD))
                )
                .appendNewLine()
                .appendNewLine()
                .append(footer, headerFooterStyle)
                .build();

        ChatUtils.sendMessageToLocalPlayer(text);
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
                ServerTPSCalculator::init
        );
    }

    private static final void initFeatures() {
        DarkUtils.init(
                ArmorStandOptimizer::init,
                AutoFishingRod::init,
                TreeGiftConfirmation::init,
                TreeGiftsPerHour::init,
                LogCleaner::init,
                AutoCloseSecretChests::init,
                DialogueSkipTimer::init,
                SoloCrushTimer::init,
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

    /**
     * This entrypoint is suitable for setting up client-specific logic, such as rendering.
     */
    @Override
    public final void onInitializeClient() {
        // Register mod commands
        DarkUtils.registerCommandWithAliases(DarkUtils.MOD_ID, ctx -> DarkUtils.openConfig(), "darkutil", "du");

        // Init custom events that wrap fabric events, other things depend on them
        DarkUtils.initEvents();

        // Init utils, features may depend on those so they should be init before features
        DarkUtils.initUtils();

        // Init feature dependencies, features will depend on those so they should be init before features
        DarkUtils.initFeatureDependencies();

        // Init features
        DarkUtils.initFeatures();

        // Send welcome message once player joins a world/server/realm
        DarkUtils.queueWelcomeMessageIfEnabled();
    }
}
