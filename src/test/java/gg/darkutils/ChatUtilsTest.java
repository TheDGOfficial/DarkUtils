package gg.darkutils;

import gg.darkutils.utils.chat.ChatUtils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

final class ChatUtilsTest {
    @Test
    final void removeControlCodes_emptyString_returnsEmpty() {
        Assertions.assertEquals("", ChatUtils.removeControlCodes(""));
    }

    @Test
    final void removeControlCodes_noControlCodes_returnsSameInstance() {
        final var text = "Hello world";

        Assertions.assertSame(text, ChatUtils.removeControlCodes(text));
    }

    @Test
    final void removeControlCodes_singleCode_removed() {
        final var input = "Hello §aWorld";

        Assertions.assertEquals("Hello World", ChatUtils.removeControlCodes(input));
    }

    @Test
    final void removeControlCodes_multipleCodes_removed() {
        final var input = "§aHello §bWorld§r!";

        Assertions.assertEquals("Hello World!", ChatUtils.removeControlCodes(input));
    }

    @Test
    final void removeControlCodes_trailingControlCode_doesNotThrow() {
        final var input = "Hello§";

        Assertions.assertEquals("Hello", ChatUtils.removeControlCodes(input));
    }
}

