package gg.darkutils.feat.foraging;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.events.TreeGiftObtainedEvent;
import gg.darkutils.events.base.EventRegistry;
import gg.darkutils.utils.chat.ChatUtils;
import gg.darkutils.utils.TickUtils;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;

public final class TreeGiftFeatures {
    private static boolean endMessageReceived;
    private static @NotNull TreeMobSpawned treeMobSpawned = TreeMobSpawned.NONE;

    private TreeGiftFeatures() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final void init() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) {
                TreeGiftFeatures.onChat(message);
            }
        });
    }

    private static final void onChat(@NotNull final Text message) {
        if (!DarkUtilsConfig.INSTANCE.treeGiftConfirmation && !DarkUtilsConfig.INSTANCE.treeGiftsPerHour) {
            // Reset state to prevent bugs when feature is turned off
            TreeGiftFeatures.endMessageReceived = false;
            TreeGiftFeatures.treeMobSpawned = TreeMobSpawned.NONE;

            return;
        }

        final var plain = message.getString();
        switch (plain) {
            case "                                 TREE GIFT" -> {
                if (ChatUtils.hasFormatting(message, Formatting.DARK_GREEN, true)) {
                    TreeGiftFeatures.endMessageReceived = false;
                    TreeGiftFeatures.treeMobSpawned = TreeMobSpawned.NONE;
                    TickUtils.awaitCondition(
                            () -> TreeGiftFeatures.endMessageReceived,
                            () -> EventRegistry.centralRegistry().triggerEvent(new TreeGiftObtainedEvent(TreeGiftFeatures.treeMobSpawned))
                    );
                }
            }
            case "                     A Phanflare fell from the Tree!" -> {
                if (ChatUtils.hasFormatting(message, Formatting.GRAY, false)) {
                    TreeGiftFeatures.treeMobSpawned = TreeMobSpawned.PHANFLARE;
                }
            }
            case "                     A Phanpyre fell from the Tree!" -> {
                if (ChatUtils.hasFormatting(message, Formatting.GRAY, false)) {
                    TreeGiftFeatures.treeMobSpawned = TreeMobSpawned.PHANPYRE;
                }
            }
            case "                     A Dreadwing fell from the Tree!" -> {
                if (ChatUtils.hasFormatting(message, Formatting.GRAY, false)) {
                    TreeGiftFeatures.treeMobSpawned = TreeMobSpawned.DREADWING;
                }
            }
            case "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬" -> {
                if (!TreeGiftFeatures.endMessageReceived && ChatUtils.hasFormatting(message, Formatting.DARK_GREEN, true)) {
                    TreeGiftFeatures.endMessageReceived = true;
                }
            }
            default -> {
                // no-op
            }
        }
    }
}
