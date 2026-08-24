package gg.darkutils.feat.safari;

import gg.darkutils.DarkUtils;
import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.utils.LocationUtils;
import gg.darkutils.utils.RenderUtils;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.monster.Shulker;
import org.jetbrains.annotations.NotNull;

public final class HideonHighlighter {
    private HideonHighlighter() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final void init() {
        LevelRenderEvents.END_MAIN.register(HideonHighlighter::renderHideonHighlighter);
    }

    private static final boolean isEnabled() {
        return DarkUtilsConfig.INSTANCE.hideonHighlighter;
    }

    private static final void renderHideonHighlighter(@NotNull final LevelRenderContext context) {
        if (!HideonHighlighter.isEnabled() || (!LocationUtils.isInSafari() && !LocationUtils.isInGalatea())) {
            return;
        }

        final var client = Minecraft.getInstance();
        final var world = client.level;

        if (null == client.player || null == world) {
            return;
        }

        for (final var entity : world.entitiesForRendering()) {
            if (entity instanceof final Shulker shulker) {
                RenderUtils.drawFilledBlockOutline(
                        shulker.blockPosition(),
                        ChatFormatting.GOLD
                );
            }
        }
    }
}
