package gg.darkutils.events.base.impl;

import gg.darkutils.events.base.Event;
import gg.darkutils.events.base.EventHandler;
import gg.darkutils.events.base.EventRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;

/**
 * A basic {@link EventRegistry}.
 */
public final class BasicEventRegistry implements EventRegistry {
    @NotNull
    private static final BasicEventRegistry INSTANCE = new BasicEventRegistry();
    @NotNull
    private final ConcurrentHashMap<Class<? extends Event>, EventHandler<? extends Event>> knownEvents = new ConcurrentHashMap<>();

    private BasicEventRegistry() {
        super();
    }

    @NotNull
    public static final BasicEventRegistry getInstance() {
        return BasicEventRegistry.INSTANCE;
    }

    @Override
    public final <T extends Event> void registerEvent(@NotNull final Class<T> event) {
        this.registerEvent(event, new BasicEventHandler<>());
    }

    @Override
    public final <T extends Event> void registerEvent(@NotNull final Class<T> event, @NotNull final EventHandler<T> handler) {
        this.knownEvents.put(event, handler);
    }

    @Override
    @SuppressWarnings("unchecked")
    @NotNull
    public final <T extends Event> EventHandler<T> getEventHandler(@NotNull final Class<T> event) {
        final var eventHandler = this.knownEvents.get(event);

        if (null == eventHandler) {
            throw new IllegalStateException("event " + event.getName() + " is not a known registered event for the requested registry, ensure its registered in the static initializer block properly in your event class!");
        }

        return (EventHandler<T>) eventHandler;
    }
}
