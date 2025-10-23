package gg.darkutils.feat.qol;

import gg.darkutils.DarkUtils;
import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.utils.LocationUtils;
import gg.darkutils.utils.RenderUtils;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public final class RejoinCooldownDisplay {
    private static final long COOLDOWN_MS = TimeUnit.MINUTES.toMillis(1L);

    private static long kickCooldownEnd;

    private RejoinCooldownDisplay() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final void init() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) {
                RejoinCooldownDisplay.onChat(message);
            }
        });

        HudElementRegistry.addLast(Identifier.of(DarkUtils.MOD_ID, "rejoin_cooldown_display"), (context, tickCounter) -> RejoinCooldownDisplay.renderRejoinCooldownDisplay(context));
    }

    private static final void onChat(@NotNull final Text message) {
        if (!DarkUtilsConfig.INSTANCE.rejoinCooldownDisplay) {
            return;
        }

        final var plain = message.getString();

        if ("You were kicked while joining that server!".equals(plain) || "There was a problem joining SkyBlock, try again in a moment!".equals(plain)) {
            RejoinCooldownDisplay.kickCooldownEnd = System.currentTimeMillis() + RejoinCooldownDisplay.COOLDOWN_MS;
        }
    }

    private static final void renderRejoinCooldownDisplay(@NotNull final DrawContext context) {
        if (!DarkUtilsConfig.INSTANCE.rejoinCooldownDisplay) {
            return;
        }

        final var client = MinecraftClient.getInstance();

        if (null == client.player) {
            return;
        }

        if (0L == RejoinCooldownDisplay.kickCooldownEnd) {
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

        RenderUtils.renderText(
                context,
                text,
                RenderUtils.MIDDLE_ALIGNED_X,
                RenderUtils.MIDDLE_ALIGNED_Y,
                color
        );
    }
}
