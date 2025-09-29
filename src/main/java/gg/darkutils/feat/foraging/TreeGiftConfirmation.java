package gg.darkutils.feat.foraging;

import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

final class TreeGiftConfirmation {
    private TreeGiftConfirmation() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    static final void onTreeGift(final MinecraftClient client, @NotNull final TreeGiftFeatures.MobSpawned mobSpawned) {
        if (null != client.player) {
            // Play sound only for this client
            client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0F, 1.0F);

            // Show title/subtitle
            client.inGameHud.setTitle(Text.of("§2Tree Gift!"));
            var subtitle = "§7You received the rewards!";

            if (TreeGiftFeatures.MobSpawned.NONE != mobSpawned) {
                final var lowerCaseName = mobSpawned.name().toLowerCase(Locale.ROOT);
                final var prettyName = Character.toUpperCase(lowerCaseName.charAt(0)) + lowerCaseName.substring(1);

                subtitle = "§7A §d" + prettyName + " §7has spawned!";
            }

            client.inGameHud.setSubtitle(Text.of(subtitle));
            client.inGameHud.setTitleTicks(10, 70, 20);
        }
    }
}
