package gg.darkutils.feat.mining;

import gg.darkutils.DarkUtils;
import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.utils.Helpers;
import gg.darkutils.utils.LocationUtils;
import gg.darkutils.utils.PrettyUtils;
import gg.darkutils.utils.RenderUtils;
import gg.darkutils.utils.TabListUtil;
import gg.darkutils.utils.TickUtils;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public final class PickaxeAbilityDisplay {
    @NotNull
    private static final RenderUtils.RenderingText TEXT =
            RenderUtils.createRenderingText();

    private static long expiresAt;

    private PickaxeAbilityDisplay() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final void init() {
        TickUtils.queueRepeatingTickTask(PickaxeAbilityDisplay::update, 60); // tab updates every 3s server-side anyway, no need to update more frequently
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(DarkUtils.MOD_ID, "pickaxe_ability_display"), (context, tickCounter) -> PickaxeAbilityDisplay.renderPickaxeAbilityDisplay(context));
    }

    private static final void update() {
        if (!LocationUtils.isInDwarvenMines() && !LocationUtils.isInMineshaft()) {
            return;
        }

        PickaxeAbilityDisplay.expiresAt = 0L;

        for (final var it = TabListUtil.tabListLinesIterator(); it.hasNext(); ) {
            final var line = it.next();

            if (line.startsWith("Pickaxe Ability") && it.hasNext()) {
                final var cd = it.next();

                if (cd.endsWith("Available")) {
                    PickaxeAbilityDisplay.expiresAt = -1L;
                } else {
                    final var cooldown = Helpers.extractNumbers(cd);

                    if (null != cooldown) {
                        try {
                            PickaxeAbilityDisplay.expiresAt = System.nanoTime() + TimeUnit.SECONDS.toNanos(Integer.parseInt(cooldown));
                        } catch (final NumberFormatException nfe) {
                            DarkUtils.error(PickaxeAbilityDisplay.class, "Error parsing cooldown \"" + cooldown + "\" from tablist widget", nfe);
                        }
                    }
                }

                break;
            }
        }
    }

    private static final boolean isEnabled() {
        return DarkUtilsConfig.INSTANCE.pickaxeAbilityDisplay;
    }

    private static final void renderPickaxeAbilityDisplay(@NotNull final GuiGraphicsExtractor context) {
        if (!PickaxeAbilityDisplay.isEnabled()) {
            return;
        }

        final var client = Minecraft.getInstance();
        final var mineshaft = LocationUtils.isInMineshaft();

        if (null == client.player || !LocationUtils.isInDwarvenMines() && !mineshaft) {
            return;
        }

        final var text = PickaxeAbilityDisplay.TEXT;
        var onCooldown = false;

        final var at = PickaxeAbilityDisplay.expiresAt;
        if (-1L == at) {
            text.setText("Pickaxe Ability: READY");
        } else if (0L == at) {
            text.setText("Pickaxe Ability: Could not detect");
        } else {
            final var remaining = PickaxeAbilityDisplay.expiresAt - System.nanoTime();
            if (0L >= remaining) {
                text.setText("Pickaxe Ability: READY");
            } else {
                text.setText("Pickaxe Ability: " + PrettyUtils.prettifyNanosToSeconds(remaining));
                onCooldown = true;
            }
        }

        final var Y_OFFSET = mineshaft ? 40 : 60; // offset a bit so that it shows under corpses per shaft if enabled

        RenderUtils.renderItem(
                context,
                onCooldown ? Items.CLOCK : Items.PRISMARINE_SHARD,
                RenderUtils.CHAT_ALIGNED_X,
                RenderUtils.MIDDLE_ALIGNED_Y.getAsInt() - (RenderUtils.CHAT_ALIGNED_X << 1) + Y_OFFSET // use chat's x offset to shift y a bit upwards so that it doesn't render under the text
        );

        RenderUtils.renderText(
                context,
                text,
                RenderUtils.CHAT_ALIGNED_X + RenderUtils.CHAT_ALIGNED_X * 10, // use chat's x offset to shift x a bit to the right so that there's a bit of a space after the rendered item before the text
                RenderUtils.MIDDLE_ALIGNED_Y.getAsInt() + Y_OFFSET,
                onCooldown ? ChatFormatting.GREEN : ChatFormatting.RED
        );
    }
}

