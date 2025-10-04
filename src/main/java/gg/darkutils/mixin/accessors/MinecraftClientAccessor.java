package gg.darkutils.mixin.accessors;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@FunctionalInterface
@Mixin(MinecraftClient.class)
public interface MinecraftClientAccessor {
    @Invoker
    void callDoItemUse();
}
