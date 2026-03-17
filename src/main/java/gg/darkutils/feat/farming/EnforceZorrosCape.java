package gg.darkutils.feat.farming;

import gg.darkutils.DarkUtils;
import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.data.PersistentData;
import gg.darkutils.events.SlotClickEvent;
import gg.darkutils.events.base.EventRegistry;
import gg.darkutils.utils.TickUtils;
import gg.darkutils.utils.chat.ChatUtils;
import org.jetbrains.annotations.NotNull;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;

public final class EnforceZorrosCape {
    private EnforceZorrosCape() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final void init() {
        TickUtils.queueRepeatingTickTask(EnforceZorrosCape::onTick, 1);
        EventRegistry.centralRegistry().addListener(EnforceZorrosCape::onSlotClick);
    }

    private static final boolean hasEquipped() {
        return PersistentData.INSTANCE.zorrosCapeEquipped;
    }

    private static final void setEquipped(final boolean equipped) {
        PersistentData.INSTANCE.zorrosCapeEquipped = equipped;
    }

    private static final void onTick() {
        // No config check, still need to track zorro's cape equip status in case user enables/disables the feature to not desync the state from the real state.
        final var client = MinecraftClient.getInstance();
        final var screen = client.currentScreen;

        if (!(screen instanceof final HandledScreen<?> handled)) {
            return;
        }

        final var title = ChatUtils.removeControlCodes(handled.getTitle().getString());

        if (!"Your Equipment and Stats".equals(title)) {
            return;
        }

        final var handler = handled.getScreenHandler();
        final var slots = handler.slots;
        final var upperSize = slots.size() - 36;

        for (var i = 0; i < upperSize; ++i) {
            final var slot = slots.get(i);
            final var stack = slot.getStack();

            if (stack.isEmpty()) {
                continue;
            }

            final var name = ChatUtils.removeControlCodes(stack.getName().getString());

            if (name.endsWith("Zorro's Cape")) {
                EnforceZorrosCape.setEquipped(true);
                return;
            }
        }

        EnforceZorrosCape.setEquipped(false);
    }

    private static final void onSlotClick(@NotNull final SlotClickEvent event) {
        if (!DarkUtilsConfig.INSTANCE.enforceZorrosCape) {
            return;
        }

        final var slot = event.slot();

        final var client = MinecraftClient.getInstance();
        final var screen = client.currentScreen;

        if (!(screen instanceof final HandledScreen<?> handled)) {
            return;
        }

        final var title = ChatUtils.removeControlCodes(handled.getTitle().getString());

        if (!"Your Contests".equals(title)) {
            return;
        }

        final var stack = slot.getStack();

        if (stack.isEmpty()) {
            return;
        }

        final var name = ChatUtils.removeControlCodes(stack.getName().getString());

        if ("Bulk Claim".equals(name)) {
            if (!EnforceZorrosCape.hasEquipped()) {
                EnforceZorrosCape.notifyPlayer();

                event.cancellationState().cancel();
                return;
            }
        }

        final var lore = stack.get(DataComponentTypes.LORE);

        if (null == lore) {
            return;
        }

        for (final var line : lore.lines()) {
            final var clean = ChatUtils.removeControlCodes(line.getString());

            if ("Click to claim reward!".equals(clean)) {
                if (!EnforceZorrosCape.hasEquipped()) {
                    EnforceZorrosCape.notifyPlayer();

                    event.cancellationState().cancel();
                    return;
                }
            }
        }
    }

    private static final void notifyPlayer() {
        DarkUtils.user("Equip Zorro's Cape before claiming rewards!", DarkUtils.UserMessageLevel.USER_ERROR);
    }
}
