package gg.darkutils.feat.qol;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.events.UseItemEvent;
import gg.darkutils.events.base.EventPriority;
import gg.darkutils.events.base.EventRegistry;
import gg.darkutils.utils.Helpers;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;
import org.jetbrains.annotations.NotNull;

public final class DisableCellsAlignment {
    private DisableCellsAlignment() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final void init() {
        EventRegistry.centralRegistry().addListener(DisableCellsAlignment::onUseItem, EventPriority.ABOVE_NORMAL);
    }

    private static final void onUseItem(@NotNull final UseItemEvent event) {
        if (!DarkUtilsConfig.INSTANCE.disableCellsAlignment || InteractionHand.MAIN_HAND != event.hand()) {
            return;
        }

        final var itemStack = event.itemStack();

        if (itemStack.is(Items.BLAZE_ROD) && Helpers.isHoldingAGyrokineticWand()) {
            event.cancellationState().cancel();
        }
    }
}
