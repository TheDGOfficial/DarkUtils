package gg.darkutils.feat.performance;

import com.google.common.collect.Ordering;
import gg.darkutils.config.DarkUtilsConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;

public final class ArmorStandOptimizer {
    private static final @NotNull HashSet<ArmorStandEntity> armorStandRenderSet = HashSet.newHashSet(128);
    private static final @NotNull ArrayList<ArmorStandEntity> reusableStands = new ArrayList<>(128);

    private ArmorStandOptimizer() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final void init() {
        // Run refresh every client tick
        ClientTickEvents.END_CLIENT_TICK.register(client -> ArmorStandOptimizer.refreshArmorStands());
    }

    private static final void refreshArmorStands() {
        if (!DarkUtilsConfig.INSTANCE.armorStandOptimizer) {
            ArmorStandOptimizer.reusableStands.clear();
            ArmorStandOptimizer.armorStandRenderSet.clear();
            return;
        }

        final var client = MinecraftClient.getInstance();

        final var world = client.world;
        final var player = client.player;

        if (null == world || null == player) {
            ArmorStandOptimizer.reusableStands.clear();
            ArmorStandOptimizer.armorStandRenderSet.clear();
            return;
        }

        ArmorStandOptimizer.reusableStands.clear();
        ArmorStandOptimizer.armorStandRenderSet.clear();

        // Collect all armor stands
        for (final var entity : world.getEntities()) {
            if (entity instanceof final ArmorStandEntity stand) {
                ArmorStandOptimizer.reusableStands.add(stand);
            }
        }

        final var limit = DarkUtilsConfig.INSTANCE.armorStandLimit;

        // Keep only closest LIMIT stands
        if (ArmorStandOptimizer.reusableStands.size() <= limit) {
            ArmorStandOptimizer.armorStandRenderSet.addAll(ArmorStandOptimizer.reusableStands);
        } else {
            final var closest = Ordering
                    .from(Comparator.<Entity>comparingDouble(player::squaredDistanceTo))
                    .leastOf(ArmorStandOptimizer.reusableStands, limit);

            ArmorStandOptimizer.armorStandRenderSet.addAll(closest);
        }

        ArmorStandOptimizer.reusableStands.clear();
    }

    public static final boolean checkRender(@NotNull final ArmorStandEntity entity) {
        return ArmorStandOptimizer.armorStandRenderSet.contains(entity);
    }
}
