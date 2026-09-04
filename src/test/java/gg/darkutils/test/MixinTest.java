package gg.darkutils.test;

import gg.darkutils.mixinbase.DarkUtilsMixinPlugin;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;

// Adapted to Java from https://github.com/hannibal002/SkyHanni/blob/5bbf2d1cc23a58553d7602a29d1a90f4eb94e3d7/src/test/java/at/hannibal2/skyhanni/test/MixinTest.kt#L9

/**
 * Audits mixins to ensure their validity without launching a full Minecraft client.
 * Implementation inspired by [Skyblocker](https://github.com/SkyblockerMod/Skyblocker).
 */
final class MixinTest {
    private static final String MIXIN_PACKAGE_PATH = "gg/darkutils/mixin";

    @Test
    final void mixinsLoadSuccessfully() {
        final var environment = MixinEnvironment.getCurrentEnvironment();
        Assertions.assertInstanceOf(IMixinTransformer.class, environment.getActiveTransformer());
        environment.audit();
    }

    @Test
    final void mixinDiscoveryIsSuccessful() {
        final var classLoader = getClass().getClassLoader();
        final var discovered = new DarkUtilsMixinPlugin().getMixins();
        Assertions.assertTrue(!discovered.isEmpty(), () ->
            "Mixin discovery returned nothing, so this test would pass without inspecting a single mixin. " +
            "DarkUtilsMixinPlugin resolves them relative to its own code source, " +
            "which the mixinTest classpath has to expose."
        );
        discovered.forEach(mixin -> {
            String path = String.format("%s/%s.class", MixinTest.MIXIN_PACKAGE_PATH, mixin.replace('.', '/'));
            if (classLoader.getResource(path) == null) {
                throw new NullPointerException(
                    "Mixin " + mixin + " was discovered but " + path + " is not on the classpath"
                );
            }
        });
    }
}
