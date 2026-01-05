package gg.darkutils.mixin.performance;

import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.YggdrasilServicesKeyInfo;
import gg.darkutils.config.DarkUtilsConfig;
import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(YggdrasilServicesKeyInfo.class)
final class YggdrasilServicesKeyInfoMixin {
    /**
     * Tracks servers we've already warned about.
     */
    @Unique
    private static final @NotNull Set<String> darkutils$warnedServers =
            ConcurrentHashMap.newKeySet(1);

    private YggdrasilServicesKeyInfoMixin() {
        super();

        throw new UnsupportedOperationException("mixin class");
    }

    /**
     * If enabled, completely disables signature validation.
     * <p>
     * This avoids RSA verification methods from being called, improving performance.
     * Might also make unverified textures show, but that's not our concern if the user choose to enable, it's their concern.
     */
    @Inject(method = "validateProperty", at = @At("HEAD"), cancellable = true)
    private final void darkutils$preventSignatureVerificationIfEnabled(@NotNull final Property property, @NotNull final CallbackInfoReturnable<Boolean> cir) {
        if (DarkUtilsConfig.INSTANCE.disableSignatureVerification) {
            cir.setReturnValue(true);
        }
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
            // Vanilla behavior if optimization is not enabled, print full stack trace of all errors every time they happen
            logger.error(format, arg, arg2);

            // Reset state if feature is turned off
            YggdrasilServicesKeyInfoMixin.darkutils$warnedServers.clear();

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
        if (YggdrasilServicesKeyInfoMixin.darkutils$warnedServers.add(serverIP)) {
            logger.error("Signature error on server {}: {}: {} - repeating signature errors for this server in this game session will not be logged anymore.", serverIP, formatted, arg2 instanceof final Throwable t ? t.getMessage() : "");
        }
    }
}
