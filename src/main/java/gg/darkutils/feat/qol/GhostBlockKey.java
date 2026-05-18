package gg.darkutils.feat.qol;

import com.mojang.blaze3d.platform.InputConstants;
import gg.darkutils.DarkUtils;
import gg.darkutils.config.DarkUtilsConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.Set;

public final class GhostBlockKey {
    /**
     * The keybinding for the trigger.
     */
    @NotNull
    private static final KeyMapping KEYBIND = KeyMappingHelper.registerKeyMapping(
            new KeyMapping(
                    "key.darkutils.createGhostBlock",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_G,
                    KeyMapping.Category.register(Identifier.fromNamespaceAndPath(DarkUtils.MOD_ID, "main"))
            )
    );

    /**
     * Blacklist of blocks that should *not* be ghosted.
     */
    @NotNull
    private static final Set<Block> BLACKLIST = Set.of(
            Blocks.CHEST,
            Blocks.TRAPPED_CHEST,
            Blocks.LEVER,
            Blocks.SKELETON_SKULL,
            Blocks.WITHER_SKELETON_SKULL,
            Blocks.CREEPER_HEAD,
            Blocks.DRAGON_HEAD,
            Blocks.PLAYER_HEAD,
            Blocks.ZOMBIE_HEAD,
            Blocks.PIGLIN_HEAD,
            Blocks.BEDROCK,
            Blocks.BARRIER
    );

    private GhostBlockKey() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final void init() {
        ClientTickEvents.END_CLIENT_TICK.register(GhostBlockKey::onTick);
    }

    private static final void onTick(@NotNull final Minecraft client) {
        if (!DarkUtilsConfig.INSTANCE.ghostBlockKey) {
            return;
        }

        while (GhostBlockKey.KEYBIND.consumeClick()) {
            GhostBlockKey.tryCreateGhostBlock(client);
        }
    }

    private static final void tryCreateGhostBlock(@NotNull final Minecraft client) {
        // What the player is currently looking at
        final var hit = client.hitResult;

        // Obviously we can't ghost block entities/players or thin air, so check for block hit result
        if (hit instanceof final BlockHitResult blockHit) {
            final var world = client.level;

            // This should not happen but is a safeguard
            if (null == world) {
                return;
            }

            final var pos = blockHit.getBlockPos();

            final var state = world.getBlockState(pos);
            final var targetBlock = state.getBlock();

            // Do not ghost buttons or other hardcoded blacklisted blocks
            if (state.is(BlockTags.BUTTONS) || GhostBlockKey.BLACKLIST.contains(targetBlock)) {
                return;
            }

            // Replace block with air *client side only*
            world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }
}
