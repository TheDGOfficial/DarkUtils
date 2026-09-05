package gg.darkutils.mixinbase;

import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;

import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Adapted from https://raw.githubusercontent.com/hannibal002/SkyHanni/refs/heads/beta/src/main/java/at/hannibal2/skyhanni/mixins/init/SkyHanniMixinPlugin.java
 */
public final class DarkUtilsMixinPlugin implements IMixinConfigPlugin {
    private static final @NonNull String MIXIN_BASE_DIR = "gg/darkutils/mixin/";
    private static final @NonNull List<@NonNull String> mixins = DarkUtilsMixinPlugin.findMixins();

    public DarkUtilsMixinPlugin() {
        super();
    }

    @Override
    public final boolean shouldApplyMixin(@NonNull final String targetClassName, @NonNull final String mixinClassName) {
        return false;
    }

    private static final @NonNull URL baseUrl(@NonNull final URL classUrl) {
        final var string = classUrl.toString();
        if ("jar".equals(classUrl.getProtocol())) {
            try {
                return new URI(string.substring(4, string.lastIndexOf('!'))).toURL();
            } catch (final URISyntaxException | MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        if (string.endsWith(".class")) {
            try {
                return new URI(string.replace("\\", "/")
                    .replace(DarkUtilsMixinPlugin.class.getCanonicalName().replace(".", "/") + ".class", ""))
                    .toURL();
            } catch (final URISyntaxException | MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        return classUrl;
    }

    private static final void tryAddMixinClass(@NonNull final String className, @NonNull final List<@NonNull String> mixins) {
        final var norm = (className.endsWith(".class") ? className.substring(0, className.length() - ".class".length()) : className)
            .replace("\\", "/");
        if (norm.startsWith(DarkUtilsMixinPlugin.MIXIN_BASE_DIR) && !norm.endsWith("/")) {
            mixins.add(norm.substring(DarkUtilsMixinPlugin.MIXIN_BASE_DIR.length()));
        }
    }

    private static final void walkDir(@NonNull final Path file, @NonNull final List<@NonNull String> mixins) {
        System.out.println("Trying to find mixins from directory");
        try (final var classes = Files.walk(file.resolve(DarkUtilsMixinPlugin.MIXIN_BASE_DIR))) {
            classes.filter(Files::isRegularFile)
                .map(it -> file.relativize(it).toString())
                .forEach(it -> DarkUtilsMixinPlugin.tryAddMixinClass(it, mixins));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static final void walkJar(@NonNull final Path file, @NonNull final List<@NonNull String> mixins) {
        System.out.println("Trying to find mixins from jar file");
        try (final var zis = new ZipInputStream(Files.newInputStream(file))) {
            ZipEntry next;
            while ((next = zis.getNextEntry()) != null) {
                DarkUtilsMixinPlugin.tryAddMixinClass(next.getName(), mixins);
                zis.closeEntry();
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static final @NonNull List<@NonNull String> findMixins() {
        System.out.println("Trying to discover mixins");
        final var discoveredMixins = new ArrayList<String>();
        final var classUrl = DarkUtilsMixinPlugin.class.getProtectionDomain().getCodeSource().getLocation();
        System.out.println("Found classes at " + classUrl);
        final Path file;
        try {
            file = Paths.get(DarkUtilsMixinPlugin.baseUrl(classUrl).toURI());
        } catch (final URISyntaxException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Base directory found at " + file);
        if (Files.isDirectory(file)) {
            DarkUtilsMixinPlugin.walkDir(file, discoveredMixins);
        } else {
            DarkUtilsMixinPlugin.walkJar(file, discoveredMixins);
        }
        System.out.println("Found mixins (" + discoveredMixins.size() + "): " + discoveredMixins);

        return List.copyOf(discoveredMixins);
    }

    @Override
    public final @NonNull List<@NonNull String> getMixins() {
        return DarkUtilsMixinPlugin.mixins;
    }
}
