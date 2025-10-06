package gg.darkutils;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.config.DarkUtilsConfigScreen;
import gg.darkutils.feat.dungeons.*;
import gg.darkutils.feat.foraging.TreeGiftConfirmation;
import gg.darkutils.feat.foraging.TreeGiftFeatures;
import gg.darkutils.feat.foraging.TreeGiftsPerHour;
import gg.darkutils.feat.performance.ArmorStandOptimizer;
import gg.darkutils.feat.performance.LogCleaner;
import gg.darkutils.feat.qol.AutoFishingRod;
import gg.darkutils.feat.qol.AutoTip;
import gg.darkutils.feat.qol.GhostBlockKey;
import gg.darkutils.utils.*;
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
        final var formattedMessage = null == args || 0 == args.length ? finalMessage : MessageFormatter.arrayFormat(finalMessage, args).getMessage();

        if (null == error) {
            final Pair<BooleanSupplier, Consumer<String>> loggingFunction = switch (level) {
                case INFO -> new Pair<>(DarkUtils.LOGGER::isInfoEnabled, DarkUtils.LOGGER::info);
                case WARN -> new Pair<>(DarkUtils.LOGGER::isWarnEnabled, DarkUtils.LOGGER::warn);
                case ERROR -> new Pair<>(DarkUtils.LOGGER::isErrorEnabled, DarkUtils.LOGGER::error);
            };

            if (loggingFunction.first().getAsBoolean()) {
                loggingFunction.second().accept(formattedMessage);
            }
        } else {
            if (LogLevel.ERROR != level) {
                throw new IllegalArgumentException("tried to log an error at a log level of " + level.name());
            }
            DarkUtils.LOGGER.error(formattedMessage, error);
        }

        DarkUtils.logInGame(level, formattedMessage);
    }

    private static final void logInGame(@NotNull final LogLevel level, @NotNull final String message) {
        if (level.ordinal() < DarkUtilsConfig.INSTANCE.ingameLogLevel.ordinal()) {
            // Too low of a verbosity to log in-game (user preference) -
            // by default only WARN and above are logged in-game.
            return;
        }

        // If logging before player joins a world/server/realm (e.g. in main menu),
        // we need to wait till player joins one so they can read chat.
        TickUtils.awaitLocalPlayer(player -> {
            final var text = Text.literal(message);
            var style = Style.EMPTY;

            style = style.withColor(switch (level) {
                case INFO -> Colors.LIGHT_GRAY;
                case WARN -> Colors.LIGHT_YELLOW;
                case ERROR -> Colors.LIGHT_RED;
            });

            text.setStyle(style);

            player.sendMessage(text, false);
        });
    }

    @NotNull
    private static final String addPrefixToLogEntry(@NotNull final Class<?> source, @NotNull final String message) {
        return '[' + DarkUtils.class.getSimpleName() + "]: " + (DarkUtils.class == source ? "" : '[' + source.getSimpleName() + "]: ") + message;
    }

    private static final void init(@NotNull final Runnable @NotNull ... initializers) {
        for (final var initializer : initializers) {
            try {
                initializer.run();
            } catch (final Throwable error) {
                DarkUtils.error(DarkUtils.class, "Error initializing feature", error);
            }
        }
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
        TickUtils.awaitLocalPlayer(player -> {
            if (!DarkUtilsConfig.INSTANCE.welcomeMessage) {
                return;
            }

            final var headerFooterColor = ChatUtils.hexToRGB("#4ffd7c");

            final var header = DarkUtils.cutInHalf(ChatUtils.fillRemainingOf('▬', true, ' ' + DarkUtils.class.getSimpleName() + ' ').replace(' ' + DarkUtils.class.getSimpleName() + ' ', ""));
            final var footer = ChatUtils.fill('▬', true);

            final var text = Text
                    .literal(header.first())
                    .setStyle(Style.EMPTY.withColor(headerFooterColor).withBold(true))
                    .append(" ")
                    .append(ChatUtils.gradient("#54daf4", "#545eb6", DarkUtils.class.getSimpleName()))
                    .append(" ")
                    .append(Text.literal(header.second()).setStyle(Style.EMPTY.withColor(headerFooterColor).withBold(true)))
                    .append("\n")
                    .append("\n")
                    .append(ChatUtils.gradient("#54daf4", "#545eb6", ChatUtils.center("Welcome to " + DarkUtils.class.getSimpleName() + " v" + DarkUtils.getVersion() + '!', true)))
                    .append("\n")
                    .append("\n")
                    .append(ChatUtils.button("#54daf4", "#545eb6", "Open Settings", "Click to open mod settings!", '/' + DarkUtils.MOD_ID, true, true))
                    .append("\n")
                    .append("\n")
                    .append(Text.literal(footer).setStyle(Style.EMPTY.withColor(headerFooterColor).withBold(true)));

            player.sendMessage(text, false);
        });
    }

    /**
     * This entrypoint is suitable for setting up client-specific logic, such as rendering.
     */
    @Override
    public final void onInitializeClient() {
        // Register mod commands
        DarkUtils.registerCommandWithAliases(DarkUtils.MOD_ID, ctx -> DarkUtils.openConfig(), "darkutil", "du");

        // Init utils, features may depend on those so they should be init before features
        DarkUtils.init(
                LocationUtils::init,
                ChatUtils::init
        );

        // Init feature dependencies, features will depend on those so they should be init before features
        DarkUtils.init(
                TreeGiftFeatures::init,
                DungeonTimer::init
        );

        // Init features
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
                AlignmentTaskSolver::init
        );

        // Send welcome message once player joins a world/server/realm
        DarkUtils.queueWelcomeMessageIfEnabled();
    }
}
