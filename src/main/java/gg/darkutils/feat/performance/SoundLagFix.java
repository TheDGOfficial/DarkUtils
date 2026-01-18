package gg.darkutils.feat.performance;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.events.ReceiveMainThreadPacketEvent;
import gg.darkutils.events.base.CancellationState;
import gg.darkutils.events.base.EventRegistry;
import gg.darkutils.utils.TickUtils;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.network.packet.s2c.play.PlaySoundFromEntityS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

public final class SoundLagFix {
    @NotNull
    private static final Object2IntOpenHashMap<SoundLagFix.SoundData> limitData = new Object2IntOpenHashMap<>(128);

    static {
        SoundLagFix.limitData.defaultReturnValue(0);
    }

    private static final int limitPerTick = 1;

    private SoundLagFix() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final void init() {
        TickUtils.queueRepeatingTickTask(SoundLagFix::reset, 1);
        EventRegistry.centralRegistry().addListener(SoundLagFix::onReceivePacket);
    }

    private static final void reset() {
        // Limits are enforced per tick
        SoundLagFix.limitData.clear();
    }

    private static final boolean isEnabled() {
        return DarkUtilsConfig.INSTANCE.soundLagFix;
    }

    private static final void onReceivePacket(@NotNull final ReceiveMainThreadPacketEvent event) {
        if (!SoundLagFix.isEnabled()) {
            return;
        }

        final var p = event.packet();

        switch (p) {
            // Only rate limit server received sounds as we trust the sounds played by client itself to be fair and not lag.
            case final PlaySoundS2CPacket packet ->
                    SoundLagFix.rateLimitServerSound(event.cancellationState(), SoundLagFix.SoundData.from(packet));

            case final PlaySoundFromEntityS2CPacket packet ->
                    SoundLagFix.rateLimitServerSound(event.cancellationState(), SoundLagFix.SoundData.from(packet));

            default -> {
                // do nothing
            }
        }
    }

    private static final void rateLimitServerSound(@NotNull final CancellationState cancellationState, @NotNull final SoundLagFix.SoundData soundData) {
        final var newValue = SoundLagFix.limitData.merge(soundData, 1, Integer::sum);

        if (SoundLagFix.limitPerTick < newValue) {
            // Log this case
            //DarkUtils.warn(SoundLagFix.class, "Skipping playing a sound due to per-tick rate-limit of {} being exceeded to {}. Sound data: {}", limit, newValue, soundData);

            // Cancel the sound from playing
            cancellationState.cancel();
        }
    }

    private sealed interface SoundData permits SoundLagFix.LocationOriginatingSoundData, SoundLagFix.EntityOriginatingSoundData {
        @NotNull
        private static SoundLagFix.SoundData from(@NotNull final PlaySoundS2CPacket packet) {
            // Intentionally does not capture .getSeed() to the record as it's a random 64-bit long created for every packet, would make all sound data objects return a different hashCode and equals and our rate-limit would never work with it.
            return SoundLagFix.SoundData.debug(new SoundLagFix.LocationOriginatingSoundData(packet.getX(), packet.getY(), packet.getZ(), packet.getVolume(), packet.getPitch(), packet.getCategory(), packet.getSound().value().id()));
        }

        @NotNull
        private static SoundLagFix.SoundData from(@NotNull final PlaySoundFromEntityS2CPacket packet) {
            // Intentionally does not capture .getSeed() to the record as it's a random 64-bit long created for every packet, would make all sound data objects return a different hashCode and equals and our rate-limit would never work with it.
            return SoundLagFix.SoundData.debug(new SoundLagFix.EntityOriginatingSoundData(packet.getEntityId(), packet.getVolume(), packet.getPitch(), packet.getCategory(), packet.getSound().value().id()));
        }

        @NotNull
        private static SoundLagFix.SoundData debug(@NotNull final SoundLagFix.SoundData soundData) {
            //DarkUtils.info(SoundLagFix.class, "Received a sound. Sound data: {}", soundData);

            return soundData;
        }
    }

    private record LocationOriginatingSoundData(double x, double y, double z, float volume, float pitch,
                                                @NotNull SoundCategory soundCategory,
                                                @NotNull Identifier sound,
                                                int cachedHashCode) implements SoundLagFix.SoundData {

        private LocationOriginatingSoundData(double x, double y, double z, float volume, float pitch,
                                             @NotNull SoundCategory soundCategory,
                                             @NotNull Identifier sound) {
            this(x, y, z, volume, pitch, soundCategory, sound, SoundLagFix.LocationOriginatingSoundData.computeHash(x, y, z, volume, pitch, soundCategory, sound));
        }

        @Override
        public final int hashCode() {
            return this.cachedHashCode;
        }

        private static final int computeHash(double x, double y, double z, float volume, float pitch,
                                             @NotNull SoundCategory soundCategory,
                                             @NotNull Identifier sound) {
            var result = Double.hashCode(x);
            result = 31 * result + Double.hashCode(y);
            result = 31 * result + Double.hashCode(z);
            result = 31 * result + Float.hashCode(volume);
            result = 31 * result + Float.hashCode(pitch);
            result = 31 * result + soundCategory.hashCode();
            result = 31 * result + sound.hashCode();
            return result;
        }
    }

    private record EntityOriginatingSoundData(int entityId, float volume, float pitch,
                                              @NotNull SoundCategory soundCategory,
                                              @NotNull Identifier sound,
                                              int cachedHashCode) implements SoundLagFix.SoundData {

        private EntityOriginatingSoundData(int entityId, float volume, float pitch,
                                           @NotNull SoundCategory soundCategory,
                                           @NotNull Identifier sound) {
            this(entityId, volume, pitch, soundCategory, sound, SoundLagFix.EntityOriginatingSoundData.computeHash(entityId, volume, pitch, soundCategory, sound));
        }

        @Override
        public final int hashCode() {
            return this.cachedHashCode;
        }

        private static final int computeHash(int entityId, float volume, float pitch,
                                              @NotNull SoundCategory soundCategory,
                                              @NotNull Identifier sound) {
            var result = Integer.hashCode(entityId);
            result = 31 * result + Float.hashCode(volume);
            result = 31 * result + Float.hashCode(pitch);
            result = 31 * result + soundCategory.hashCode();
            result = 31 * result + sound.hashCode();
            return result;
        }
    }
}
