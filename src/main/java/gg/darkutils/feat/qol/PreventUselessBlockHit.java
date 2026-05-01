package gg.darkutils.feat.qol;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.events.UseItemEvent;
import gg.darkutils.events.base.EventRegistry;
import gg.darkutils.utils.ItemUtils;
import gg.darkutils.utils.chat.ChatUtils;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
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
        if (!DarkUtilsConfig.INSTANCE.preventUselessBlockHit || InteractionHand.MAIN_HAND != event.hand()) {
            return;
        }

        final var itemStack = event.itemStack();

        if (itemStack.is(ItemTags.SWORDS) && !ItemUtils.hasRightClickAbility(itemStack)) {
            final var name = itemStack.getCustomName();
            if (null != name) {
                if (!ChatUtils.removeControlCodes(name.getString()).contains("Cactus Knife")) { // When in Farming Toolkit we need an exception to prevent being locked out of Shift+RC to open the menu.
                    event.cancellationState().cancel();
                }
            }
        }
    }
}
