package gg.darkutils.feat.mining;

import gg.darkutils.DarkUtils;
import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.utils.LocationUtils;
import gg.darkutils.utils.RenderUtils;
import gg.darkutils.utils.TickUtils;
import gg.darkutils.utils.chat.ChatUtils;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class LittlefootDisplay {
    @NotNull
    private static RenderUtils.RenderingText NO_LITTLEFOOTS = RenderUtils.createRenderingText();

    @Nullable
    private static RenderUtils.RenderingText @Nullable [] LINES;

    @Nullable
    private static List<FormattedCharSequence> littlefoots;

    private LittlefootDisplay() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final void init() {
        TickUtils.queueRepeatingTickTask(LittlefootDisplay::update, 1);
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(DarkUtils.MOD_ID, "littlefoot_display"), (context, tickCounter) -> LittlefootDisplay.renderLittlefootDisplay(context));
    }

    private static final void update() {
        if (!LittlefootDisplay.isEnabled()) {
            return;
        }

        final var mc = Minecraft.getInstance();

        final var world = mc.level;
        final var player = mc.player;

        if (null != world && null != player) {
            if (LocationUtils.isInMineshaft()) {
                final var littlefoots = new ArrayList<FormattedCharSequence>();

                for (final var entity : world.entitiesForRendering()) {
                    if (entity instanceof final ArmorStand stand) {
                        final var name = stand.getCustomName();
                        if (null != name) {
                            final var clean = ChatUtils.removeControlCodes(name.getString());
                            if (clean.contains("Littlefoot")) {
                                littlefoots.add(name.getVisualOrderText());
                            }
                        }
                    }
                }

                final var newArray = new RenderUtils.RenderingText[littlefoots.size()];
                for (int i = 0, len = newArray.length; i < len; ++i) {
                    newArray[i] = RenderUtils.createRenderingText();
                }

                LittlefootDisplay.LINES = newArray;
                LittlefootDisplay.littlefoots = littlefoots;
            }
        }
    }

    private static final boolean isEnabled() {
        return DarkUtilsConfig.INSTANCE.littlefootDisplay;
    }

    private static final void renderLittlefootDisplay(@NotNull final GuiGraphics context) {
        if (!LittlefootDisplay.isEnabled()) {
            return;
        }

        final var client = Minecraft.getInstance();

        if (null == client.player || !LocationUtils.isInMineshaft()) {
            return;
        }

        final var Y_OFFSET = 60; // offset a bit so that it shows under will o wisp display if enabled
        final var Y_OFFSET_PER_LITTLEFOOT = 20; // offset each line a bit

        final var littlefoots = LittlefootDisplay.littlefoots;
        final var lines = LittlefootDisplay.LINES;

        if (null == littlefoots || null == lines || littlefoots.isEmpty() || 0 == lines.length) {
            RenderUtils.renderItem(
                    context,
                    Items.PACKED_ICE,
                    RenderUtils.CHAT_ALIGNED_X,
                    RenderUtils.MIDDLE_ALIGNED_Y.getAsInt() - (RenderUtils.CHAT_ALIGNED_X << 1) + Y_OFFSET // use chat's x offset to shift y a bit upwards so that it doesn't render under the text
            );

            final var text = LittlefootDisplay.NO_LITTLEFOOTS;
            text.setText("No littlefoots in render distance!");

            RenderUtils.renderText(
                    context,
                    text,
                    RenderUtils.CHAT_ALIGNED_X + RenderUtils.CHAT_ALIGNED_X * 10, // use chat's x offset to shift x a bit to the right so that there's a bit of a space after the rendered item before the text
                    RenderUtils.MIDDLE_ALIGNED_Y.getAsInt() + Y_OFFSET,
                    ChatFormatting.RED
            );

            return;
        }

        if (littlefoots.size() != lines.length) {
            throw new IllegalStateException("Data inconsistency");
        }

        var currentOffset = 0;

        for (int i = 0, len = littlefoots.size(); i < len; ++i) {
            final var littlefoot = littlefoots.get(i);

            RenderUtils.renderItem(
                    context,
                    Items.PACKED_ICE,
                    RenderUtils.CHAT_ALIGNED_X,
                    RenderUtils.MIDDLE_ALIGNED_Y.getAsInt() - (RenderUtils.CHAT_ALIGNED_X << 1) + Y_OFFSET + currentOffset // use chat's x offset to shift y a bit upwards so that it doesn't render under the text
            );

            final var text = lines[i];
            text.setShownText(littlefoot);

            RenderUtils.renderText(
                    context,
                    text,
                    RenderUtils.CHAT_ALIGNED_X + RenderUtils.CHAT_ALIGNED_X * 10, // use chat's x offset to shift x a bit to the right so that there's a bit of a space after the rendered item before the text
                    RenderUtils.MIDDLE_ALIGNED_Y.getAsInt() + Y_OFFSET + currentOffset,
                    ChatFormatting.WHITE // overridden by the text
            );

            currentOffset += Y_OFFSET_PER_LITTLEFOOT;
        }
    }
}
