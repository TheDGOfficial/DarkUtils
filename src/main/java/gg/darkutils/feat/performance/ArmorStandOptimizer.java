package gg.darkutils.feat.performance;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.events.RenderEntityEvent;
import gg.darkutils.events.base.EventRegistry;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Random;

public final class ArmorStandOptimizer {
    private static final @NotNull ObjectOpenHashSet<ArmorStandEntity> armorStandRenderSet = new ObjectOpenHashSet<>(128);
    private static final @NotNull ObjectArrayList<ArmorStandEntity> reusableStands = new ObjectArrayList<>(128);
    private static final @NotNull Random RANDOM = new Random();

    private ArmorStandOptimizer() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final void init() {
        // Run refresh every client tick
        ClientTickEvents.END_CLIENT_TICK.register(client -> ArmorStandOptimizer.refreshArmorStands());

        // Render cancellation logic
        EventRegistry.centralRegistry().addListener(ArmorStandOptimizer::onRenderEntity);
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
            // Partial selection: closest `limit` stands will be in the first `limit` positions
            ArmorStandOptimizer.selectClosest(ArmorStandOptimizer.reusableStands, limit, player);
            for (var i = 0; limit > i; ++i) {
                ArmorStandOptimizer.armorStandRenderSet.add(ArmorStandOptimizer.reusableStands.get(i));
            }
        }

        ArmorStandOptimizer.reusableStands.clear();
    }

    private static final void onRenderEntity(@NotNull final RenderEntityEvent event) {
        if (DarkUtilsConfig.INSTANCE.armorStandOptimizer && event.entity() instanceof final ArmorStandEntity armorStand && !ArmorStandOptimizer.armorStandRenderSet.contains(armorStand)) {
            event.cancellationState().cancel();
        }
    }

    /**
     * Performs partial selection using QuickSelect.
     */
    private static final void selectClosest(@NotNull final ObjectArrayList<ArmorStandEntity> list, final int closestCount, @NotNull final ClientPlayerEntity player) {
        var left = 0;
        var right = list.size() - 1;
        while (left <= right) {
            final var pivotIndex = ArmorStandOptimizer.partition(list, left, right, player);
            if (pivotIndex == closestCount) {
                return;
            }
            if (pivotIndex < closestCount) {
                left = pivotIndex + 1;
            } else {
                right = pivotIndex - 1;
            }
        }
    }

    private static final int partition(@NotNull final ObjectArrayList<ArmorStandEntity> list, final int left, final int right, @NotNull final ClientPlayerEntity player) {
        // Random pivot to avoid worst-case
        final var pivotIdx = left + ArmorStandOptimizer.RANDOM.nextInt(right - left + 1);
        final var pivot = list.get(pivotIdx);
        list.set(pivotIdx, list.get(right));
        list.set(right, pivot);

        final var pivotDist = player.squaredDistanceTo(pivot);
        var i = left;
        for (var j = left; j < right; ++j) {
            if (player.squaredDistanceTo(list.get(j)) < pivotDist) {
                list.set(j, list.set(i, list.get(j)));
                ++i;
            }
        }
        list.set(right, list.set(i, pivot));
        return i;
    }
}
