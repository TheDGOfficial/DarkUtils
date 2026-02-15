package gg.darkutils;

import gg.darkutils.utils.chat.ChatUtils;
import gg.darkutils.utils.chat.SimpleStyle;
import gg.darkutils.utils.chat.SimpleColor;
import gg.darkutils.utils.chat.SimpleFormatting;
import gg.darkutils.utils.chat.TextBuilder;

import net.minecraft.text.Text;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class ChatUtilsHasFormattingTest {
    @Test
    void shouldDetectColorFormattingInsideComponent() {
        final var style = SimpleStyle.colored(SimpleColor.RED);

        final var text = TextBuilder
                .withInitial("Hello", style)
                .build();

        final var result = ChatUtils.hasFormatting(text, SimpleColor.RED);

        Assertions.assertTrue(result);
    }

    @Test
    void shouldNotDetectDifferentColorFormattingInsideComponent() {
        final var text = TextBuilder
                .withInitial("Hello", SimpleStyle.colored(SimpleColor.BLUE))
                .build();

        final var result = ChatUtils.hasFormatting(text, SimpleColor.RED);

        Assertions.assertFalse(result);
    }

    @Test
    void shouldDetectColorAndBoldFormattingInsideComponent() {
        final var style = SimpleStyle
                .colored(SimpleColor.GOLD)
                .also(SimpleStyle.formatted(SimpleFormatting.BOLD));

        final var text = TextBuilder
                .withInitial("Hello", style)
                .build();

        final var result = ChatUtils.hasFormatting(
                text,
                SimpleColor.GOLD,
                SimpleFormatting.BOLD
        );

        Assertions.assertTrue(result);
    }

    @Test
    void shouldNotDetectFormattingIfBoldMissing() {
        final var text = TextBuilder
                .withInitial("Hello", SimpleStyle.colored(SimpleColor.GOLD))
                .build();

        final var result = ChatUtils.hasFormatting(
                text,
                SimpleColor.GOLD,
                SimpleFormatting.BOLD
        );

        Assertions.assertFalse(result);
    }

    @Test
    void shouldDetectFormattingFromRawLegacyCodesFallback() {
        final var style = SimpleStyle.colored(SimpleColor.GREEN);

        final var legacyFormatted =
                style.getRawFormattingCharacters() + "Hello";

        final var text = Text.literal(legacyFormatted);

        final var result = ChatUtils.hasFormatting(text, SimpleColor.GREEN);

        Assertions.assertTrue(result);
    }

    @Test
    void shouldNotDetectFormattingWhenPlainTextProvided() {
        final var text = TextBuilder
                .withInitial("Hello", SimpleStyle.inherited())
                .build();

        final var result = ChatUtils.hasFormatting(
                text,
                SimpleColor.YELLOW
        );

        Assertions.assertFalse(result);
    }

    @Test
    void shouldDetectFormattingInAppendedChildComponent() {
        final var style = SimpleStyle
                .colored(SimpleColor.AQUA)
                .also(SimpleStyle.formatted(SimpleFormatting.BOLD));

        final var text = TextBuilder
                .withInitial("Base", SimpleStyle.inherited())
                .append(" Child", style)
                .build();

        final var result = ChatUtils.hasFormatting(
                text,
                SimpleColor.AQUA,
                SimpleFormatting.BOLD
        );

        Assertions.assertTrue(result);
    }

    @Test
    void shouldDetectItalicFormattingInsideComponent() {
        final var style = SimpleStyle
                .colored(SimpleColor.YELLOW)
                .also(SimpleStyle.formatted(SimpleFormatting.ITALIC));

        final var text = TextBuilder
                .withInitial("Hello", style)
                .build();

        final var result = ChatUtils.hasFormatting(
                text,
                SimpleColor.YELLOW,
                SimpleFormatting.ITALIC
        );

        Assertions.assertTrue(result);
    }

    @Test
    void shouldDetectUnderlineFormattingInsideComponent() {
        final var style = SimpleStyle
                .colored(SimpleColor.AQUA)
                .also(SimpleStyle.formatted(SimpleFormatting.UNDERLINE));

        final var text = TextBuilder
                .withInitial("Hello", style)
                .build();

        final var result = ChatUtils.hasFormatting(
                text,
                SimpleColor.AQUA,
                SimpleFormatting.UNDERLINE
        );

        Assertions.assertTrue(result);
    }

    @Test
    void shouldDetectStrikethroughFormattingInsideComponent() {
        final var style = SimpleStyle
                .colored(SimpleColor.GREEN)
                .also(SimpleStyle.formatted(SimpleFormatting.STRIKETHROUGH));

        final var text = TextBuilder
                .withInitial("Hello", style)
                .build();

        final var result = ChatUtils.hasFormatting(
                text,
                SimpleColor.GREEN,
                SimpleFormatting.STRIKETHROUGH
        );

        Assertions.assertTrue(result);
    }

    @Test
    void shouldDetectObfuscatedFormattingInsideComponent() {
        final var style = SimpleStyle
                .colored(SimpleColor.RED)
                .also(SimpleStyle.formatted(SimpleFormatting.OBFUSCATED));

        final var text = TextBuilder
                .withInitial("Hello", style)
                .build();

        final var result = ChatUtils.hasFormatting(
                text,
                SimpleColor.RED,
                SimpleFormatting.OBFUSCATED
        );

        Assertions.assertTrue(result);
    }

    @Test
    void shouldNotDetectItalicIfMissing() {
        final var text = TextBuilder
                .withInitial("Hello", SimpleStyle.colored(SimpleColor.YELLOW))
                .build();

        final var result = ChatUtils.hasFormatting(
                text,
                SimpleColor.YELLOW,
                SimpleFormatting.ITALIC
        );

        Assertions.assertFalse(result);
    }
}

