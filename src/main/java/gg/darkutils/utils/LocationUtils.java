package gg.darkutils.utils;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.hypixel.data.type.GameType;
import net.hypixel.data.type.ServerType;
import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket;
import org.jetbrains.annotations.Nullable;

public final class LocationUtils {
    @Nullable
    private static String serverName;
    @Nullable
    private static ServerType serverType;
    @Nullable
    private static String mode;

    private LocationUtils() {
        super();

        throw new UnsupportedOperationException("static utility class");
    }

    public static final void init() {
        HypixelModAPI.getInstance().subscribeToEventPacket(ClientboundLocationPacket.class);

        HypixelModAPI.getInstance().createHandler(ClientboundLocationPacket.class, packet -> {
            LocationUtils.serverName = packet.getServerName();
            LocationUtils.serverType = packet.getServerType().orElse(null);
            LocationUtils.mode = packet.getMode().orElse(null);
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            LocationUtils.serverName = null;
            LocationUtils.serverType = null;
            LocationUtils.mode = null;
        });
    }

    public static final boolean isInDungeons() {
        return "dungeon".equals(LocationUtils.mode);
    }

    public static final boolean isInSkyblock() {
        return GameType.SKYBLOCK == LocationUtils.serverType;
    }

    public static final boolean isInHypixel() {
        return null != LocationUtils.serverName;
    }
}
