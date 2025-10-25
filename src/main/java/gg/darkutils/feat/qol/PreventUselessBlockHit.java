package gg.darkutils.feat.qol;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.events.UseItemEvent;
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
        EventRegistry.centralRegistry().addListener(PreventUselessBlockHit::onUseItem);
    }

    private static final void onUseItem(@NotNull final UseItemEvent event) {
        if (!DarkUtilsConfig.INSTANCE.preventUselessBlockHit) {
            return;
        }

        final var itemStack = event.itemStack();

        if (itemStack.isIn(ItemTags.SWORDS) && !ItemUtils.hasRightClickAbility(itemStack)) {
            event.cancellationState().cancel();
        }
    }
}
