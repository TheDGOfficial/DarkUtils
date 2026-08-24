package gg.darkutils.feat.safari;

import gg.darkutils.DarkUtils;
import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.utils.LocationUtils;
import gg.darkutils.utils.RenderUtils;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Interaction;
import org.jetbrains.annotations.NotNull;

public final class DuplicoHighlighter {
    private DuplicoHighlighter() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final void init() {
        LevelRenderEvents.END_MAIN.register(DuplicoHighlighter::renderDuplicoHighlighter);
    }

    private static final boolean isEnabled() {
        return DarkUtilsConfig.INSTANCE.duplicoHighlighter;
    }

    private static final void renderDuplicoHighlighter(@NotNull final LevelRenderContext context) {
        if (!DuplicoHighlighter.isEnabled() || !LocationUtils.isInSafari()) {
            return;
        }

        final var client = Minecraft.getInstance();
        final var world = client.level;

        if (null == client.player || null == world) {
            return;
        }

        for (final var entity : world.entitiesForRendering()) {
            if (entity instanceof final Interaction interactionEntity) {
                RenderUtils.drawFilledBlockOutline(
                        interactionEntity.blockPosition(),
                        ChatFormatting.RED
                );
            }
        }
    }
}
