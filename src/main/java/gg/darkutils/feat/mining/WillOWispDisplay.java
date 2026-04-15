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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class WillOWispDisplay {
    @NotNull
    private static final RenderUtils.RenderingText TEXT =
            RenderUtils.createRenderingText();

    @Nullable
    private static String timeLeft;

    private WillOWispDisplay() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final void init() {
        TickUtils.queueRepeatingTickTask(WillOWispDisplay::update, 20); // 1 second precision, hypixel sends time left of deployables in seconds anyway
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(DarkUtils.MOD_ID, "will_o_wisp_display"), (context, tickCounter) -> WillOWispDisplay.renderWillOWispDisplay(context));
    }

    private static final void update() {
        final var mineshaft = LocationUtils.isInMineshaft();

        if (!WillOWispDisplay.isEnabled() || (!mineshaft && !LocationUtils.isInDwarvenMines())) {
            return;
        }

        WillOWispDisplay.timeLeft = null;

        final var mc = Minecraft.getInstance();

        final var world = mc.level;
        final var player = mc.player;

        if (null != world && null != player) {
            if (mineshaft) {
                // range is infinite when in a shaft
                for (final var entity : world.entitiesForRendering()) {
                    if (entity instanceof final ArmorStand stand) {
                        WillOWispDisplay.processPossibleWillOWispTag(stand);
                    }
                }
            } else {
                // 30 block range
                for (final var stand : world.getEntities(EntityType.ARMOR_STAND, player.getBoundingBox().inflate(30.0D), ignored -> true)) {
                    WillOWispDisplay.processPossibleWillOWispTag(stand);
                }
            }
        }
    }

    private static final void processPossibleWillOWispTag(@NotNull final ArmorStand stand) {
        final var name = stand.getCustomName();
        if (null != name) {
            final var clean = ChatUtils.removeControlCodes(name.getString());
            if (clean.startsWith("Will-o'-wisp ") && 's' == clean.charAt(clean.length() - 1)) {
                WillOWispDisplay.timeLeft = clean.replace("Will-o'-wisp ", "");
            }
        }
    }

    private static final boolean isEnabled() {
        return DarkUtilsConfig.INSTANCE.willOWispDisplay;
    }

    private static final void renderWillOWispDisplay(@NotNull final GuiGraphics context) {
        if (!WillOWispDisplay.isEnabled()) {
            return;
        }

        final var client = Minecraft.getInstance();
        final var mineshaft = LocationUtils.isInMineshaft();

        if (null == client.player || (!LocationUtils.isInDwarvenMines() && !mineshaft)) {
            return;
        }

        final var text = WillOWispDisplay.TEXT;

        final var time = WillOWispDisplay.timeLeft;
        final var hasTime = null != time;

        text.setText("Will-o'-wisp " + (hasTime ? time : "NOT ACTIVE"));

        final var Y_OFFSET = mineshaft ? 60 : 80; // offset a bit so that it shows under pickaxe ability display if enabled

        RenderUtils.renderItem(
                context,
                hasTime ? Items.GOLDEN_PICKAXE : Items.WOODEN_PICKAXE,
                RenderUtils.CHAT_ALIGNED_X,
                RenderUtils.MIDDLE_ALIGNED_Y.getAsInt() - (RenderUtils.CHAT_ALIGNED_X << 1) + Y_OFFSET // use chat's x offset to shift y a bit upwards so that it doesn't render under the text
        );

        RenderUtils.renderText(
                context,
                text,
                RenderUtils.CHAT_ALIGNED_X + RenderUtils.CHAT_ALIGNED_X * 10, // use chat's x offset to shift x a bit to the right so that there's a bit of a space after the rendered item before the text
                RenderUtils.MIDDLE_ALIGNED_Y.getAsInt() + Y_OFFSET,
                hasTime ? ChatFormatting.GOLD : ChatFormatting.RED
        );
    }
}
