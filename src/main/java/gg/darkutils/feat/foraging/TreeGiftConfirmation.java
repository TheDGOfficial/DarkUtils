package gg.darkutils.feat.foraging;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.events.ObtainTreeGiftEvent;
import gg.darkutils.events.base.EventRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

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

        final var client = MinecraftClient.getInstance();

        if (null != client.player) {
            // Play sound only for this client
            client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0F, 1.0F);

            // Show title/subtitle
            client.inGameHud.setTitle(Text.of("§2Tree Gift!"));
            var subtitle = "§7You received the rewards!";

            final var mobSpawned = event.treeMobSpawned();

            if (TreeMobSpawned.NONE != mobSpawned) {
                final var lowerCaseName = mobSpawned.name().toLowerCase(Locale.ROOT);
                final var prettyName = Character.toUpperCase(lowerCaseName.charAt(0)) + lowerCaseName.substring(1);

                subtitle = "§7A §d" + prettyName + " §7has spawned!";
            }

            client.inGameHud.setSubtitle(Text.of(subtitle));
            client.inGameHud.setTitleTicks(10, 70, 20);
        }
    }
}
