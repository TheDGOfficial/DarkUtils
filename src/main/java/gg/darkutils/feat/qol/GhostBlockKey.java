package gg.darkutils.feat.qol;

import gg.darkutils.DarkUtils;
import gg.darkutils.config.DarkUtilsConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.Set;

public final class GhostBlockKey {
    /**
     * The keybinding for the trigger.
     */
    @NotNull
    private static final KeyBinding KEYBIND = KeyBindingHelper.registerKeyBinding(
            new KeyBinding(
                    "Create Ghost Block",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_G,
                    KeyBinding.Category.create(Identifier.of(DarkUtils.MOD_ID, "main"))
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

    private static final void onTick(@NotNull final MinecraftClient client) {
        if (!DarkUtilsConfig.INSTANCE.ghostBlockKey) {
            return;
        }

        while (GhostBlockKey.KEYBIND.wasPressed()) {
            GhostBlockKey.tryCreateGhostBlock(client);
        }
    }

    private static final void tryCreateGhostBlock(@NotNull final MinecraftClient client) {
        // What the player is currently looking at
        final var hit = client.crosshairTarget;

        // Obviously we can't ghost block entities/players or thin air, so check for block hit result
        if (hit instanceof final BlockHitResult blockHit) {
            final var world = client.world;

            // This should not happen but is a safeguard
            if (null == world) {
                return;
            }

            final var pos = blockHit.getBlockPos();

            final var state = world.getBlockState(pos);
            final var targetBlock = state.getBlock();

            // Do not ghost buttons or other hardcoded blacklisted blocks
            if (state.isIn(BlockTags.BUTTONS) || GhostBlockKey.BLACKLIST.contains(targetBlock)) {
                return;
            }

            // Replace block with air *client side only*
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
        }
    }
}
