package gg.darkutils.utils;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.feat.qol.BackpackPreview;
import gg.darkutils.utils.chat.ChatUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public final class SkyblockProfileUtils {
    private static final String COMMAND = "profileid";
    private static final Pattern PROFILE_ID = Pattern.compile("Profile ID: ([0-9a-f]{8}-?[0-9a-f]{4}-?[0-9a-f]{4}-?[0-9a-f]{4}-?[0-9a-f]{12})", Pattern.CASE_INSENSITIVE);
    private static final Pattern PROFILE_ANNOUNCEMENT = Pattern.compile("You are playing on profile: ([a-zA-Z]+).*");
    private static final Pattern TAB_PROFILE = Pattern.compile("Profile: ([a-zA-Z]+).*");
    private static final String SUGGEST_DASHES = "CLICK THIS TO SUGGEST IT IN CHAT [DASHES]";
    private static final String SUGGEST_NO_DASHES = "CLICK THIS TO SUGGEST IT IN CHAT [NO DASHES]";
    private static final ProfileReply reply = new ProfileReply();

    private static @Nullable String serverName;
    private static @Nullable String announcedName;
    private static @Nullable String profileName;
    private static @Nullable String profileId;
    private static boolean verifyProfile;
    private static boolean lookupScheduled;
    private static int locationRevision;

    private SkyblockProfileUtils() {
        throw new UnsupportedOperationException("static utility class");
    }

    public static final void onLocationUpdate(@Nullable final String server) {
        if (server == null) {
            reset();
        } else if (!Objects.equals(serverName, server)) {
            serverName = server;
            announcedName = null;
            verifyProfile = true;
            reply.disarm();
            scheduleLookup();
        }
    }

    public static final void reset() {
        serverName = null;
        announcedName = null;
        profileName = null;
        profileId = null;
        verifyProfile = false;
        lookupScheduled = false;
        ++locationRevision;
        reply.reset();
        BackpackPreview.reset();
    }

    private static final void scheduleLookup() {
        lookupScheduled = true;
        final var revision = ++locationRevision;
        TickUtils.queueTickTask(() -> {
            if (DarkUtilsConfig.INSTANCE.backpackPreview && revision == locationRevision && LocationUtils.isInSkyblock()) {
                lookupScheduled = false;
                final var connection = Minecraft.getInstance().getConnection();
                if (connection != null) {
                    refresh(connection);
                }
            }
        }, 30);
    }

    public static final void refresh(@NotNull final ClientPacketListener connection) {
        if (observeName(currentName(connection))) {
            reply.disarm();
        }
        if ((profileId == null || verifyProfile) && !lookupScheduled) {
            final var packet = reply.begin(Util.getMillis());
            if (packet != null) {
                connection.send(packet);
            }
        }
    }

    public static final boolean isResolving() {
        return profileId == null && (lookupScheduled || reply.isPending(Util.getMillis()));
    }

    private static final @Nullable String currentName(@NotNull final ClientPacketListener connection) {
        if (announcedName != null) {
            return announcedName;
        }
        for (final var player : connection.getOnlinePlayers()) {
            final var line = player.getTabListDisplayName();
            if (line != null) {
                final var name = TAB_PROFILE.matcher(ChatUtils.removeControlCodes(line.getString()).strip());
                if (name.matches()) {
                    return name.group(1);
                }
            }
        }
        return null;
    }

    private static final boolean observeName(@Nullable final String name) {
        if (name == null) {
            return false;
        }
        final var changed = profileName != null && !profileName.equalsIgnoreCase(name);
        profileName = name;
        verifyProfile = false;
        if (changed) {
            profileId = null;
            BackpackPreview.invalidateProfile();
        }
        return changed;
    }

    public static final boolean onSystemChat(@NotNull final Component message, @NotNull final ClientPacketListener connection) {
        final var text = ChatUtils.removeControlCodes(message.getString()).strip();
        final var match = PROFILE_ID.matcher(text);
        if (match.matches()) {
            // Learn IDs from manual and unsolicited replies as well. Only the chat decision depends on ownership.
            final var id = match.group(1).replace("-", "").toLowerCase(Locale.ROOT);
            final var hide = reply.hideLine(1, Util.getMillis());
            observeName(currentName(connection));
            verifyProfile = false;
            if (!id.equals(profileId)) {
                profileId = id;
                BackpackPreview.loadProfile(id, connection);
            }
            return hide;
        }
        final var announcement = PROFILE_ANNOUNCEMENT.matcher(text);
        if (announcement.matches()) {
            announcedName = announcement.group(1);
            if (observeName(announcedName)) {
                reply.disarm();
                scheduleLookup();
            }
        }
        return switch (text) {
            case SUGGEST_DASHES -> reply.hideLine(2, Util.getMillis());
            case SUGGEST_NO_DASHES -> reply.hideLine(3, Util.getMillis());
            default -> false;
        };
    }

    public static final void onOutgoingCommand(@NotNull final Connection transport, @NotNull final Packet<?> packet,
                                               @NotNull final String command) {
        if (COMMAND.equalsIgnoreCase(command.strip())) {
            final var connection = Minecraft.getInstance().getConnection();
            if (connection != null && connection.getConnection() == transport) {
                reply.onCommand(packet, Util.getMillis());
            }
        }
    }

    private static final class ProfileReply {
        private static final long TIMEOUT_MS = 5000L;
        private long deadline = Long.MIN_VALUE;
        private int outstanding;
        private int expectedLine = 1;
        private boolean suppress;
        private @Nullable Packet<?> ownPacket;

        private synchronized final @Nullable ServerboundChatCommandPacket begin(final long now) {
            if (now < this.deadline) {
                return null;
            }
            final var packet = new ServerboundChatCommandPacket(COMMAND);
            this.ownPacket = packet;
            this.deadline = now + TIMEOUT_MS;
            return packet;
        }

        private synchronized final void onCommand(@NotNull final Packet<?> packet, final long now) {
            // There is no reply ID in the protocol. Leave overlapping replies visible.
            this.suppress = this.outstanding == 0 && packet == this.ownPacket;
            ++this.outstanding;
            this.ownPacket = null;
            this.deadline = now + TIMEOUT_MS;
        }

        private synchronized final boolean hideLine(final int line, final long now) {
            if (this.outstanding == 0) {
                return false;
            }
            if (line != this.expectedLine) {
                this.suppress = false;
                this.expectedLine = line == 1 ? 2 : 1;
                return false;
            }
            final var hide = this.suppress && now < this.deadline;
            if (line == 3) {
                --this.outstanding;
                this.expectedLine = 1;
                this.suppress = false;
                if (this.outstanding == 0) {
                    this.deadline = Long.MIN_VALUE;
                }
            } else {
                this.expectedLine = line + 1;
            }
            return hide;
        }

        private synchronized final boolean isPending(final long now) {
            return now < this.deadline;
        }

        private synchronized final void disarm() {
            this.ownPacket = null;
            this.suppress = false;
        }

        private synchronized final void reset() {
            this.ownPacket = null;
            this.deadline = Long.MIN_VALUE;
            this.outstanding = 0;
            this.expectedLine = 1;
            this.suppress = false;
        }
    }
}
