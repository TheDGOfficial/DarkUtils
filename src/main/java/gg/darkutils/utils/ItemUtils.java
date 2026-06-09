package gg.darkutils.utils;

import gg.darkutils.utils.chat.ChatUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class ItemUtils {
    private ItemUtils() {
        super();

        throw new UnsupportedOperationException("static utility class");
    }

    public static final boolean hasRightClickAbility(@NotNull final ItemStack stack) {
        final var lore = stack.getComponents().get(DataComponents.LORE);

        if (null == lore) {
            return false;
        }

        for (final var line : lore.lines()) {
            final var plain = ChatUtils.removeControlCodes(line.getString());

            if (!plain.contains("Ability: ")) {
                continue;
            }

            return plain.endsWith("RIGHT CLICK");
        }

        return false;
    }
}
