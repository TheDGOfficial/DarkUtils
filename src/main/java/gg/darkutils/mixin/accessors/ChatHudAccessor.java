package gg.darkutils.mixin.accessors;

import net.minecraft.client.gui.hud.ChatHud;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@FunctionalInterface
@Mixin(ChatHud.class)
public interface ChatHudAccessor {
    @Invoker
    int callGetWidth();
}
