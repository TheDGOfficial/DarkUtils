package gg.darkutils.feat.farming;

import gg.darkutils.DarkUtils;
import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.data.PersistentData;
import gg.darkutils.events.SlotClickEvent;
import gg.darkutils.events.base.EventRegistry;
import gg.darkutils.utils.TickUtils;
import gg.darkutils.utils.chat.ChatUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.NotNull;

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
        // No config check, still need to track Zorro's cape equip status in case user enables/disables the feature to not desync the state from the real state.
        final AbstractContainerMenu screenHandler;

        if (!(Minecraft.getInstance().screen instanceof final ContainerScreen container) || MenuType.GENERIC_9x6 != (screenHandler = container.getMenu()).menuType || !"Your Equipment and Stats".equals(ChatUtils.removeControlCodes(container.getTitle().getString()))) {
            return;
        }

        { // new scope so that slots variable is not available for use outside the block
            final var slots = screenHandler.slots;

            if (90 == slots.size()) {
                final var stack = slots.get(19).getItem();

                if (!stack.isEmpty()) {
                    final var customName = stack.getCustomName();

                    if (null != customName && ChatUtils.removeControlCodes(customName.getString()).endsWith("Zorro's Cape")) {
                        EnforceZorrosCape.setEquipped(true);
                        return;
                    }
                }
            }
        }

        EnforceZorrosCape.setEquipped(false);
    }

    private static final void onSlotClick(@NotNull final SlotClickEvent event) {
        final var i = event.slotId();

        // 50 = index of gold block/bulk claim item
        // rest are possible indexes for contest items
        // out of radius items are glass pane or arrows
        // makes it short circuit and exit method early to avoid running more code later
        if (50 != i && !(10 <= i && 16 >= i ||
                19 <= i && 25 >= i ||
                28 <= i && 34 >= i ||
                37 <= i && 43 >= i)) {
            return;
        }

        final var handledScreen = event.handledScreen();
        final ItemStack stack;

        if (!DarkUtilsConfig.INSTANCE.enforceZorrosCape || MenuType.GENERIC_9x6 != handledScreen.getMenu().menuType || (stack = event.slot().getItem()).isEmpty() || !"Your Contests".equals(ChatUtils.removeControlCodes(handledScreen.getTitle().getString()))) {
            return;
        }

        if (stack.is(Items.GOLD_BLOCK)) {
            final var customName = stack.getCustomName();

            if (null == customName) {
                return;
            }

            if ("Bulk Claim".equals(ChatUtils.removeControlCodes(customName.getString())) && !EnforceZorrosCape.hasEquipped()) {
                EnforceZorrosCape.notifyPlayer();
                event.cancellationState().cancel();
            }

            return;
        }

        if (stack.is(Items.ARROW)) {
            return;
        }

        final var lore = stack.get(DataComponents.LORE);

        if (null == lore) {
            return;
        }

        final var lines = lore.lines();

        if (lines.isEmpty()) {
            return;
        }

        if ("Click to claim reward!".equals(ChatUtils.removeControlCodes(lines.getLast().getString())) && !EnforceZorrosCape.hasEquipped()) {
            EnforceZorrosCape.notifyPlayer();
            event.cancellationState().cancel();
        }
    }

    private static final void notifyPlayer() {
        DarkUtils.user("Equip Zorro's Cape before claiming rewards!", DarkUtils.UserMessageLevel.USER_ERROR);
    }
}
