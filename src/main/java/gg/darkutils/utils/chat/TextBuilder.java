package gg.darkutils.utils.chat;

import gg.darkutils.utils.chat.LinkData;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;
import java.net.URI;

/**
 * Non-thread safe builder for Text instances with helper methods.
 */
public final class TextBuilder {
    @NotNull
    private final MutableText text;

    private TextBuilder() {
        super();

        this.text = Text.empty();
    }

    private TextBuilder(@NotNull final String initialText, @NotNull final SimpleStyle initialTextStyle) {
        super();

        this.text = Text.literal(initialText).setStyle(initialTextStyle.toStyle());
    }

    @NotNull
    public static final TextBuilder empty() {
        return new TextBuilder();
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
    public final TextBuilder appendDoubleNewLine() {
        this.appendNewLine();

        return this.appendNewLine();
    }

    @NotNull
    public final TextBuilder appendGradientText(@NotNull final String startHex, @NotNull final String endHex, @NotNull final String text, @NotNull final SimpleStyle simpleStyle) {
        return this.appendGradientText(startHex, endHex, text, simpleStyle, null);
    }

    @NotNull
    public final TextBuilder appendGradientText(@NotNull final String startHex, @NotNull final String endHex, @NotNull final String text, @NotNull final SimpleStyle simpleStyle, @Nullable final LinkData link) {
        final var bold = simpleStyle.toStyle().isBold();
        final var gradientText = ChatUtils.gradient(startHex, endHex, simpleStyle.isCentered() ? ChatUtils.center(text, bold) : text);

        if (null != link) {
            TextBuilder.addGradientLink(startHex, endHex, gradientText, link, bold);
        }

        this.text.append(gradientText);
        return this;
    }

    @NotNull
    public final TextBuilder appendGradientButton(@NotNull final String startHex, @NotNull final String endHex, @NotNull final ButtonData buttonData, @NotNull final SimpleStyle simpleStyle) {
        this.text.append(ChatUtils.button(startHex, endHex, buttonData.label(), buttonData.hover(), buttonData.command(), simpleStyle.isCentered(), simpleStyle.toStyle().isBold()));
        return this;
    }

    @NotNull
    private final TextBuilder append(@NotNull final String text) {
        this.text.append(text);
        return this;
    }

    @NotNull
    public final TextBuilder append(@NotNull final String text, @NotNull final SimpleStyle style) {
        return this.append(text, style, null);
    }

    @NotNull
    public final TextBuilder append(@NotNull final String text, @NotNull final SimpleStyle style, @Nullable final LinkData link) {
        final var actualStyle = style.toStyle();
        final var literal = Text.literal(style.isCentered() ? ChatUtils.center(text, actualStyle.isBold()) : text);

        if (!style.isInheritedStyle()) {
            literal.setStyle(actualStyle);
        }

        if (null != link) {
            TextBuilder.addLink(literal, link);
        }

        this.text.append(literal);
        return this;
    }

    private static final void addGradientLink(@NotNull final String startHex, @NotNull final String endHex, @NotNull final MutableText text, @NotNull final LinkData link, final boolean bold) {
        TextBuilder.addLink(text, ChatUtils.gradient(startHex, endHex, link.hover(), bold), link.link());
    }

    private static final void addLink(@NotNull final MutableText text, @NotNull final LinkData link) {
        TextBuilder.addLink(text, Text.literal(link.hover()).setStyle(text.getStyle()), link.link());
    }

    private static final void addLink(@NotNull final MutableText text, @NotNull final MutableText hover, @NotNull final String link) {
        text.setStyle(
            text.getStyle()
                .withHoverEvent(new HoverEvent.ShowText(hover))
                .withClickEvent(new ClickEvent.OpenUrl(URI.create(link)))
        );
    }

    /**
     * Builds and obtains the Text.
     * <p>
     * The TextBuilder shall not be used again after this call
     * as it is unsafe because of modifying the underlying MutableText.
     *
     * @return The built text instance.
     */
    @NotNull
    public final Text build() {
        return this.text;
    }

    @Override
    public final String toString() {
        return "TextBuilder{" +
                "text=" + this.text +
                '}';
    }
}
