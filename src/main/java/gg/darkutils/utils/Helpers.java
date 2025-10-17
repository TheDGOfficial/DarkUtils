package gg.darkutils.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

public final class Helpers {
    private Helpers() {
        super();

        throw new UnsupportedOperationException("static utility class");
    }

    public static final void displayCountdownTitles(@NotNull final String color, @NotNull final String finalText, final int seconds) {
        // Show the first number immediately
        Helpers.notify(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, color + seconds);

        // Queue the rest
        for (var i = seconds - 1; 0 < i; --i) {
            final var value = i;
            final var delay = 20 * (seconds - i);
            TickUtils.queueTickTask(
                    () -> Helpers.notify(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, color + value),
                    delay
            );
        }

        // Queue the final text
        TickUtils.queueTickTask(
                () -> Helpers.notify(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), color + finalText),
                20 * seconds
        );
    }

    private static final void notify(@NotNull final SoundEvent sound, @NotNull final String text) {
        final var client = MinecraftClient.getInstance();

        Helpers.playSound(sound, 1.0F, 1.0F);
        client.inGameHud.setTitle(Text.of(text));
        client.inGameHud.setTitleTicks(0, 20, 0);
    }

    private static final void playSound(@NotNull final SoundEvent sound, final float volume, final float pitch) {
        final var player = MinecraftClient.getInstance().player;

        if (null != player) {
            player.playSound(sound, volume, pitch);
        }
    }
}
