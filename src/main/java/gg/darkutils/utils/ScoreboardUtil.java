package gg.darkutils.utils;

import gg.darkutils.utils.chat.ChatUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

public final class ScoreboardUtil {
    private ScoreboardUtil() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final @NotNull Iterable<String> scoreboardLines() {
        final var mc = Minecraft.getInstance();
        final var player = mc.player;

        if (null == player || null == mc.level) {
            return Collections::emptyIterator;
        }

        final var scoreboard = player.connection.scoreboard();
        final var objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);

        return null == objective ? Collections::emptyIterator : () -> new ScoreboardUtil.ScoreboardIterator(scoreboard);
    }

    private static final class ScoreboardIterator implements Iterator<String> {
        private final @NotNull Iterator<ScoreHolder> trackedPlayers;
        private final @NotNull Scoreboard scoreboard;

        private @Nullable String nextLine;

        private ScoreboardIterator(final @NotNull Scoreboard scoreboard) {
            super();

            this.scoreboard = scoreboard;
            this.trackedPlayers = scoreboard.getTrackedPlayers().iterator();
        }

        @Override
        public final boolean hasNext() {
            if (null != this.nextLine) {
                return true;
            }

            final var scoreboard = this.scoreboard;
            final var players = this.trackedPlayers;

            while (players.hasNext()) {
                final var scoreHolder = players.next();
                final var team = scoreboard.getPlayersTeam(scoreHolder.getScoreboardName());

                if (null == team) {
                    continue;
                }

                this.nextLine = ChatUtils.removeControlCodes(team.getPlayerPrefix().getString() + team.getPlayerSuffix().getString());
                return true;
            }

            return false;
        }

        @Override
        public final @NotNull String next() {
            if (!this.hasNext()) {
                throw new NoSuchElementException("scoreboard next line");
            }

            final var result = this.nextLine;
            Objects.requireNonNull(result, "scoreboard next line result");

            this.nextLine = null;

            return result;
        }

        @Override
        public final String toString() {
            return "ScoreboardIterator{" +
                    "trackedPlayers=" + this.trackedPlayers +
                    ", scoreboardObjectiveNames={" + String.join(", ", this.scoreboard.getObjectiveNames()) + '}' +
                    ", nextLine='" + this.nextLine + '\'' +
                    '}';
        }
    }
}
