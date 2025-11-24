package gg.darkutils.utils;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class ItemUtils {
    private ItemUtils() {
        super();

        throw new UnsupportedOperationException("static utility class");
    }

    private static final @NotNull List<Text> getLoreLines(@NotNull final ItemStack stack) {
        final var lore = stack.getComponents().get(DataComponentTypes.LORE);

        return null == lore ? List.of() : lore.lines();
    }

    @Nullable
    private static final String getRightClickAbility(@NotNull final ItemStack stack) {
        for (final var line : ItemUtils.getLoreLines(stack)) {
            final var plain = line.getString();

            if (plain.contains("Ability: ") && plain.endsWith("RIGHT CLICK")) {
                return plain;
            }
        }

        return null;
    }

    public static final boolean hasRightClickAbility(@NotNull final ItemStack stack) {
        return null != ItemUtils.getRightClickAbility(stack);
    }
}
