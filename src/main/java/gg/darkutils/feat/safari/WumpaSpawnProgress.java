package gg.darkutils.feat.safari;

import gg.darkutils.DarkUtils;
import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.events.ReceiveGameMessageEvent;
import gg.darkutils.events.base.EventRegistry;
import gg.darkutils.utils.RenderUtils;
import gg.darkutils.utils.LocationUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public final class WumpaSpawnProgress {
    private static final int Y_OFFSET = 20;
    private static final int LINE_HEIGHT = 20;

    @NotNull
    private static final ArrayList<WumpaSpawnProgress.RenderableLine> LINES =
            new ArrayList<>(9);

    static {
        WumpaSpawnProgress.LINES.add(new WumpaSpawnProgress.RenderableLine(
                RenderUtils.createRenderingText(),
                ChatFormatting.GOLD,
                null
        ));

        for (final var mob : WumpaSpawnProgress.UniqueMob.values()) {
            WumpaSpawnProgress.LINES.add(new WumpaSpawnProgress.RenderableLine(
                    RenderUtils.createRenderingText(),
                    mob.color,
                    null
            ));
        }
    }

    private WumpaSpawnProgress() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final void init() {
        EventRegistry.centralRegistry().addListener(WumpaSpawnProgress::onChat);
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register(WumpaSpawnProgress::reset);
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(DarkUtils.MOD_ID, "wumpa_spawn_progress"),
                (context, tickCounter) -> WumpaSpawnProgress.renderWumpaSpawnProgress(context)
        );
    }

    private static final boolean isEnabled() {
        return DarkUtilsConfig.INSTANCE.wumpaSpawnProgress;
    }

    private static final void onChat(@NotNull final ReceiveGameMessageEvent event) {
        if (!WumpaSpawnProgress.isEnabled()) {
            return;
        }

        final var content = event.content();

        if (!content.contains("catching") || (!content.contains("CAPTURE!") && !content.contains("LOOT SHARE!")) || (!content.contains("You caught a ") && !content.contains("You received "))) {
            return;
        }

        for (final var mob : WumpaSpawnProgress.UniqueMob.values()) {
            if (content.contains(mob.name)) {
                mob.spawned = true;
                return;
            }
        }
    }

    private static final void reset(@NotNull final Minecraft client, @NotNull final ClientLevel world) {
        WumpaSpawnProgress.UniqueMob.reset();
    }

    private static final void renderWumpaSpawnProgress(@NotNull final GuiGraphicsExtractor context) {
        if (!WumpaSpawnProgress.isEnabled()) {
            return;
        }

        final var client = Minecraft.getInstance();

        if (null == client.player || !LocationUtils.isInSafari()) {
            return;
        }

        final var completed = WumpaSpawnProgress.UniqueMob.completedCount();
        final var total = WumpaSpawnProgress.UniqueMob.values().length;

        final var header = WumpaSpawnProgress.LINES.getFirst();
        header.renderingText().setText("Wumpa Spawn Progress: " + completed + "/" + total);

        final var baseX = RenderUtils.CHAT_ALIGNED_X;
        final var baseY = 20;

        RenderUtils.renderText(
                context,
                header.renderingText(),
                baseX,
                baseY + WumpaSpawnProgress.Y_OFFSET,
                header.color()
        );

        for (int i = 0, len = WumpaSpawnProgress.UniqueMob.values().length; i < len; ++i) {
            final var mob = WumpaSpawnProgress.UniqueMob.values()[i];
            final var line = WumpaSpawnProgress.LINES.get(i + 1);

            line.renderingText().setText(
                    mob.name + ": " + (mob.spawned ? "✔" : "✖")
            );

            RenderUtils.renderText(
                    context,
                    line.renderingText(),
                    baseX,
                    baseY + WumpaSpawnProgress.Y_OFFSET + (i + 1) * WumpaSpawnProgress.LINE_HEIGHT,
                    line.color()
            );
        }
    }

    private enum UniqueMob {
        NOZZLENOSE("Nozzlenose", ChatFormatting.BLUE),
        TEPID("Tepid", ChatFormatting.WHITE),
        MANTIS_SHRIMP("Mantis Shrimp", ChatFormatting.BLUE),
        STRONGARM("Strongarm", ChatFormatting.WHITE),
        TROODON("Troodon", ChatFormatting.BLUE),
        SHUDDERSQUID("Shuddersquid", ChatFormatting.GREEN),
        BILLYGOAT("Billygoat", ChatFormatting.BLUE),
        POLARIS("Polaris", ChatFormatting.GREEN);

        private static final UniqueMob[] VALUES = UniqueMob.values();

        @NotNull
        private final String name;

        @NotNull
        private final ChatFormatting color;

        private boolean spawned;

        private UniqueMob(@NotNull final String name, @NotNull final ChatFormatting color) {
            this.name = name;
            this.color = color;
        }

        private static final int completedCount() {
            var completed = 0;

            for (final var mob : UniqueMob.VALUES) {
                if (mob.spawned) {
                    ++completed;
                }
            }

            return completed;
        }

        private static final void reset() {
            for (final var mob : UniqueMob.VALUES) {
                mob.spawned = false;
            }
        }
    }

    private record RenderableLine(
            RenderUtils.RenderingText renderingText,
            ChatFormatting color,
            @Nullable Item optionalItemIcon
    ) {
    }
}
