package gg.darkutils.feat.foraging;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.events.ObtainTreeGiftEvent;
import gg.darkutils.events.ReceiveGameMessageEvent;
import gg.darkutils.events.base.EventRegistry;
import gg.darkutils.utils.TickUtils;
import gg.darkutils.utils.chat.SimpleColor;
import gg.darkutils.utils.chat.SimpleFormatting;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Consumer;

public final class TreeGiftFeatures {
    private static boolean endMessageReceived;
    private static @NotNull TreeMobSpawned treeMobSpawned = TreeMobSpawned.NONE;

    @NotNull
    private static final Map<String, Consumer<ReceiveGameMessageEvent>> MESSAGE_HANDLERS = Map.of(
            "                                 TREE GIFT", event -> {
                if (event.isStyledWith(SimpleColor.DARK_GREEN, SimpleFormatting.BOLD)) {
                    TreeGiftFeatures.endMessageReceived = false;
                    TreeGiftFeatures.treeMobSpawned = TreeMobSpawned.NONE;
                    TickUtils.awaitCondition(
                            () -> TreeGiftFeatures.endMessageReceived,
                            () -> new ObtainTreeGiftEvent(TreeGiftFeatures.treeMobSpawned).trigger()
                    );
                }
            },
            "                     A Phanflare fell from the Tree!", event -> {
                if (event.isStyledWith(SimpleColor.GRAY)) {
                    TreeGiftFeatures.treeMobSpawned = TreeMobSpawned.PHANFLARE;
                }
            },
            "                     A Phanpyre fell from the Tree!", event -> {
                if (event.isStyledWith(SimpleColor.GRAY)) {
                    TreeGiftFeatures.treeMobSpawned = TreeMobSpawned.PHANPYRE;
                }
            },
            "                     A Dreadwing fell from the Tree!", event -> {
                if (event.isStyledWith(SimpleColor.GRAY)) {
                    TreeGiftFeatures.treeMobSpawned = TreeMobSpawned.DREADWING;
                }
            },
            "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬", event -> {
                if (!TreeGiftFeatures.endMessageReceived && event.isStyledWith(SimpleColor.DARK_GREEN, SimpleFormatting.BOLD)) {
                    TreeGiftFeatures.endMessageReceived = true;
                }
            }
    );

    private TreeGiftFeatures() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final void init() {
        EventRegistry.centralRegistry().addListener(TreeGiftFeatures::onChat);
    }

    private static final void onChat(@NotNull final ReceiveGameMessageEvent event) {
        if (!DarkUtilsConfig.INSTANCE.treeGiftConfirmation && !DarkUtilsConfig.INSTANCE.treeGiftsPerHour) {
            // Reset state to prevent bugs when feature is turned off
            TreeGiftFeatures.endMessageReceived = false;
            TreeGiftFeatures.treeMobSpawned = TreeMobSpawned.NONE;

            return;
        }

        event.match(TreeGiftFeatures.MESSAGE_HANDLERS);
    }
}
