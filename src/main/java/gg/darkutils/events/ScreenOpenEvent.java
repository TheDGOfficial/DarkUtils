package gg.darkutils.events;

import gg.darkutils.events.base.CancellableEvent;
import gg.darkutils.events.base.CancellationState;
import gg.darkutils.events.base.EventRegistry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

public record ScreenOpenEvent(@NotNull CancellationState cancellationState,
                              @NotNull ScreenHandlerType<?> screenHandlerType,
                              @NotNull Text name) implements CancellableEvent {
    static {
        EventRegistry.centralRegistry().registerEvent(ScreenOpenEvent.class);
    }

    public ScreenOpenEvent(@NotNull final ScreenHandlerType<?> screenHandlerType, @NotNull final Text name) {
        this(CancellationState.ofFresh(), screenHandlerType, name);
    }

    @Override
    @NotNull
    public final CancellationState getCancellationState() {
        return this.cancellationState;
    }
}
