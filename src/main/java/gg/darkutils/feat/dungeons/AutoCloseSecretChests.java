package gg.darkutils.feat.dungeons;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.events.OpenScreenEvent;
import gg.darkutils.events.base.EventRegistry;
import gg.darkutils.utils.LocationUtils;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.jetbrains.annotations.NotNull;

public final class AutoCloseSecretChests {
    private AutoCloseSecretChests() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final void init() {
        EventRegistry.centralRegistry().addListener(AutoCloseSecretChests::onOpenScreen);
    }

    private static final void onOpenScreen(@NotNull final OpenScreenEvent event) {
        if (!DarkUtilsConfig.INSTANCE.autoCloseSecretChests || !LocationUtils.isInDungeons()) {
            return;
        }

        final var handlerType = event.screenHandlerType();

        if (MenuType.GENERIC_9x3 == handlerType || MenuType.GENERIC_9x6 == handlerType) {
            final var name = event.name();
            if (name.getContents() instanceof final TranslatableContents translatable) {
                final var key = translatable.getKey();

                if ("container.chest".equals(key) || "container.chestDouble".equals(key)) {
                    event.cancellationState().cancel();
                }
            }
        }
    }
}
