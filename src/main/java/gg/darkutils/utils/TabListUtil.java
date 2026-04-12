package gg.darkutils.utils;

import gg.darkutils.utils.chat.ChatUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.gui.components.PlayerTabOverlay;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

public final class TabListUtil {
    private TabListUtil() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final @NotNull Iterator<String> tabListLinesIterator() {
        final var mc = Minecraft.getInstance();

        if (null == mc.player || null == mc.level) {
            return Collections.emptyIterator();
        }

        final var tabList = mc.gui.getTabList();

        if (null == tabList) {
            return Collections.emptyIterator();
        }

        return new TabListUtil.TabListIterator(tabList);
    }

    public static final @NotNull Iterable<String> tabListLines() {
        return () -> TabListUtil.tabListLinesIterator();
    }

    private static final class TabListIterator implements Iterator<String> {
        private final @NotNull Iterator<PlayerInfo> trackedPlayers;
        private final @NotNull PlayerTabOverlay tabList;

        private @Nullable String nextLine;

        private TabListIterator(final @NotNull PlayerTabOverlay tabList) {
            this.tabList = tabList;
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
                throw new NoSuchElementException();
            }

            final var result = this.nextLine;
            this.nextLine = null;

            return result;
        }
    }
}

