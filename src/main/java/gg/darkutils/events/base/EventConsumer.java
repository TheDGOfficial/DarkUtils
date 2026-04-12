package gg.darkutils.events.base;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface EventConsumer<T extends Event> {
    abstract void accept(@NotNull final T event);
}
