package gg.darkutils.feat.performance;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.mixin.accessors.LivingEntityAccessor;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class ArmorStandOptimizer {
    private static final @NotNull ReferenceArrayList<ArmorStandEntity> reusableStands = new ReferenceArrayList<>(512);
    private static final @NotNull ReferenceArrayList<ArmorStandEntity> loadedArmorStands = new ReferenceArrayList<>(1024);
    private static final @NotNull ReferenceOpenHashSet<ArmorStandEntity> pendingRemovals = new ReferenceOpenHashSet<>(512);

    private ArmorStandOptimizer() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final void init() {
        // Save armor stands on spawn/despawn to avoid having to iterate over all entities each tick
        ClientEntityEvents.ENTITY_LOAD.register(ArmorStandOptimizer::onEntityJoinWorld);
        ClientEntityEvents.ENTITY_UNLOAD.register(ArmorStandOptimizer::onEntityLeaveWorld);

        // Perform cleanup on world change to ensure no memory leaks happen
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register(ArmorStandOptimizer::onWorldChange);

        // Run refresh every client tick
        ClientTickEvents.END_CLIENT_TICK.register(ArmorStandOptimizer::refreshArmorStands);
    }

    private static final boolean isEnabled() {
        return DarkUtilsConfig.INSTANCE.armorStandOptimizer;
    }

    private static final int getLimit() {
        return DarkUtilsConfig.INSTANCE.armorStandLimit;
    }

    private static final void clearState() {
        ArmorStandOptimizer.reusableStands.clear();

        for (final var armorStand : ArmorStandOptimizer.loadedArmorStands) {
            ArmorStandOptimizer.setShouldSkipRender(armorStand, false);
        }
    }

    private static final void onEntityJoinWorld(@NotNull final Entity entity, @NotNull final ClientWorld world) {
        // No config check - we always need to track armor stands in case user enables the feature while some armor stands are already in the world.
        // No load event would be called for those, which in turn bugs the state.

        if (entity instanceof final ArmorStandEntity armorStand) {
            ArmorStandOptimizer.loadedArmorStands.add(armorStand);
        }
    }

    private static final void onEntityLeaveWorld(@NotNull final Entity entity, @NotNull final ClientWorld world) {
        // No config check - we always need to track armor stands in case user enables the feature while some armor stands are already in the world.
        // No load event would be called for those, which in turn bugs the state.

        if (entity instanceof final ArmorStandEntity armorStand) {
            ArmorStandOptimizer.pendingRemovals.add(armorStand);
        }
    }

    private static final void onWorldChange(@NotNull final MinecraftClient client, @Nullable final ClientWorld world) {
        // No config check - we always need to track armor stands in case user enables the feature while some armor stands are already in the world.
        // No load event would be called for those, which in turn bugs the state.

        // Removes if:
        // 1- World is not loaded
        // 2- Entity with the same id no longer exists in the world
        // 3- Entity with the same id exists in the world, but with a different instance (ID collision)
        ArmorStandOptimizer.loadedArmorStands.removeIf(armorStand -> !ArmorStandOptimizer.isLoaded(armorStand));
    }

    private static final boolean isLoaded(@NotNull final ArmorStandEntity armorStand) {
        Objects.requireNonNull(armorStand, "armorStand");

        final var world = MinecraftClient.getInstance().world;
        return null != world && armorStand == world.getEntityById(armorStand.getId());
    }

    private static final boolean isBlankTemporaryStand(@NotNull final ArmorStandEntity armorStand) {
        return 10 > armorStand.age && null == armorStand.getCustomName() && ArmorStandOptimizer.isInventoryEmpty(armorStand);
    }

    private static final void refreshArmorStands(@NotNull final MinecraftClient client) {
        // No config check - we always need to track armor stands in case user enables the feature while some armor stands are already in the world.
        // No load event would be called for those, which in turn bugs the state.
        if (!ArmorStandOptimizer.pendingRemovals.isEmpty()) {
            ArmorStandOptimizer.loadedArmorStands.removeAll(ArmorStandOptimizer.pendingRemovals);
            ArmorStandOptimizer.pendingRemovals.clear();
        }

        if (!ArmorStandOptimizer.isEnabled()) {
            ArmorStandOptimizer.clearState();
            return;
        }

        final var world = client.world;
        final var player = client.player;

        if (null == world || null == player) {
            ArmorStandOptimizer.clearState();
            return;
        }

        if (ArmorStandOptimizer.loadedArmorStands.isEmpty()) {
            return;
        }

        for (final var armorStand : ArmorStandOptimizer.loadedArmorStands) {
            ArmorStandOptimizer.setShouldSkipRender(armorStand, true);
        }

        final var limit = ArmorStandOptimizer.getLimit();

        if (0 == limit) {
            // Avoid unnecessary work when the limit is zero.
            return;
        }

        final var loadedSize = ArmorStandOptimizer.loadedArmorStands.size();

        if (loadedSize <= limit) {
            for (final var armorStand : ArmorStandOptimizer.loadedArmorStands) {
                ArmorStandOptimizer.setShouldSkipRender(armorStand, ArmorStandOptimizer.isBlankTemporaryStand(armorStand));
            }
            return;
        }

        // Collect loaded armor stands into the list
        ArmorStandOptimizer.reusableStands.addAll(ArmorStandOptimizer.loadedArmorStands);

        // Select only closest LIMIT stands
        ArmorStandOptimizer.selectClosest(ArmorStandOptimizer.reusableStands, loadedSize, limit, player);

        for (var i = 0; limit > i; ++i) {
            final var armorStand = ArmorStandOptimizer.reusableStands.get(i);
            ArmorStandOptimizer.setShouldSkipRender(armorStand, ArmorStandOptimizer.isBlankTemporaryStand(armorStand));
        }

        ArmorStandOptimizer.reusableStands.clear();
    }

    private static final boolean isInventoryEmpty(@NotNull final ArmorStandEntity armorStand) {
        return ((LivingEntityAccessor) armorStand).getEquipment().isEmpty();
    }

    private static final void setShouldSkipRender(@NotNull final ArmorStandEntity armorStand, final boolean shouldSkipRender) {
        ((ArmorStandCustomRenderState) armorStand).darkutils$setShouldSkipRender(shouldSkipRender);
    }

    public static final boolean shouldNotSkipRenderArmorStand(@NotNull final ArmorStandEntity armorStand) {
        return !ArmorStandOptimizer.isEnabled() || !((ArmorStandCustomRenderState) armorStand).darkutils$shouldSkipRender();
    }

    /**
     * Performs partial selection using QuickSelect.
     */
    private static final void selectClosest(@NotNull final ReferenceArrayList<ArmorStandEntity> list, final int listSize, final int closestCount, @NotNull final ClientPlayerEntity player) {
        final var playerX = player.getX();
        final var playerY = player.getY();
        final var playerZ = player.getZ();

        var left = 0;
        var right = listSize - 1;

        while (left <= right) {
            final var pivotIndex = ArmorStandOptimizer.partition(list, left, right, playerX, playerY, playerZ);
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

    private static final double distanceSquaredToStand(final double originX, final double originY, final double originZ, @NotNull final ArmorStandEntity entity) {
        final var deltaX = originX - entity.getX();
        final var deltaY = originY - entity.getY();
        final var deltaZ = originZ - entity.getZ();

        return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
    }

    private static final int medianOfThreeIndex(@NotNull final ReferenceArrayList<ArmorStandEntity> list, final int left, final int right, final double playerX, final double playerY, final double playerZ) {
        final var middleIndex = left + right >>> 1;

        final var leftDistance = ArmorStandOptimizer.distanceSquaredToStand(playerX, playerY, playerZ, list.get(left));
        final var middleDistance = ArmorStandOptimizer.distanceSquaredToStand(playerX, playerY, playerZ, list.get(middleIndex));
        final var rightDistance = ArmorStandOptimizer.distanceSquaredToStand(playerX, playerY, playerZ, list.get(right));

        return leftDistance < middleDistance
                ? middleDistance < rightDistance
                ? middleIndex
                : leftDistance < rightDistance ? right : left
                : leftDistance < rightDistance
                ? left
                : middleDistance < rightDistance ? right : middleIndex;
    }

    private static final int partition(@NotNull final ReferenceArrayList<ArmorStandEntity> list, final int left, final int right, final double playerX, final double playerY, final double playerZ) {
        final var pivotIdx = ArmorStandOptimizer.medianOfThreeIndex(list, left, right, playerX, playerY, playerZ);

        final var pivot = list.get(pivotIdx);
        list.set(pivotIdx, list.get(right));
        list.set(right, pivot);

        final var pivotDistance = ArmorStandOptimizer.distanceSquaredToStand(playerX, playerY, playerZ, pivot);

        var i = left;

        for (var j = left; right > j; ++j) {
            final var element = list.get(j);
            if (pivotDistance > ArmorStandOptimizer.distanceSquaredToStand(playerX, playerY, playerZ, element)) {
                list.set(j, list.set(i, element));
                ++i;
            }
        }

        list.set(right, list.set(i, pivot));
        return i;
    }
}
