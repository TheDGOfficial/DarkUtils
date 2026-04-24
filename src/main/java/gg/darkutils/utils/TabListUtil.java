package gg.darkutils.utils;

import gg.darkutils.utils.chat.ChatUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

public final class TabListUtil {
    private TabListUtil() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final @NotNull Iterator<String> tabListLinesIterator() {
        final var mc = Minecraft.getInstance();

        return null == mc.player || null == mc.level ? Collections.emptyIterator() : new TabListUtil.TabListIterator(mc.gui.getTabList());
    }

    public static final @NotNull Iterable<String> tabListLines() {
        return TabListUtil::tabListLinesIterator;
    }

    private static final class TabListIterator implements Iterator<String> {
        private final @NotNull Iterator<PlayerInfo> trackedPlayers;
        private @Nullable String nextLine;

        private TabListIterator(final @NotNull PlayerTabOverlay tabList) {
            super();

            this.trackedPlayers = tabList.getPlayerInfos().iterator();
        }

        @Override
        public final boolean hasNext() {
            if (null != this.nextLine) {
                return true;
            }

            final var players = this.trackedPlayers;

            while (players.hasNext()) {
                final var entry = players.next();

                if (null == entry) {
                    continue;
                }

                final var name = entry.getTabListDisplayName();

                if (null == name) {
                    continue;
                }

                this.nextLine = ChatUtils.removeControlCodes(name.getString());
                return true;
            }

            return false;
        }

        @Override
        public final @NotNull String next() {
            if (!this.hasNext()) {
                throw new NoSuchElementException("tab list next line");
            }

            final var result = this.nextLine;
            Objects.requireNonNull(result, "tab list next line result");

            this.nextLine = null;

            return result;
        }

        @Override
        public final String toString() {
            return "TabListIterator{" +
                    "trackedPlayers=" + this.trackedPlayers +
                    ", nextLine='" + this.nextLine + '\'' +
                    '}';
        }
    }
}

