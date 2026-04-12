package gg.darkutils.feat.mining;

import gg.darkutils.DarkUtils;
import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.data.PersistentData;
import gg.darkutils.utils.LocationUtils;
import gg.darkutils.utils.PrettyUtils;
import gg.darkutils.utils.RenderUtils;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.Items;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public final class CorpsesPerShaftDisplay {
    @NotNull
    private static final RenderUtils.RenderingText TEXT =
            RenderUtils.createRenderingText();

    private CorpsesPerShaftDisplay() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final void init() {
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(DarkUtils.MOD_ID, "corpses_per_shaft_display"), (context, tickCounter) -> CorpsesPerShaftDisplay.renderCorpsesPerShaftDisplay(context));
    }

    private static final boolean isEnabled() {
        return DarkUtilsConfig.INSTANCE.corpsesPerShaftDisplay;
    }

    private static final void renderCorpsesPerShaftDisplay(@NotNull final GuiGraphicsExtractor context) {
        if (!CorpsesPerShaftDisplay.isEnabled()) {
            return;
        }

        final var client = Minecraft.getInstance();
        final var mineshaft = LocationUtils.isInMineshaft();

        if (null == client.player || !LocationUtils.isInDwarvenMines() && !mineshaft) {
            return;
        }

        final var text = CorpsesPerShaftDisplay.TEXT;

        final var shafts = PersistentData.INSTANCE.shaftsEntered;

        if (0 == shafts) {
            text.setText("No shafts yet");
        } else {
            final var lapis = PrettyUtils.formatPercentage(100.0D * ((double) PersistentData.INSTANCE.lapisCorpsesOpened / shafts));
            final var umber = PrettyUtils.formatPercentage(100.0D * ((double) PersistentData.INSTANCE.umberCorpsesOpened / shafts));
            final var tungsten = PrettyUtils.formatPercentage(100.0D * ((double) PersistentData.INSTANCE.tungstenCorpsesOpened / shafts));
            final var vanguard = PrettyUtils.formatPercentage(100.0D * ((double) PersistentData.INSTANCE.vanguardCorpsesOpened / shafts));

            text.setText("Corpses per Shaft: " +
                    lapis + " Lapis | " +
                    umber + " Umber | " +
                    tungsten + " Tungsten | " +
                    vanguard + " Vanguard"
            );
        }

        final var Y_OFFSET = mineshaft ? 20 : 40; // offset a bit so that it shows under mineshaft display if enabled

        RenderUtils.renderItem(
                context,
                Items.BOOK,
                RenderUtils.CHAT_ALIGNED_X,
                RenderUtils.MIDDLE_ALIGNED_Y.getAsInt() - (RenderUtils.CHAT_ALIGNED_X << 1) + Y_OFFSET // use chat's x offset to shift y a bit upwards so that it doesn't render under the text
        );

        RenderUtils.renderText(
                context,
                text,
                RenderUtils.CHAT_ALIGNED_X + RenderUtils.CHAT_ALIGNED_X * 10, // use chat's x offset to shift x a bit to the right so that there's a bit of a space after the rendered item before the text
                RenderUtils.MIDDLE_ALIGNED_Y.getAsInt() + Y_OFFSET,
                ChatFormatting.YELLOW
        );
    }
}

