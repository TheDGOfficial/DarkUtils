package gg.darkutils.feat.safari;

import gg.darkutils.DarkUtils;
import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.utils.LocationUtils;
import gg.darkutils.utils.PrettyUtils;
import gg.darkutils.utils.RenderUtils;
import gg.darkutils.utils.TickUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

public final class SafariTimer {
    @NotNull
    private static final RenderUtils.RenderingText TEXT =
            RenderUtils.createRenderingText();

    private static long timer;
    private static boolean wasInSafari;

    private SafariTimer() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final void init() {
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register(SafariTimer::reset);
        TickUtils.queueRepeatingTickTask(SafariTimer::update, 1);
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(DarkUtils.MOD_ID, "safari_timer"), (context, tickCounter) -> SafariTimer.renderSafariTimer(context));
    }

    private static final void update() {
        final var isInSafari = LocationUtils.isInSafari();

        if (isInSafari && !SafariTimer.wasInSafari) {
            SafariTimer.timer = System.nanoTime();
        }

        SafariTimer.wasInSafari = isInSafari;
    }

    private static final void reset(@NotNull final Minecraft client, @NotNull final ClientLevel world) {
        SafariTimer.timer = 0L;
        SafariTimer.wasInSafari = false;
    }

    private static final boolean isEnabled() {
        return DarkUtilsConfig.INSTANCE.safariTimer;
    }

    private static final void renderSafariTimer(@NotNull final GuiGraphicsExtractor context) {
        if (!SafariTimer.isEnabled()) {
            return;
        }

        final var client = Minecraft.getInstance();

        if (null == client.player || !LocationUtils.isInSafari()) {
            return;
        }

        final var text = SafariTimer.TEXT;
        final var elapsed = System.nanoTime() - SafariTimer.timer;

        text.setText("Safari Timer " + PrettyUtils.prettifyNanosToSeconds(elapsed));

        RenderUtils.renderItem(
                context,
                Items.CLOCK,
                RenderUtils.CHAT_ALIGNED_X,
                RenderUtils.MIDDLE_ALIGNED_Y.getAsInt() - (RenderUtils.CHAT_ALIGNED_X << 1)
        );

        RenderUtils.renderText(
                context,
                text,
                RenderUtils.CHAT_ALIGNED_X + RenderUtils.CHAT_ALIGNED_X * 10,
                RenderUtils.MIDDLE_ALIGNED_Y.getAsInt(),
                ChatFormatting.GOLD
        );
    }
}
