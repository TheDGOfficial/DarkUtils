package gg.darkutils.feat.foraging;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.events.ObtainTreeGiftEvent;
import gg.darkutils.events.base.EventRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Arrays;
import java.util.stream.Collectors;

public final class TreeGiftConfirmation {
    private TreeGiftConfirmation() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final void init() {
        EventRegistry.centralRegistry().addListener(TreeGiftConfirmation::onTreeGift);
    }

    private static final void onTreeGift(@NotNull final ObtainTreeGiftEvent event) {
        if (!DarkUtilsConfig.INSTANCE.treeGiftConfirmation) {
            return;
        }

        final var client = Minecraft.getInstance();

        if (null != client.player) {
            // Play sound only for this client
            client.player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 1.0F, 1.0F);

            // Show title/subtitle
            client.gui.setTitle(Component.nullToEmpty("§2Tree Gift!"));
            var subtitle = "§7You received the rewards!";

            final var mobSpawned = event.treeMobSpawned();

            if (TreeMobSpawned.NONE != mobSpawned) {
                final var prettyName = Arrays.stream(mobSpawned.name().split("_"))
                        .map(part -> part.substring(0, 1) + part.substring(1).toLowerCase(Locale.ROOT))
                        .collect(Collectors.joining(" "));

                subtitle = "§7A §d" + prettyName + " §7has spawned!";
            }

            client.gui.setSubtitle(Component.nullToEmpty(subtitle));
            client.gui.setTimes(10, 70, 20);
        }
    }
}
