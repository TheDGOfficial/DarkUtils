package gg.darkutils.mixinquirks;

import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.NotNull;

// This class allows us to hold our fields outside the mixin injected class.
// It is particularly relevant after fabric mixin 0.17.1, where static blocks are not converted to <clinit> injections anymore (when not using a lower compatibility mode).
// Sometimes you can do a manual @Inject on <clinit> at RETURN, but most of the time you need to assign a static final field inside the <clinit>,
// which is not possible with the @Inject method, as Java requires it to be done inside an actual static block.
// Removing the final modifier is an option; but is not ideal. Therefore the existence of this class and its inner classes.
public final class HolderFields {
    // Each one is a seperate inner-class so that accessing one does not initialize others, preventing eager initialization and memory usage from the static field.

    public static final class FormattingCache {
        public static final @NotNull Formatting @NotNull [] FORMATTING_VALUES = Formatting.values();
    }

    public static final class FirmamentValues {
        public static final @NotNull Identifier MOD_LIST_IDENTIFIER = Identifier.of("firmament", "mod_list");
    }

    public static final class ServerValues {
        public static final @NotNull Set<@NotNull String> WARNED_SERVERS = ConcurrentHashMap.newKeySet(1);
    }

    private HolderFields() {
        super();

        throw new UnsupportedOperationException("static utility class");
    }
}
