package gg.darkutils.feat.foraging;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.utils.ChatUtils;
import gg.darkutils.utils.TickUtils;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;

public final class TreeGiftFeatures {
    private static boolean endMessageReceived;
    private static @NotNull TreeGiftFeatures.MobSpawned mobSpawned = TreeGiftFeatures.MobSpawned.NONE;

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
            TreeGiftFeatures.mobSpawned = TreeGiftFeatures.MobSpawned.NONE;

            return;
        }

        final var plain = message.getString();
        switch (plain) {
            case "                                 TREE GIFT" -> {
                if (ChatUtils.hasFormatting(message, Formatting.DARK_GREEN, true)) {
                    TreeGiftFeatures.endMessageReceived = false;
                    TreeGiftFeatures.mobSpawned = TreeGiftFeatures.MobSpawned.NONE;
                    TickUtils.awaitCondition(
                            () -> TreeGiftFeatures.endMessageReceived,
                            () -> TreeGiftFeatures.onTreeGift(MinecraftClient.getInstance())
                    );
                }
            }
            case "                     A Phanflare fell from the Tree!" -> {
                if (ChatUtils.hasFormatting(message, Formatting.GRAY, false)) {
                    TreeGiftFeatures.mobSpawned = TreeGiftFeatures.MobSpawned.PHANFLARE;
                }
            }
            case "                     A Phanpyre fell from the Tree!" -> {
                if (ChatUtils.hasFormatting(message, Formatting.GRAY, false)) {
                    TreeGiftFeatures.mobSpawned = TreeGiftFeatures.MobSpawned.PHANPYRE;
                }
            }
            case "                     A Dreadwing fell from the Tree!" -> {
                if (ChatUtils.hasFormatting(message, Formatting.GRAY, false)) {
                    TreeGiftFeatures.mobSpawned = TreeGiftFeatures.MobSpawned.DREADWING;
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

    private static final void onTreeGift(@NotNull final MinecraftClient client) {
        if (DarkUtilsConfig.INSTANCE.treeGiftConfirmation) {
            TreeGiftConfirmation.onTreeGift(client, TreeGiftFeatures.mobSpawned);
        }

        if (DarkUtilsConfig.INSTANCE.treeGiftsPerHour) {
            TreeGiftsPerHour.onTreeGift();
        }
    }

    enum MobSpawned {
        NONE,
        PHANFLARE,
        PHANPYRE,
        DREADWING
    }
}
