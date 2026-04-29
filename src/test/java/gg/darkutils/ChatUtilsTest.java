package gg.darkutils;

import gg.darkutils.utils.chat.ChatUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class ChatUtilsTest {
    @Test
    final void removeControlCodes_emptyString_returnsEmpty() {
        Assertions.assertEquals("", ChatUtils.removeControlCodes(""), "Empty string should remain empty");
    }

    @Test
    final void removeControlCodes_noControlCodes_returnsSameInstance() {
        final var text = "Hello world";

        Assertions.assertSame(text, ChatUtils.removeControlCodes(text), "String without control codes should return same instance");
    }

    @Test
    final void removeControlCodes_singleCode_removed() {
        final var input = "Hello §aWorld";

        Assertions.assertEquals("Hello World", ChatUtils.removeControlCodes(input), "Single control code should be removed");
    }

    @Test
    final void removeControlCodes_multipleCodes_removed() {
        final var input = "§aHello §bWorld§r!";

        Assertions.assertEquals("Hello World!", ChatUtils.removeControlCodes(input), "Multiple control codes should be removed");
    }

    @Test
    final void removeControlCodes_trailingControlCode_doesNotThrow() {
        final var input = "Hello§";

        Assertions.assertEquals("Hello", ChatUtils.removeControlCodes(input), "Trailing control code should be safely ignored");
    }
}