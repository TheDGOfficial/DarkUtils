package gg.darkutils.utils;

import gg.darkutils.utils.chat.ChatUtils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;

import org.jetbrains.annotations.NotNull;

public final class ScoreboardUtil {
    private ScoreboardUtil() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final ScoreboardUtil.Continue<Void> CONTINUE = new Continue<Void>();
    public static final ScoreboardUtil.Return<Void> BREAK = () -> null;

    public sealed interface IterationDecision<T> permits Continue, Return {
    }

    public static final record Continue<T>() implements IterationDecision<T> {
    }

    @FunctionalInterface
    public non-sealed interface Return<T> extends IterationDecision<T> {
        T getReturnValue();

        static <T> Return<T> of(@NotNull final T value) {
            return () -> value;
        }
    }

    public static final <T> ScoreboardUtil.Return<T> returning(@NotNull final T value) {
        return ScoreboardUtil.Return.of(value);
    }

    public static final <T> ScoreboardUtil.Continue<T> continuing() {
        return new ScoreboardUtil.Continue<>();
    }

    @FunctionalInterface
    public interface ScoreboardLineConsumer<T> {
        @NotNull ScoreboardUtil.IterationDecision<T> accept(final @NotNull String line);
    }

    public static final void forEachScoreboardLine(@NotNull final ScoreboardUtil.ScoreboardLineConsumer<Void> action) {
        ScoreboardUtil.forEachScoreboardLine(action, null);
    }

    public static final <T> T forEachScoreboardLine(@NotNull final ScoreboardUtil.ScoreboardLineConsumer<T> action, @NotNull final T defaultValue) {
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
                case Return<T> r -> {
                    return r.getReturnValue();
                }

                case Continue<?> c -> {
                    // implicit continue
                }
            }
        }

        return defaultValue;
    }
}

