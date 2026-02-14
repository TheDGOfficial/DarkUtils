package gg.darkutils.utils;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.hypixel.data.type.GameType;
import net.hypixel.data.type.ServerType;
import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class LocationUtils {
    private static boolean isInHypixel;
    private static boolean isInSkyblock;
    @Nullable
    private static SkyblockIsland skyblockIsland;

    public enum SkyblockIsland {
        DUNGEONS,
        GALATEA;

        private SkyblockIsland() {
        }

        public static final @Nullable LocationUtils.SkyblockIsland fromId(@Nullable final String id) {
            if (null == id) {
                return null;
            }

            return switch (id) {
                case "dungeon"    -> LocationUtils.SkyblockIsland.DUNGEONS;
                case "foraging_2" -> LocationUtils.SkyblockIsland.GALATEA;
                default           -> null;
            };
        }
    }

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
        LocationUtils.isInHypixel = null != packet.getServerName();
        LocationUtils.isInSkyblock = GameType.SKYBLOCK == packet.getServerType().orElse(null);
        LocationUtils.skyblockIsland = LocationUtils.SkyblockIsland.fromId(packet.getMode().orElse(null));
    }

    private static final void onQuit(@NotNull final ClientPlayNetworkHandler handler, @NotNull final MinecraftClient client) {
        LocationUtils.isInHypixel = false;
        LocationUtils.isInSkyblock = false;
        LocationUtils.skyblockIsland = null;
    }

    public static final boolean isInDungeons() {
        return LocationUtils.SkyblockIsland.DUNGEONS == LocationUtils.skyblockIsland;
    }

    public static final boolean isInGalatea() {
        return LocationUtils.SkyblockIsland.GALATEA == LocationUtils.skyblockIsland;
    }

    public static final boolean isInSkyblock() {
        return LocationUtils.isInSkyblock;
    }

    public static final boolean isInHypixel() {
        return LocationUtils.isInHypixel;
    }

    public static final boolean isInSingleplayer() {
        return MinecraftClient.getInstance().isConnectedToLocalServer();
    }
}

