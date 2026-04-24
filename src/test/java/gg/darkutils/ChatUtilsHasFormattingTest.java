package gg.darkutils;

import gg.darkutils.utils.chat.ChatUtils;
import gg.darkutils.utils.chat.SimpleColor;
import gg.darkutils.utils.chat.SimpleFormatting;
import gg.darkutils.utils.chat.SimpleStyle;
import gg.darkutils.utils.chat.TextBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class ChatUtilsHasFormattingTest {
    @Test
    final void shouldDetectColorFormattingInsideComponent() {
        final var text = TextBuilder
                .withInitial("Hello", SimpleStyle.colored(SimpleColor.RED))
                .build();

        final var result = ChatUtils.hasFormatting(text, SimpleColor.RED);

        Assertions.assertTrue(result, "Should detect RED formatting");
    }

    @Test
    final void shouldNotDetectDifferentColorFormattingInsideComponent() {
        final var text = TextBuilder
                .withInitial("Hello", SimpleStyle.colored(SimpleColor.BLUE))
                .build();

        final var result = ChatUtils.hasFormatting(text, SimpleColor.RED);

        Assertions.assertFalse(result, "Should not detect RED when BLUE is applied");
    }

    @Test
    final void shouldDetectColorAndBoldFormattingInsideComponent() {
        final var style = SimpleStyle.colored(SimpleColor.GOLD).also(SimpleStyle.formatted(SimpleFormatting.BOLD));
        Assertions.assertTrue(ChatUtils.hasFormatting(TextBuilder.withInitial("Hello", style).build(), SimpleColor.GOLD, SimpleFormatting.BOLD), "Text should have had gold and bold formatting");
    }

    @Test
    final void shouldNotDetectFormattingIfBoldMissing() {
        final var text = TextBuilder
                .withInitial("Hello", SimpleStyle.colored(SimpleColor.GOLD))
                .build();

        final var result = ChatUtils.hasFormatting(
                text,
                SimpleColor.GOLD,
                SimpleFormatting.BOLD
        );

        Assertions.assertFalse(result, "Should not detect bold when it is missing");
    }

    @Test
    final void shouldDetectFormattingFromRawLegacyCodesFallback() {
        final var style = SimpleStyle.colored(SimpleColor.GREEN);

        final var legacyFormatted =
                style.getRawFormattingCharacters() + "Hello";

        final var text = TextBuilder.empty().append(legacyFormatted).build();

        final var result = ChatUtils.hasFormatting(text, SimpleColor.GREEN);

        Assertions.assertTrue(result, "Should detect formatting from legacy codes");
    }

    @Test
    final void shouldNotDetectFormattingWhenPlainTextProvided() {
        final var text = TextBuilder
                .withInitial("Hello", SimpleStyle.inherited())
                .build();

        final var result = ChatUtils.hasFormatting(
                text,
                SimpleColor.YELLOW
        );

        Assertions.assertFalse(result, "Plain text should not report formatting");
    }

    @Test
    final void shouldDetectFormattingInAppendedChildComponent() {
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

        Assertions.assertTrue(result, "Should detect formatting in child component");
    }

    @Test
    final void shouldDetectItalicFormattingInsideComponent() {
        final var text = TextBuilder
                .withInitial("Hello", SimpleStyle
                        .colored(SimpleColor.YELLOW)
                        .also(SimpleStyle.formatted(SimpleFormatting.ITALIC)))
                .build();

        final var result = ChatUtils.hasFormatting(
                text,
                SimpleColor.YELLOW,
                SimpleFormatting.ITALIC
        );

        Assertions.assertTrue(result, "Should detect italic formatting");
    }

    @Test
    final void shouldDetectUnderlineFormattingInsideComponent() {
        final var text = TextBuilder
                .withInitial("Hello", SimpleStyle
                        .colored(SimpleColor.AQUA)
                        .also(SimpleStyle.formatted(SimpleFormatting.UNDERLINE)))
                .build();

        final var result = ChatUtils.hasFormatting(
                text,
                SimpleColor.AQUA,
                SimpleFormatting.UNDERLINE
        );

        Assertions.assertTrue(result, "Should detect underline formatting");
    }

    @Test
    final void shouldDetectStrikethroughFormattingInsideComponent() {
        final var text = TextBuilder
                .withInitial("Hello", SimpleStyle
                        .colored(SimpleColor.GREEN)
                        .also(SimpleStyle.formatted(SimpleFormatting.STRIKETHROUGH)))
                .build();

        final var result = ChatUtils.hasFormatting(
                text,
                SimpleColor.GREEN,
                SimpleFormatting.STRIKETHROUGH
        );

        Assertions.assertTrue(result, "Should detect strikethrough formatting");
    }

    @Test
    final void shouldDetectObfuscatedFormattingInsideComponent() {
        final var text = TextBuilder
                .withInitial("Hello", SimpleStyle
                        .colored(SimpleColor.RED)
                        .also(SimpleStyle.formatted(SimpleFormatting.OBFUSCATED)))
                .build();

        final var result = ChatUtils.hasFormatting(
                text,
                SimpleColor.RED,
                SimpleFormatting.OBFUSCATED
        );

        Assertions.assertTrue(result, "Should detect obfuscated formatting");
    }

    @Test
    final void shouldNotDetectItalicIfMissing() {
        final var text = TextBuilder
                .withInitial("Hello", SimpleStyle.colored(SimpleColor.YELLOW))
                .build();

        final var result = ChatUtils.hasFormatting(
                text,
                SimpleColor.YELLOW,
                SimpleFormatting.ITALIC
        );

        Assertions.assertFalse(result, "Should not detect italic when missing");
    }

    @Test
    final void shouldDetectVanillaFormattingColorAgainstRgbConstructedStyle() {
        final var text = TextBuilder.withInitial("Hello", SimpleStyle.colored(SimpleColor.GREEN)).build();

        final var result = ChatUtils.hasFormatting(
                text,
                SimpleStyle.colored(SimpleColor.GREEN)
        );

        Assertions.assertTrue(result, "Should detect equivalent color formatting");
    }
}