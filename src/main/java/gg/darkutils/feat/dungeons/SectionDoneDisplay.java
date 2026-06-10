package gg.darkutils.feat.dungeons;

import gg.darkutils.DarkUtils;
import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.events.ReceiveGameMessageEvent;
import gg.darkutils.events.base.EventRegistry;
import gg.darkutils.utils.LocationUtils;
import gg.darkutils.utils.RenderUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

public final class SectionDoneDisplay {
    @NotNull
    private static final RenderUtils.RenderingText TEXT =
            RenderUtils.createRenderingText();

    private static int sectionsDone;

    private SectionDoneDisplay() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final void init() {
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register(SectionDoneDisplay::onWorldChange);

        EventRegistry.centralRegistry().addListener(SectionDoneDisplay::onChat);

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(DarkUtils.MOD_ID, "section_done_display"), (context, tickCounter) -> SectionDoneDisplay.renderSectionDoneDisplay(context));
    }

    private static final void onChat(@NotNull final ReceiveGameMessageEvent event) {
        final var message = event.content();

        if (message.endsWith("! (7/7)") || message.endsWith("! (8/8)")) {
            ++SectionDoneDisplay.sectionsDone;
        }
    }

    private static final void onWorldChange(@NotNull final Minecraft mc, @NotNull final ClientLevel world) {
        SectionDoneDisplay.sectionsDone = 0;
    }

    private static final boolean isEnabled() {
        return DarkUtilsConfig.INSTANCE.sectionDoneDisplay;
    }

    private static final void renderSectionDoneDisplay(@NotNull final GuiGraphicsExtractor context) {
        if (!SectionDoneDisplay.isEnabled()) {
            return;
        }

        final var client = Minecraft.getInstance();
        final var sections = SectionDoneDisplay.sectionsDone;

        if (null == client.player || 4 == sections || !LocationUtils.isInDungeons() || !DungeonTimer.isInBetweenPhases(DungeonTimer.DungeonPhase.PHASE_2_CLEAR, DungeonTimer.DungeonPhase.TERMINALS_CLEAR)) {
            return;
        }

        final var text = SectionDoneDisplay.TEXT;

        text.setText("Sections Done: " + sections + "/4");

        RenderUtils.renderItem(
                context,
                Items.BRICKS,
                RenderUtils.middleAlignedXForText(text),
                RenderUtils.MIDDLE_ALIGNED_Y.getAsInt() - RenderUtils.CHAT_ALIGNED_X * 7 // use chat's x offset to shift y a bit upwards so that it doesn't render directly inside the crosshair, which is at exact middle
        );

        RenderUtils.renderText(
                context,
                text,
                RenderUtils.middleAlignedXForText(text) + RenderUtils.CHAT_ALIGNED_X * 10, // use chat's x offset to shift x a bit to the right so that there's a bit of a space after the rendered item before the text
                RenderUtils.MIDDLE_ALIGNED_Y.getAsInt() - RenderUtils.CHAT_ALIGNED_X * 5, // use chat's x offset to shift y a bit upwards so that it doesn't render directly inside the crosshair, which is at exact middle
                 sections == 3 ? ChatFormatting.GREEN : ChatFormatting.YELLOW
        );
    }
}

