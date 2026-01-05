package gg.darkutils.events.base.impl;

import gg.darkutils.events.base.Event;
import gg.darkutils.events.base.EventHandler;
import gg.darkutils.events.base.EventRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A basic {@link EventRegistry}.
 */
public final class BasicEventRegistry implements EventRegistry {
    /**
     * Singleton instance.
     */
    @NotNull
    private static final BasicEventRegistry INSTANCE = new BasicEventRegistry();

    /**
     * Holds the map of known events to their handlers in a thread-safe manner (immutable map copy each time one is registered).
     */
    @NotNull
    private final AtomicReference<Map<Class<? extends Event>, EventHandler<? extends Event>>> knownEvents = new AtomicReference<>(Map.of());

    /**
     * Creates the singleton {@link BasicEventRegistry} instance.
     */
    private BasicEventRegistry() {
        super();
    }

    /**
     * Gets the singleton {@link BasicEventRegistry} instance.
     *
     * @return The singleton {@link BasicEventRegistry} instance.
     */
    @NotNull
    public static final BasicEventRegistry getInstance() {
        return BasicEventRegistry.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    private static final <T extends Event> EventHandler<T> uncheckedCastToEventHandler(@NotNull final EventHandler<?> eventHandler) {
        return (EventHandler<T>) eventHandler;
    }

    @Override
    public final <T extends Event> void registerEvent(@NotNull final Class<T> event) {
        this.registerEvent(event, new BasicEventHandler<>(event));
    }

    @Override
    public final <T extends Event> void registerEvent(@NotNull final Class<T> event, @NotNull final EventHandler<T> handler) {
        this.knownEvents.updateAndGet(oldMap -> {
            if (oldMap.containsKey(event)) {
                throw new IllegalStateException("event " + event.getName() + " is already registered");
            }

            final var newMap = new IdentityHashMap<>(oldMap);
            newMap.put(event, handler);
            return Map.copyOf(newMap);
        });
    }

    @Override
    @NotNull
    public final <T extends Event> EventHandler<T> getEventHandler(@NotNull final Class<T> event) {
        final var eventHandler = this.knownEvents.get().get(event);

        if (null == eventHandler) {
            throw new IllegalStateException("event " + event.getName() + " is not a known registered event for the requested registry, ensure its registered in the static initializer block properly in your event class!");
        }

        return BasicEventRegistry.uncheckedCastToEventHandler(eventHandler);
    }

    @Override
    public final String toString() {
        return "BasicEventRegistry{" +
                "knownEvents=" + this.knownEvents +
                '}';
    }
}
