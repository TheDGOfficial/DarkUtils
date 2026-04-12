package gg.darkutils.utils;

import gg.darkutils.utils.chat.ChatUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class ItemUtils {
    private ItemUtils() {
        super();

        throw new UnsupportedOperationException("static utility class");
    }

    private static final @NotNull List<Component> getLoreLines(@NotNull final ItemStack stack) {
        final var lore = stack.getComponents().get(DataComponents.LORE);

        return null == lore ? List.of() : lore.lines();
    }

    @Nullable
    private static final String getRightClickAbility(@NotNull final ItemStack stack) {
        for (final var line : ItemUtils.getLoreLines(stack)) {
            final var plain = ChatUtils.removeControlCodes(line.getString());

            if (!plain.contains("Ability: ")) {
                continue;
            }

            return plain.endsWith("RIGHT CLICK") ? plain : null;
        }

        return null;
    }

    public static final boolean hasRightClickAbility(@NotNull final ItemStack stack) {
        return null != ItemUtils.getRightClickAbility(stack);
    }
}
