package gg.darkutils.utils;

import gg.darkutils.utils.chat.ChatUtils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public final class ScoreboardUtil {
    public static final ScoreboardUtil.@NonNull Continue<Void> CONTINUE = new ScoreboardUtil.Continue<>();
    public static final ScoreboardUtil.@NonNull Return<Void> BREAK = () -> null;

    private ScoreboardUtil() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final <T> ScoreboardUtil.Return<T> returning(@NotNull final T value) {
        return ScoreboardUtil.Return.of(value);
    }

    public static final <T> ScoreboardUtil.Continue<T> continuing() {
        return new ScoreboardUtil.Continue<>();
    }

    public static final void forEachScoreboardLine(@NotNull final ScoreboardUtil.ScoreboardLineConsumer<Void> action) {
        ScoreboardUtil.queryScoreboardLines(action, null);
    }

    public static final <T> @NotNull T forEachScoreboardLine(@NotNull final ScoreboardUtil.ScoreboardLineConsumer<T> action, @NotNull final T defaultValue) {
        final var result = ScoreboardUtil.queryScoreboardLines(action, defaultValue);
        return Objects.requireNonNullElse(result, defaultValue);
    }

    private static final <T> @Nullable T queryScoreboardLines(@NotNull final ScoreboardUtil.ScoreboardLineConsumer<T> action, @Nullable final T defaultValue) {
        final var mc = MinecraftClient.getInstance();

        if (null == mc.player || null == mc.world) {
            return defaultValue;
        }

        final var scoreboard = mc.player.networkHandler.getScoreboard();
        final var objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);

        if (null == objective) {
            return defaultValue;
        }

        for (final var scoreHolder : scoreboard.getKnownScoreHolders()) {
            final var team = scoreboard.getScoreHolderTeam(scoreHolder.getNameForScoreboard());

            if (null == team) {
                continue;
            }

            final var line = ChatUtils.removeControlCodes(team.getPrefix().getString() + team.getSuffix().getString());
            final var result = action.accept(line);

            switch (result) {
                case final ScoreboardUtil.Return<T> returning -> {
                    return returning.getReturnValue();
                }

                case final ScoreboardUtil.Continue<?> continuing -> {
                    // implicit continue
                }
            }
        }

        return defaultValue;
    }

    public sealed interface IterationDecision<T> permits ScoreboardUtil.Continue, ScoreboardUtil.Return {
    }

    @FunctionalInterface
    public non-sealed interface Return<T> extends ScoreboardUtil.IterationDecision<T> {
        static <T> ScoreboardUtil.Return<T> of(@NotNull final T value) {
            return () -> value;
        }

        @Nullable T getReturnValue();
    }

    @FunctionalInterface
    public interface ScoreboardLineConsumer<T> {
        @NotNull ScoreboardUtil.IterationDecision<T> accept(final @NotNull String line);
    }

    public static final record Continue<T>() implements ScoreboardUtil.IterationDecision<T> {
    }
}

