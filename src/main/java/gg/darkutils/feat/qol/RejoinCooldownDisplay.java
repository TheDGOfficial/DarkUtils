package gg.darkutils.feat.qol;

import gg.darkutils.DarkUtils;
import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.events.ReceiveGameMessageEvent;
import gg.darkutils.events.base.EventRegistry;
import gg.darkutils.utils.LocationUtils;
import gg.darkutils.utils.RenderUtils;
import gg.darkutils.utils.chat.BasicColor;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Items;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class RejoinCooldownDisplay {
    private static final long COOLDOWN_MS = TimeUnit.MINUTES.toMillis(1L);

    private static long kickCooldownEnd;

    @NotNull
    private static final Consumer<ReceiveGameMessageEvent> MESSAGE_ACTION = event -> {
        if (event.isStyledWith(BasicColor.RED)) {
            RejoinCooldownDisplay.kickCooldownEnd = System.currentTimeMillis() + RejoinCooldownDisplay.COOLDOWN_MS;
        }
    };

    @NotNull
    private static final Map<String, Consumer<ReceiveGameMessageEvent>> MESSAGE_HANDLERS = Map.of(
            "You were kicked while joining that server!", RejoinCooldownDisplay.MESSAGE_ACTION,
            "There was a problem joining SkyBlock, try again in a moment!", RejoinCooldownDisplay.MESSAGE_ACTION
    );

    private RejoinCooldownDisplay() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final void init() {
        EventRegistry.centralRegistry().addListener(RejoinCooldownDisplay::onChat);

        HudElementRegistry.addLast(Identifier.of(DarkUtils.MOD_ID, "rejoin_cooldown_display"), (context, tickCounter) -> RejoinCooldownDisplay.renderRejoinCooldownDisplay(context));
    }

    private static final void onChat(@NotNull final ReceiveGameMessageEvent event) {
        if (!DarkUtilsConfig.INSTANCE.rejoinCooldownDisplay) {
            return;
        }

        event.match(RejoinCooldownDisplay.MESSAGE_HANDLERS);
    }

    private static final void renderRejoinCooldownDisplay(@NotNull final DrawContext context) {
        if (!DarkUtilsConfig.INSTANCE.rejoinCooldownDisplay) {
            return;
        }

        final var client = MinecraftClient.getInstance();

        if (null == client.player || 0L == RejoinCooldownDisplay.kickCooldownEnd) {
            return;
        }

        final var timeLeftSeconds = Math.max(0L, TimeUnit.MILLISECONDS.toSeconds(RejoinCooldownDisplay.kickCooldownEnd - System.currentTimeMillis()));

        if (LocationUtils.isInSkyblock()) {
            if (0L == timeLeftSeconds) {
                RejoinCooldownDisplay.kickCooldownEnd = 0L;
            }

            return;
        }

        final var text = 0L == timeLeftSeconds ? "Try rejoining SkyBlock now!" : "Can rejoin SkyBlock in " + timeLeftSeconds + 's';
        final var color = 0L == timeLeftSeconds ? Formatting.GREEN : Formatting.RED;

        RenderUtils.renderItem(
                context,
                Items.CLOCK,
                RenderUtils.middleAlignedXForText(text),
                RenderUtils.MIDDLE_ALIGNED_Y.getAsInt() - RenderUtils.CHAT_ALIGNED_X * 7 // use chat's x offset to shift y a bit upwards so that it doesn't render directly inside the crosshair, which is at exact middle
        );

        RenderUtils.renderText(
                context,
                text,
                RenderUtils.middleAlignedXForText(text) + RenderUtils.CHAT_ALIGNED_X * 10, // use chat's x offset to shift x a bit to the right so that there's a bit of a space after the rendered item before the text
                RenderUtils.MIDDLE_ALIGNED_Y.getAsInt() - RenderUtils.CHAT_ALIGNED_X * 5, // use chat's x offset to shift y a bit upwards so that it doesn't render directly inside the crosshair, which is at exact middle
                color
        );
    }
}
