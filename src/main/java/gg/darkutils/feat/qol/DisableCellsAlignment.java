package gg.darkutils.feat.qol;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.events.UseItemEvent;
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
        EventRegistry.centralRegistry().addListener(DisableCellsAlignment::onUseItem, EventPriority.ABOVE_NORMAL, false);
    }

    private static final void onUseItem(@NotNull final UseItemEvent event) {
        if (!DarkUtilsConfig.INSTANCE.disableCellsAlignment) {
            return;
        }

        final var itemStack = event.itemStack();

        if (itemStack.isOf(Items.BLAZE_ROD)) {
            final var customNameText = itemStack.getCustomName();
            if (null != customNameText && "Gyrokinetic Wand".equals(customNameText.getString())) {
                event.cancellationState().cancel();
            }
        }
    }
}
