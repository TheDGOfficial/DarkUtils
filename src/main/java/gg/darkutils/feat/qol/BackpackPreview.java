package gg.darkutils.feat.qol;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import gg.darkutils.utils.SkyblockProfileUtils;
import gg.darkutils.utils.chat.ChatUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

// Storage slot layout and previews adapted from Skyblocker (LGPL-3.0).
// https://github.com/SkyblockerMod/Skyblocker/blob/main/src/main/java/de/hysky/skyblocker/skyblock/item/tooltip/BackpackPreview.java
public final class BackpackPreview {
    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final Pattern ENDER_CHEST = Pattern.compile("Ender Chest.*\\((\\d{1,2})/\\d+\\)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern BACKPACK = Pattern.compile("Backpack.*\\(Slot #(\\d{1,2})\\)$", Pattern.CASE_INSENSITIVE);
    private static final int COLUMNS = 9;
    private static final int STORAGE_MENU = -1;
    private static final int PAGES = 27;
    private static final Preview[] pendingPages = new Preview[PAGES];

    private static @Nullable PreviewCache cache;
    private static @Nullable ContainerScreen storageScreen;
    private static @Nullable ContainerScreen readyScreen;
    private static int readyPage;

    private BackpackPreview() {
        throw new UnsupportedOperationException("static utility class");
    }

    public static final void reset() {
        storageScreen = null;
        invalidateProfile();
    }

    public static final void invalidateProfile() {
        cache = null;
        readyScreen = null;
        Arrays.fill(pendingPages, null);
    }

    public static final void loadProfile(@NotNull final String profileId, @NotNull final ClientPacketListener connection) {
        final var client = Minecraft.getInstance();
        final var directory = FabricLoader.getInstance().getConfigDir().resolve("darkutils/backpack-preview")
                .resolve(client.getUser().getProfileId().toString()).resolve(profileId);
        if (cache != null && readyPage != STORAGE_MENU) {
            readyScreen = null;
        }
        final var selected = new PreviewCache(directory, connection.registryAccess().createSerializationContext(NbtOps.INSTANCE));
        cache = selected;
        Disk.IO.execute(() -> {
            for (var page = 0; page < PAGES; ++page) {
                final var index = page;
                final var loaded = Disk.load(selected.file(index), selected.ops);
                client.execute(() -> {
                    if (cache == selected) {
                        selected.finishLoading(index, loaded);
                    }
                });
            }
        });
        for (var page = 0; page < PAGES; ++page) {
            if (pendingPages[page] != null) {
                selected.save(page, pendingPages[page]);
            }
        }
        Arrays.fill(pendingPages, null);
        if (readyScreen != null && readyPage == STORAGE_MENU) {
            removeUnavailablePages(readyScreen, selected);
        }
    }

    public static final void onOpen(@NotNull final ContainerScreen screen, @NotNull final ClientPacketListener connection) {
        if (isStorageMenu(titleOf(screen), screen.getMenu().getRowCount())) {
            // Slots can appear before the complete contents packet arrives.
            storageScreen = screen;
            SkyblockProfileUtils.refresh(connection);
        }
    }

    public static final void onContents(@NotNull final ContainerScreen screen, @NotNull final ClientPacketListener connection) {
        final var title = titleOf(screen);
        final var rows = screen.getMenu().getRowCount();
        final var page = pageFromTitle(title);
        if (page == STORAGE_MENU ? !isStorageMenu(title, rows) : rows < 2 || rows > 6) {
            return;
        }

        SkyblockProfileUtils.refresh(connection);
        readyScreen = screen;
        readyPage = page;
        if (page == STORAGE_MENU) {
            storageScreen = screen;
            if (cache != null) {
                removeUnavailablePages(screen, cache);
            }
        }
    }

    public static final void onClose(@NotNull final ContainerScreen screen) {
        if (screen == storageScreen) {
            storageScreen = null;
        }
        if (screen != readyScreen) {
            return;
        }
        readyScreen = null;
        if (readyPage == STORAGE_MENU) {
            if (cache != null) {
                removeUnavailablePages(screen, cache);
            }
            return;
        }

        final var menu = screen.getMenu();
        final var end = menu.getRowCount() * COLUMNS;
        final var items = new ArrayList<ItemStack>(end - COLUMNS);
        // The first row is Hypixel's navigation bar. The player inventory follows the chest rows.
        for (var slot = COLUMNS; slot < end; ++slot) {
            items.add(menu.getSlot(slot).getItem().copy());
        }
        final var preview = new Preview(ChatUtils.removeControlCodes(screen.getTitle().getString()), items);
        if (cache == null) {
            pendingPages[readyPage] = preview;
        } else {
            cache.save(readyPage, preview);
        }
    }

    public static final boolean extract(@NotNull final ContainerScreen screen, @Nullable final Slot slot,
                                        @NotNull final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        if (screen != storageScreen || slot == null
                || !screen.getMenu().getCarried().isEmpty()) {
            return false;
        }

        final var slotIndex = slot.index;
        final var page = slotIndex >= 9 && slotIndex < 18 ? slotIndex - 9
                : slotIndex >= 27 && slotIndex < 45 ? slotIndex - 18 : STORAGE_MENU;
        if (page == STORAGE_MENU || !slot.hasItem() || isUnavailable(page, slot)) {
            return false;
        }
        final var selected = cache;
        final var preview = selected == null ? pendingPages[page] : selected.pages[page];
        if (preview == null) {
            // Wait for the lookup or preload before falling back to the vanilla tooltip.
            return selected == null ? SkyblockProfileUtils.isResolving() : selected.isLoading(page);
        }
        renderPreview(preview, graphics, Minecraft.getInstance().font, mouseX, mouseY);
        return true;
    }

    private static final void renderPreview(@NotNull final Preview preview, @NotNull final GuiGraphicsExtractor graphics,
                                            @NotNull final Font font, final int mouseX, final int mouseY) {
        final var width = 176;
        final var items = preview.items();
        final var bodyHeight = 17 + items.size() / COLUMNS * 18;
        final var screenWidth = graphics.guiWidth();
        final var x = Math.clamp(mouseX + width + 8 <= screenWidth ? mouseX + 8 : mouseX - width - 8,
                0, Math.max(0, screenWidth - width));
        final var y = Math.clamp(mouseY - 16, 0, Math.max(0, graphics.guiHeight() - bodyHeight - 7));
        graphics.nextStratum();
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0, width, bodyHeight, 256, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y + bodyHeight, 0, 215, width, 7, 256, 256);
        graphics.text(font, preview.title(), x + 8, y + 6, 0xFF404040, false);
        for (var index = 0; index < items.size(); ++index) {
            final var item = items.get(index);
            if (!item.isEmpty()) {
                final var itemX = x + 8 + index % COLUMNS * 18;
                final var itemY = y + 18 + index / COLUMNS * 18;
                graphics.item(item, itemX, itemY);
                graphics.itemDecorations(font, item, itemX, itemY);
            }
        }
    }

    private static final int pageFromTitle(@NotNull final String title) {
        final var enderChest = ENDER_CHEST.matcher(title);
        if (enderChest.find()) {
            final var number = Integer.parseInt(enderChest.group(1));
            return number >= 1 && number <= 9 ? number - 1 : STORAGE_MENU;
        }
        final var backpack = BACKPACK.matcher(title);
        if (backpack.find()) {
            final var number = Integer.parseInt(backpack.group(1));
            return number >= 1 && number <= 18 ? number + 8 : STORAGE_MENU;
        }
        return STORAGE_MENU;
    }

    private static final @NotNull String titleOf(@NotNull final ContainerScreen screen) {
        return ChatUtils.removeControlCodes(screen.getTitle().getString()).strip();
    }

    private static final boolean isStorageMenu(@NotNull final String title, final int rows) {
        return rows >= 5 && title.equals("Storage");
    }

    private static final boolean isUnavailable(final int page, @NotNull final Slot slot) {
        return slot.getItem().is(page < 9 ? Items.STAINED_GLASS_PANE.red() : Items.STAINED_GLASS_PANE.brown());
    }

    private static final void removeUnavailablePages(@NotNull final ContainerScreen screen, @NotNull final PreviewCache selected) {
        for (var page = 0; page < PAGES; ++page) {
            final var slot = page < 9 ? page + 9 : page + 18;
            if (isUnavailable(page, screen.getMenu().getSlot(slot)) && (selected.isLoading(page) || selected.pages[page] != null)) {
                selected.remove(page);
            }
        }
    }

    private static final class PreviewCache {
        private final Path directory;
        private final RegistryOps<Tag> ops;
        private final Preview[] pages = new Preview[PAGES];
        private final BitSet knownPages = new BitSet(PAGES);

        private PreviewCache(@NotNull final Path directory, @NotNull final RegistryOps<Tag> ops) {
            this.directory = directory;
            this.ops = ops;
        }

        private final @NotNull Path file(final int page) {
            return this.directory.resolve(page + ".nbt");
        }

        private final boolean isLoading(final int page) {
            return !this.knownPages.get(page);
        }

        private final void finishLoading(final int page, @Nullable final Preview loaded) {
            if (this.isLoading(page)) {
                this.pages[page] = loaded;
                this.knownPages.set(page);
            }
        }

        private final void save(final int page, @NotNull final Preview preview) {
            this.pages[page] = preview;
            this.knownPages.set(page);
            Disk.IO.execute(() -> Disk.save(this.file(page), this.ops, preview));
        }

        private final void remove(final int page) {
            this.pages[page] = null;
            this.knownPages.set(page);
            Disk.IO.execute(() -> Disk.remove(this.file(page)));
        }
    }

    private static final class Disk {
        private static final ExecutorService IO = Executors.newSingleThreadExecutor(Thread.ofVirtual().name("DarkUtils storage previews").factory());
        private static final Logger LOGGER = LoggerFactory.getLogger(BackpackPreview.class);

        static {
            ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
                SkyblockProfileUtils.reset();
                IO.close();
            });
        }

        private static final @Nullable Preview load(@NotNull final Path file, @NotNull final RegistryOps<Tag> ops) {
            if (!Files.isRegularFile(file)) {
                return null;
            }
            try {
                final var data = NbtIo.readCompressed(file, NbtAccounter.create(32L * 1024 * 1024));
                return Preview.CODEC.parse(ops, data).getOrThrow();
            } catch (final IOException | RuntimeException e) {
                LOGGER.error("Failed to load storage preview {}", file, e);
                return null;
            }
        }

        private static final void remove(@NotNull final Path file) {
            try {
                Files.deleteIfExists(file);
            } catch (final IOException e) {
                LOGGER.error("Failed to remove storage preview {}", file, e);
            }
        }

        private static final void save(@NotNull final Path file, @NotNull final RegistryOps<Tag> ops, @NotNull final Preview preview) {
            try {
                final var data = (CompoundTag) Preview.CODEC.encodeStart(ops, preview).getOrThrow();
                final var directory = Files.createDirectories(file.getParent());
                final var temporary = Files.createTempFile(directory, "page-", ".tmp");
                try {
                    NbtIo.writeCompressed(data, temporary);
                    try {
                        Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                    } catch (final AtomicMoveNotSupportedException e) {
                        Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
                    }
                } finally {
                    Files.deleteIfExists(temporary);
                }
            } catch (final IOException | RuntimeException e) {
                LOGGER.error("Failed to save storage preview {}", file, e);
            }
        }
    }

    private record Preview(@NotNull String title, @NotNull List<ItemStack> items) {
        private static final Codec<Preview> CODEC = RecordCodecBuilder.<Preview>create(instance -> instance.group(
                Codec.STRING.fieldOf("title").forGetter(Preview::title),
                ItemStack.OPTIONAL_CODEC.listOf(9, 45).fieldOf("items").forGetter(Preview::items)
        ).apply(instance, Preview::new)).validate(preview -> preview.items().size() % COLUMNS == 0
                ? DataResult.success(preview)
                : DataResult.error(() -> "Storage preview must contain whole rows"));
    }
}
