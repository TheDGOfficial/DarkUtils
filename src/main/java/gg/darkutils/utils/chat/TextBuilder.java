package gg.darkutils.utils.chat;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

public final class TextBuilder {
    @NotNull
    private final MutableText text;

    private TextBuilder(@NotNull final String initialText, @NotNull final SimpleStyle initialTextStyle) {
        super();

        this.text = Text.literal(initialText).setStyle(initialTextStyle.toStyle());
    }

    @NotNull
    public static final TextBuilder withInitial(@NotNull final String initialText, @NotNull final SimpleStyle initialTextStyle) {
        return new TextBuilder(initialText, initialTextStyle);
    }

    @NotNull
    public final TextBuilder appendSpace() {
        return this.append(" ");
    }

    @NotNull
    public final TextBuilder appendNewLine() {
        return this.append(ChatUtils.NEW_LINE);
    }

    @NotNull
    public final TextBuilder appendGradientText(@NotNull final String startHex, @NotNull final String endHex, @NotNull final String text, @NotNull final SimpleStyle simpleStyle) {
        this.text.append(ChatUtils.gradient(startHex, endHex, simpleStyle.isCentered() ? ChatUtils.center(text, simpleStyle.toStyle().isBold()) : text));
        return this;
    }

    @NotNull
    public final TextBuilder appendGradientButton(@NotNull final String startHex, @NotNull final String endHex, @NotNull final ButtonData buttonData, @NotNull final SimpleStyle simpleStyle) {
        this.text.append(ChatUtils.button(startHex, endHex, buttonData.label(), buttonData.hover(), buttonData.command(), simpleStyle.isCentered(), simpleStyle.toStyle().isBold()));
        return this;
    }

    @NotNull
    public final TextBuilder append(@NotNull final String text) {
        this.text.append(text);
        return this;
    }

    @NotNull
    public final TextBuilder append(@NotNull final String text, @NotNull final SimpleStyle style) {
        final var actualStyle = style.toStyle();
        final var literal = Text.literal(style.isCentered() ? ChatUtils.center(text, actualStyle.isBold()) : text);

        if (!style.isInheritedStyle()) {
            literal.setStyle(actualStyle);
        }

        this.text.append(literal);
        return this;
    }

    @NotNull
    public final Text build() {
        return this.text;
    }
}
