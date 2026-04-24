package gg.darkutils;

import gg.darkutils.events.ReceiveGameMessageEvent;
import gg.darkutils.events.base.EventRegistry;
import gg.darkutils.utils.chat.SimpleColor;
import gg.darkutils.utils.chat.SimpleFormatting;
import gg.darkutils.utils.chat.SimpleStyle;
import gg.darkutils.utils.chat.TextBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

final class ReceiveGameMessageEventTest {
    @BeforeEach
    final void resetRegistry() {
        EventRegistry.centralRegistry().getEventHandler(ReceiveGameMessageEvent.class).clearListeners();
    }

    @Test
    final void testEventTriggerAndReceive() {
        final var counter = new AtomicInteger(0);

        EventRegistry.centralRegistry().addListener((ReceiveGameMessageEvent e) -> counter.incrementAndGet());

        new ReceiveGameMessageEvent(TextBuilder.empty().append("Hello").build()).trigger();

        Assertions.assertEquals(1, counter.get(), "Listener should be invoked exactly once");
    }

    @Test
    final void testContentAndRawContent() {
        EventRegistry.centralRegistry().addListener((final ReceiveGameMessageEvent e) -> {
            Assertions.assertEquals("Hello", e.content(), "Content should match plain text");
            Assertions.assertEquals("Hello", e.rawContent(), "Raw content should match plain text");

            Assertions.assertTrue(e.matches("Hello"), "Event should match 'Hello'");

            Assertions.assertFalse(e.isStyledWith(SimpleColor.RED), "Should not be styled with RED");
            Assertions.assertFalse(e.isStyledWith(SimpleColor.WHITE), "Should not be styled with WHITE");
            Assertions.assertFalse(e.isStyledWith(SimpleColor.WHITE, SimpleFormatting.BOLD), "Should not be styled with WHITE and BOLD");
        });

        new ReceiveGameMessageEvent(TextBuilder.empty().append("Hello").build()).trigger();
    }

    @Test
    void testContentAndRawContentFormatted() {
        EventRegistry.centralRegistry().addListener((final ReceiveGameMessageEvent e) -> {
            Assertions.assertEquals("Hello", e.content(), "Formatted content should strip styling");
            Assertions.assertEquals("Hello", e.rawContent(), "Raw content should not include legacy codes");

            Assertions.assertTrue(e.matches("Hello"), "Event should match 'Hello'");

            Assertions.assertTrue(e.isStyledWith(SimpleColor.RED), "Should be styled with RED");
            Assertions.assertFalse(e.isStyledWith(SimpleColor.WHITE), "Should not be styled with WHITE");
            Assertions.assertFalse(e.isStyledWith(SimpleColor.WHITE, SimpleFormatting.BOLD), "Should not be styled with WHITE and BOLD");
        });

        new ReceiveGameMessageEvent(TextBuilder
                .withInitial("Hello", SimpleStyle.colored(SimpleColor.RED))
                .build()
        ).trigger();
    }

    @Test
    final void testContentAndRawContentLegacyFormatted() {
        EventRegistry.centralRegistry().addListener((final ReceiveGameMessageEvent e) -> {
            Assertions.assertEquals("Hello", e.content(), "Content should strip legacy formatting");
            Assertions.assertEquals("§cHello", e.rawContent(), "Raw content should retain legacy formatting");

            Assertions.assertTrue(e.matches("Hello"), "Event should match 'Hello'");

            Assertions.assertTrue(e.isStyledWith(SimpleColor.RED), "Should detect RED styling from legacy code");
            Assertions.assertFalse(e.isStyledWith(SimpleColor.WHITE), "Should not be styled with WHITE");
            Assertions.assertFalse(e.isStyledWith(SimpleColor.WHITE, SimpleFormatting.BOLD), "Should not be styled with WHITE and BOLD");
        });

        new ReceiveGameMessageEvent(TextBuilder.empty().append("§cHello").build()).trigger();
    }

    @Test
    final void testExtractPart() {
        EventRegistry.centralRegistry().addListener((final ReceiveGameMessageEvent e) ->
                Assertions.assertEquals("John Doe", e.extractPart("[Player] ", ':'), "Extracted player name should match"));

        new ReceiveGameMessageEvent(TextBuilder.empty().append("[Player] John Doe: Hello").build()).trigger();
    }

    @Test
    final void testMatch() {
        final var counter = new AtomicInteger(0);

        EventRegistry.centralRegistry().addListener((final ReceiveGameMessageEvent e) -> e.match(Map.of(
                "Lorem ipsum dolor sit amet",
                ev -> counter.incrementAndGet()
        )));

        new ReceiveGameMessageEvent(TextBuilder.empty().append("Lorem ipsum dolor sit amet").build()).trigger();

        Assertions.assertEquals(1, counter.get(), "Match handler should be invoked exactly once");
    }
}