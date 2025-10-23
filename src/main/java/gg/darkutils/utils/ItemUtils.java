package gg.darkutils.utils;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public final class ItemUtils {
    private ItemUtils() {
        super();

        throw new UnsupportedOperationException("static utility class");
    }

    private static final @NotNull List<String> getLoreLines(@NotNull final ItemStack stack) {
        final var lore = stack.getComponents().get(DataComponentTypes.LORE);

        if (null != lore) {
            final var lines = new ObjectArrayList<String>();

            for (final var line : lore.lines()) {
                lines.add(line.getString());
            }

            return Collections.unmodifiableList(lines);
        }

        return Collections.emptyList();
    }

    @Nullable
    private static final String getRightClickAbility(@NotNull final ItemStack stack) {
        for (final var line : ItemUtils.getLoreLines(stack)) {
            if (line.contains("Ability: ") && line.endsWith("RIGHT CLICK")) {
                return line;
            }
        }

        return null;
    }

    public static final boolean hasRightClickAbility(@NotNull final ItemStack stack) {
        return null != ItemUtils.getRightClickAbility(stack);
    }
}
