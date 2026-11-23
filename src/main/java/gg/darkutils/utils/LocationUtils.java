package gg.darkutils.utils;

import gg.darkutils.config.DarkUtilsConfig;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.hypixel.data.type.GameType;
import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class LocationUtils {
    private static boolean isInHypixel;
    private static boolean isInSkyblock;
    @Nullable
    private static LocationUtils.SkyblockIsland skyblockIsland;

    private LocationUtils() {
        super();

        throw new UnsupportedOperationException("static utility class");
    }

    public static final void init() {
        HypixelModAPI.getInstance().subscribeToEventPacket(ClientboundLocationPacket.class);

        HypixelModAPI.getInstance().createHandler(ClientboundLocationPacket.class, LocationUtils::onLocationUpdate);
        ClientPlayConnectionEvents.DISCONNECT.register(LocationUtils::onQuit);
    }

    private static final void onLocationUpdate(@NotNull final ClientboundLocationPacket packet) {
        final var serverName = packet.getServerName();
        LocationUtils.isInHypixel = null != serverName;
        final var inSkyblock = GameType.SKYBLOCK == packet.getServerType().orElse(null);
        LocationUtils.isInSkyblock = inSkyblock;
        LocationUtils.skyblockIsland = LocationUtils.SkyblockIsland.fromId(packet.getMode().orElse(null));
        if (DarkUtilsConfig.INSTANCE.backpackPreview) {
            SkyblockProfileUtils.onLocationUpdate(inSkyblock ? serverName : null);
        }
    }

    private static final void onQuit(@NotNull final ClientPacketListener handler, @NotNull final Minecraft client) {
        LocationUtils.isInHypixel = false;
        LocationUtils.isInSkyblock = false;
        LocationUtils.skyblockIsland = null;
        if (DarkUtilsConfig.INSTANCE.backpackPreview) {
            SkyblockProfileUtils.reset();
        }
    }

    public static final boolean isInDungeons() {
        return LocationUtils.SkyblockIsland.DUNGEONS == LocationUtils.skyblockIsland;
    }

    public static final boolean isInGalatea() {
        return LocationUtils.SkyblockIsland.GALATEA == LocationUtils.skyblockIsland;
    }

    public static final boolean isInSafari() {
        return LocationUtils.SkyblockIsland.SAFARI == LocationUtils.skyblockIsland;
    }

    public static final boolean isInGarden() {
        return LocationUtils.SkyblockIsland.GARDEN == LocationUtils.skyblockIsland;
    }

    public static final boolean isInDwarvenMines() {
        return LocationUtils.SkyblockIsland.DWARVEN_MINES == LocationUtils.skyblockIsland;
    }

    public static final boolean isInMineshaft() {
        return LocationUtils.SkyblockIsland.MINESHAFT == LocationUtils.skyblockIsland;
    }

    public static final boolean isInSkyblock() {
        return LocationUtils.isInSkyblock;
    }

    public static final boolean isInHypixel() {
        return LocationUtils.isInHypixel;
    }

    public static final boolean isInSingleplayer() {
        return null != Minecraft.getInstance().getSingleplayerServer();
    }

    private enum SkyblockIsland {
        DUNGEONS,
        GALATEA,
        SAFARI,
        GARDEN,
        DWARVEN_MINES,
        MINESHAFT;

        private SkyblockIsland() {
        }

        private static final @Nullable LocationUtils.SkyblockIsland fromId(@Nullable final String id) {
            return null == id ? null : switch (id) {
                case "dungeon" -> LocationUtils.SkyblockIsland.DUNGEONS;
                case "foraging_2", "foraging_3" -> LocationUtils.SkyblockIsland.GALATEA; // moonglade marsh + torrhus canyon
                case "safari" -> LocationUtils.SkyblockIsland.SAFARI;
                case "garden" -> LocationUtils.SkyblockIsland.GARDEN;
                case "mining_3" -> LocationUtils.SkyblockIsland.DWARVEN_MINES; // includes dwarven base camp/glacite tunnels, but not mineshaft
                case "mineshaft" -> LocationUtils.SkyblockIsland.MINESHAFT;
                default -> null;
            };
        }
    }
}

