package gg.darkutils.feat.qol;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.events.ItemUseEvent;
import gg.darkutils.events.base.EventRegistry;
import gg.darkutils.utils.ItemUtils;
import net.minecraft.registry.tag.ItemTags;
import org.jetbrains.annotations.NotNull;

public final class PreventUselessBlockHit {
    private PreventUselessBlockHit() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final void init() {
        EventRegistry.centralRegistry().addListener(PreventUselessBlockHit::onItemUse);
    }

    private static final void onItemUse(@NotNull final ItemUseEvent event) {
        if (!DarkUtilsConfig.INSTANCE.preventUselessBlockHit) {
            return;
        }

        final var itemStack = event.itemStack();

        if (itemStack.isIn(ItemTags.SWORDS) && !ItemUtils.hasRightClickAbility(itemStack)) {
            event.cancellationState().cancel();
        }
    }
}
