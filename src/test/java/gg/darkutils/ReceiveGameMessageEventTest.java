package gg.darkutils;

import gg.darkutils.events.ReceiveGameMessageEvent;
import gg.darkutils.events.base.Event;
import gg.darkutils.events.base.EventRegistry;
import gg.darkutils.events.base.EventListener;

import gg.darkutils.utils.chat.SimpleColor;
import gg.darkutils.utils.chat.SimpleStyle;
import gg.darkutils.utils.chat.SimpleFormatting;
import gg.darkutils.utils.chat.TextBuilder;

import net.minecraft.text.Text;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

final class ReceiveGameMessageEventTest {
    @BeforeEach
    void resetRegistry() {
        clearListeners(ReceiveGameMessageEvent.class);
    }

    @SuppressWarnings("unchecked")
    <T extends Event> void clearListeners(Class<T> event) {
        final var handler = EventRegistry.centralRegistry().getEventHandler(event);

        handler.getListeners().forEach((l) -> handler.removeListener((EventListener<T>) l));
    }

    @Test
    void testEventTriggerAndReceive() {
        final var counter = new AtomicInteger(0);

        EventRegistry.centralRegistry().addListener((ReceiveGameMessageEvent e) -> counter.incrementAndGet());

        new ReceiveGameMessageEvent(Text.literal("Hello")).trigger();

        Assertions.assertEquals(counter.get(), 1);
    }

    @Test
    void testContentAndRawContent() {
        EventRegistry.centralRegistry().addListener((ReceiveGameMessageEvent e) -> {
            Assertions.assertEquals(e.content(), "Hello");
            Assertions.assertEquals(e.rawContent(), "Hello");

            Assertions.assertTrue(e.matches("Hello"));

            Assertions.assertFalse(e.isStyledWith(SimpleColor.RED));
            Assertions.assertFalse(e.isStyledWith(SimpleColor.WHITE));
            Assertions.assertFalse(e.isStyledWith(SimpleColor.WHITE, SimpleFormatting.BOLD));
        });

        new ReceiveGameMessageEvent(Text.literal("Hello")).trigger();
    }

    @Test
    void testContentAndRawContentFormatted() {
        EventRegistry.centralRegistry().addListener((ReceiveGameMessageEvent e) -> {
            Assertions.assertEquals(e.content(), "Hello");
            Assertions.assertEquals(e.rawContent(), "Hello");

            Assertions.assertTrue(e.matches("Hello"));

            Assertions.assertTrue(e.isStyledWith(SimpleColor.RED));
            Assertions.assertFalse(e.isStyledWith(SimpleColor.WHITE));
            Assertions.assertFalse(e.isStyledWith(SimpleColor.WHITE, SimpleFormatting.BOLD));
        });

        new ReceiveGameMessageEvent(TextBuilder
                .withInitial("Hello", SimpleStyle.colored(SimpleColor.RED))
                .build()
        ).trigger();
    }

    @Test
    void testContentAndRawContentLegacyFormatted() {
        EventRegistry.centralRegistry().addListener((ReceiveGameMessageEvent e) -> {
            Assertions.assertEquals(e.content(), "Hello");
            Assertions.assertEquals(e.rawContent(), "§cHello");

            Assertions.assertTrue(e.matches("Hello"));

            Assertions.assertTrue(e.isStyledWith(SimpleColor.RED));
            Assertions.assertFalse(e.isStyledWith(SimpleColor.WHITE));
            Assertions.assertFalse(e.isStyledWith(SimpleColor.WHITE, SimpleFormatting.BOLD));
        });

        new ReceiveGameMessageEvent(Text.literal("§cHello")).trigger();
    }

    @Test
    void testExtractPart() {
        EventRegistry.centralRegistry().addListener((ReceiveGameMessageEvent e) -> {
            Assertions.assertEquals(e.extractPart("[Player] ", ':'), "John Doe");
        });

        new ReceiveGameMessageEvent(Text.literal("[Player] John Doe: Hello")).trigger();
    }

    @Test
    void testMatch() {
        final var counter = new AtomicInteger(0);

        EventRegistry.centralRegistry().addListener((ReceiveGameMessageEvent e) -> {
            e.match(Map.of(
                "Lorem ipsum dolor sit amet",
                (ev) -> counter.incrementAndGet()
            ));
        });

        new ReceiveGameMessageEvent(Text.literal("Lorem ipsum dolor sit amet")).trigger();

        Assertions.assertEquals(counter.get(), 1);
    }
}

