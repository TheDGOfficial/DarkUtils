package gg.darkutils.utils;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket;
import org.jetbrains.annotations.Nullable;

public final class LocationUtils {
    @Nullable
    private static String mode;

    private LocationUtils() {
        super();

        throw new UnsupportedOperationException("static utility class");
    }

    public static final void init() {
        HypixelModAPI.getInstance().subscribeToEventPacket(ClientboundLocationPacket.class);

        HypixelModAPI.getInstance().createHandler(ClientboundLocationPacket.class, packet -> LocationUtils.mode = packet.getMode().orElse(null));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> LocationUtils.mode = null);
    }

    public static final boolean isInDungeons() {
        return "dungeon".equals(LocationUtils.mode);
    }

    public static final boolean isInHypixel() {
        return null != LocationUtils.mode;
    }
}
