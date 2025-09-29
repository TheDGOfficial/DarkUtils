package gg.darkutils.mixin.performance;

import com.mojang.authlib.yggdrasil.YggdrasilServicesKeyInfo;
import gg.darkutils.config.DarkUtilsConfig;
import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(YggdrasilServicesKeyInfo.class)
final class YggdrasilServicesKeyInfoMixin {
    /**
     * Tracks servers we've already warned about.
     */
    @Unique
    private static final @NotNull Set<String> warnedServers =
            Collections.newSetFromMap(new ConcurrentHashMap<>(1));

    private YggdrasilServicesKeyInfoMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    /**
     * Redirects LOGGER.error(Throwable) calls in validateProperty(Property)
     * to log only the first occurrence per server, avoiding stack trace spam.
     */
    @Redirect(
            method = "validateProperty",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/slf4j/Logger;error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V",
                    remap = false
            ),
            remap = false
    )
    private final void darkutils$preventSignatureLogSpamIfEnabled(@NotNull final Logger logger, @NotNull final String format, @NotNull final Object arg, @NotNull final Object arg2) {
        if (!DarkUtilsConfig.INSTANCE.optimizeExceptions) {
            // Vanilla behaviour if optimization is not enabled, print full stack trace of all errors every time they happen
            logger.error(format, arg, arg2);

            // Reset state if feature is turned off
            YggdrasilServicesKeyInfoMixin.warnedServers.clear();

            return;
        }

        // Try to resolve current server IP
        final var client = MinecraftClient.getInstance();
        final var serverData = client.getCurrentServerEntry();
        final var formatted = format.replace("{}", arg.toString());

        if (null == serverData) {
            // Singleplayer or no server IP available → fallback to logging normally
            logger.error("{}: {}", formatted, arg2 instanceof final Throwable t ? t.getMessage() : "");
            return;
        }

        final var serverIP = serverData.address;

        // Only log once per server
        if (YggdrasilServicesKeyInfoMixin.warnedServers.add(serverIP)) {
            logger.error("Signature error on server {}: {}: {} - repeating signature errors for this server in this game session will not be logged anymore.", serverIP, formatted, arg2 instanceof final Throwable t ? t.getMessage() : "");
        }
    }
}
