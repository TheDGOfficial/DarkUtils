package gg.darkutils.feat.dungeons;

import gg.darkutils.config.DarkUtilsConfig;
import gg.darkutils.utils.LocationUtils;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.text.TranslatableTextContent;
import org.jetbrains.annotations.NotNull;

public final class AutoCloseSecretChests {
    private AutoCloseSecretChests() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    public static final boolean shouldCancelOpen(@NotNull final OpenScreenS2CPacket packet) {
        if (!DarkUtilsConfig.INSTANCE.autoCloseSecretChests || !LocationUtils.isInDungeons()) {
            return false;
        }

        final var handlerType = packet.getScreenHandlerType();

        if (ScreenHandlerType.GENERIC_9X3 == handlerType || ScreenHandlerType.GENERIC_9X6 == handlerType) {
            final var name = packet.getName();
            if (name.getContent() instanceof final TranslatableTextContent translatable) {
                final var key = translatable.getKey();

                return "container.chest".equals(key) || "container.chestDouble".equals(key);
            }
        }

        return false;
    }
}
