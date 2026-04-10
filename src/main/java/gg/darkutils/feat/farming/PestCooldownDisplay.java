package gg.darkutils.feat.farming;

import gg.darkutils.DarkUtils;
import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.events.ReceiveGameMessageEvent;
import gg.darkutils.events.base.EventRegistry;
import gg.darkutils.utils.LocationUtils;
import gg.darkutils.utils.RenderUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.Items;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

public final class PestCooldownDisplay {
    @NotNull
    private static final RenderUtils.RenderingText TEXT =
            RenderUtils.createRenderingText();

    private static long lastPestSpawnTime;

    private PestCooldownDisplay() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final void init() {
        EventRegistry.centralRegistry().addListener(PestCooldownDisplay::onChat);
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register(PestCooldownDisplay::onWorldChange);
        HudElementRegistry.addLast(Identifier.of(DarkUtils.MOD_ID, "pest_cooldown_display"), (context, tickCounter) -> PestCooldownDisplay.renderPestCooldownDisplay(context));
    }

    private static final boolean isEnabled() {
        return DarkUtilsConfig.INSTANCE.pestCooldownDisplay;
    }

    private static final void reset() {
        PestCooldownDisplay.lastPestSpawnTime = 0L;
    }

    private static final void onWorldChange(@NotNull final MinecraftClient client, @Nullable final ClientWorld world) {
        PestCooldownDisplay.reset();
    }

    private static final void onChat(@NotNull final ReceiveGameMessageEvent event) {
        if (!PestCooldownDisplay.isEnabled()) {
            PestCooldownDisplay.reset();
            return;
        }

        final var content = event.content();

        if (content.contains("ൠ Pest") && (content.contains("have spawned in") || content.contains("has appeared in"))) {
            PestCooldownDisplay.lastPestSpawnTime = System.nanoTime();
        }
    }

    private static final void renderPestCooldownDisplay(@NotNull final DrawContext context) {
        if (!PestCooldownDisplay.isEnabled()) {
            PestCooldownDisplay.reset();
            return;
        }

        final var client = MinecraftClient.getInstance();

        if (null == client.player || !LocationUtils.isInGarden()) {
            return;
        }

        final var spawnCd = DarkUtilsConfig.INSTANCE.pestCooldown;
        final var lastSpawn = PestCooldownDisplay.lastPestSpawnTime;

        final var now = System.nanoTime();
        final var cooldownEnd = 0L == lastSpawn ? now : lastSpawn + TimeUnit.SECONDS.toNanos(spawnCd);

        final var remainingNs = cooldownEnd - now;
        final var ready = 0L >= remainingNs;

        final var remainingSeconds = TimeUnit.NANOSECONDS.toSeconds(Math.max(remainingNs, 0L));

        final var text = PestCooldownDisplay.TEXT;
        text.setText("Pest Cooldown: " + (ready ? "READY" : remainingSeconds + "s"));

        RenderUtils.renderItem(
                context,
                ready ? Items.TNT_MINECART : Items.CLOCK,
                RenderUtils.CHAT_ALIGNED_X,
                RenderUtils.MIDDLE_ALIGNED_Y.getAsInt() - (RenderUtils.CHAT_ALIGNED_X << 1) // use chat's x offset to shift y a bit upwards so that it doesn't render under the text
        );

        RenderUtils.renderText(
                context,
                text,
                RenderUtils.CHAT_ALIGNED_X + RenderUtils.CHAT_ALIGNED_X * 10, // use chat's x offset to shift x a bit to the right so that there's a bit of a space after the rendered item before the text
                RenderUtils.MIDDLE_ALIGNED_Y,
                ready ? Formatting.GREEN : Formatting.DARK_GREEN
        );
    }
}
