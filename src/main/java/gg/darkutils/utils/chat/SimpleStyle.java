package gg.darkutils.utils.chat;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.text.Style;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * A simple wrapper system around Minecraft's {@link Style}.
 * Provides composable style definitions that start from {@link Style#EMPTY}.
 */
public sealed interface SimpleStyle permits SimpleStyle.InheritedStyle, SimpleStyle.ColoredStyle, SimpleStyle.FormattedStyle, SimpleStyle.CenteredStyle, SimpleStyle.CompositeStyle {
    // === Static factory methods ===

    static @NotNull SimpleStyle.InheritedStyle inherited() {
        return SimpleStyle.InheritedStyle.INSTANCE;
    }

    static @NotNull SimpleStyle.ColoredStyle colored(@NotNull final SimpleColor color) {
        return SimpleStyle.colored(color.toRgb());
    }

    static @NotNull SimpleStyle.ColoredStyle colored(final int rgb) {
        return new SimpleStyle.ColoredStyle(rgb);
    }

    static @NotNull SimpleStyle.FormattedStyle formatted(final @NotNull SimpleFormatting simpleFormatting) {
        return new SimpleStyle.FormattedStyle(Objects.requireNonNull(simpleFormatting, "simpleFormatting"));
    }

    static @NotNull SimpleStyle.CenteredStyle centered() {
        return SimpleStyle.CenteredStyle.INSTANCE;
    }

    default boolean isCentered() {
        return this instanceof SimpleStyle.CenteredStyle || this instanceof SimpleStyle.CompositeStyle(
                final var styles
        ) && styles.contains(SimpleStyle.CenteredStyle.INSTANCE);
    }

    default boolean isInheritedStyle() {
        return this instanceof SimpleStyle.InheritedStyle;
    }

    /**
     * Combine two {@link SimpleStyle}s. This will merge them while preventing duplicates.
     */
    default @NotNull SimpleStyle also(final @NotNull SimpleStyle other) {
        if (this instanceof SimpleStyle.InheritedStyle || other instanceof SimpleStyle.InheritedStyle) {
            throw new IllegalArgumentException("inherited style can't be customized");
        }

        Objects.requireNonNull(other, "other");
        if (this.equals(other)) {
            return this; // avoid duplicates
        }

        // Flatten both styles into a list
        final var list = new ObjectArrayList<SimpleStyle>();
        if (this instanceof SimpleStyle.CompositeStyle(final var styles)) {
            list.addAll(styles);
        } else {
            list.add(this);
        }

        if (other instanceof SimpleStyle.CompositeStyle(final var styles)) {
            list.addAll(styles);
        } else {
            list.add(other);
        }

        // remove duplicates by type (ColoredStyle, FormattedStyle, CenteredStyle)
        final var deduped = list.stream()
                .distinct()
                .toList();

        return 1 == deduped.size() ? deduped.getFirst() : new SimpleStyle.CompositeStyle(deduped);
    }

    /**
     * Applies this style onto an existing {@link Style}.
     */
    @NotNull Style applyStyle(@NotNull Style style);

    /**
     * Converts this to a full Minecraft {@link Style}, starting from {@link Style#EMPTY}.
     */
    default @NotNull Style toStyle() {
        var finalStyle = Style.EMPTY;
        if (this instanceof SimpleStyle.CompositeStyle(final var styles)) {
            for (final var style : styles) {
                finalStyle = style.applyStyle(finalStyle);
            }
        } else {
            return this.applyStyle(finalStyle);
        }
        return finalStyle;
    }

    // === Implementations ===

    record ColoredStyle(int rgb) implements SimpleStyle {
        @Override
        public final @NotNull Style applyStyle(final @NotNull Style style) {
            return style.withColor(this.rgb);
        }
    }

    record FormattedStyle(@NotNull SimpleFormatting simpleFormatting) implements SimpleStyle {
        public FormattedStyle {
            Objects.requireNonNull(simpleFormatting, "simpleFormatting");
        }

        @Override
        public final @NotNull Style applyStyle(final @NotNull Style style) {
            return style.withFormatting(this.simpleFormatting.toFormatting());
        }
    }

    /**
     * Marker style that signals text should be rendered with inherited style.
     * Does not modify the Minecraft {@link Style} itself.
     */
    public final class InheritedStyle implements SimpleStyle {
        @NotNull
        private static final SimpleStyle.InheritedStyle INSTANCE = new SimpleStyle.InheritedStyle();

        private InheritedStyle() {
            super();
        }

        @Override
        public @NotNull Style applyStyle(final @NotNull Style style) {
            return style; // no change
        }
    }

    /**
     * Marker style that signals text should be rendered centered.
     * Does not modify the Minecraft {@link Style} itself.
     */
    public final class CenteredStyle implements SimpleStyle {
        @NotNull
        private static final SimpleStyle.CenteredStyle INSTANCE = new SimpleStyle.CenteredStyle();

        private CenteredStyle() {
            super();
        }

        @Override
        public @NotNull Style applyStyle(final @NotNull Style style) {
            return style; // no change
        }
    }

    /**
     * A composite of multiple distinct {@link SimpleStyle}s.
     *
     * @param styles The styles.
     */
    record CompositeStyle(@NotNull List<@NotNull SimpleStyle> styles) implements SimpleStyle {
        public CompositeStyle {
            Objects.requireNonNull(styles, "styles");
            if (styles.isEmpty()) {
                throw new IllegalArgumentException("CompositeStyle cannot be empty");
            }
            if (styles.stream().distinct().count() != styles.size()) {
                throw new IllegalArgumentException("Duplicate styles are not allowed");
            }
        }

        @Override
        public final @NotNull Style applyStyle(final @NotNull Style style) {
            var current = style;
            for (final var otherStyles : this.styles) {
                current = otherStyles.applyStyle(current);
            }
            return current;
        }
    }
}
