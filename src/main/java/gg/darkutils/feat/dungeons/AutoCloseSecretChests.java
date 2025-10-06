package gg.darkutils.feat.dungeons;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.events.ScreenOpenEvent;
import gg.darkutils.events.base.EventRegistry;
import gg.darkutils.utils.LocationUtils;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.text.TranslatableTextContent;
import org.jetbrains.annotations.NotNull;

public final class AutoCloseSecretChests {
    private AutoCloseSecretChests() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final void init() {
        EventRegistry.centralRegistry().addListener(AutoCloseSecretChests::onScreenOpen);
    }

    private static final void onScreenOpen(@NotNull final ScreenOpenEvent event) {
        if (!DarkUtilsConfig.INSTANCE.autoCloseSecretChests || !LocationUtils.isInDungeons()) {
            return;
        }

        final var handlerType = event.screenHandlerType();

        if (ScreenHandlerType.GENERIC_9X3 == handlerType || ScreenHandlerType.GENERIC_9X6 == handlerType) {
            final var name = event.name();
            if (name.getContent() instanceof final TranslatableTextContent translatable) {
                final var key = translatable.getKey();

                if ("container.chest".equals(key) || "container.chestDouble".equals(key)) {
                    event.cancellationState().cancel();
                }
            }
        }
    }
}
