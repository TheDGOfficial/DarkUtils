package gg.darkutils.feat.qol;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.events.ItemUseEvent;
import gg.darkutils.events.base.EventListener;
import gg.darkutils.events.base.EventPriority;
import gg.darkutils.events.base.EventRegistry;
import net.minecraft.item.Items;
import org.jetbrains.annotations.NotNull;

public final class DisableCellsAlignment {
    private DisableCellsAlignment() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final void init() {
        EventRegistry.centralRegistry().addListener(EventListener.create(DisableCellsAlignment::onItemUse, EventPriority.ABOVE_NORMAL, false));
    }

    private static final void onItemUse(@NotNull final ItemUseEvent event) {
        if (!DarkUtilsConfig.INSTANCE.disableCellsAlignment) {
            return;
        }

        final var itemStack = event.itemStack();

        if (itemStack.isOf(Items.BLAZE_ROD)) {
            final var customNameText = itemStack.getCustomName();
            if (null != customNameText) {
                final var customName = customNameText.getString();
                if ("Gyrokinetic Wand".equals(customName)) {
                    event.cancellationState().cancel();
                }
            }
        }
    }
}
