package gg.darkutils.feat.qol;

import gg.darkutils.DarkUtils;
import gg.darkutils.config.DarkUtilsConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.hit.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.Set;

public final class GhostBlockKey {
    /**
     * Blacklist of blocks that should *not* be ghosted
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
        final var keyBinding = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                        "Create Ghost Block",
                        InputUtil.Type.KEYSYM,
                        GLFW.GLFW_KEY_G,
                        DarkUtils.class.getSimpleName()
                )
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (DarkUtilsConfig.INSTANCE.ghostBlockKey) {
                while (keyBinding.wasPressed()) {
                    // What the player is currently looking at
                    final var hit = client.crosshairTarget;

                    if (hit instanceof final BlockHitResult blockHit) {
                        final var pos = blockHit.getBlockPos();
                        final var state = client.world.getBlockState(pos);
                        final var targetBlock = state.getBlock();

                        // Do not ghost blacklisted blocks
                        if (state.isIn(BlockTags.BUTTONS) || GhostBlockKey.BLACKLIST.contains(targetBlock)) {
                            return;
                        }

                        // Replace block with air *client side only*
                        client.world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
                    }
                }
            }
        });
    }
}
