package gg.darkutils.events.base.impl;

import gg.darkutils.events.base.Event;
import gg.darkutils.events.base.EventHandler;
import gg.darkutils.events.base.EventRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * An {@link EventRegistry} implementation.
 */
public final class EventRegistryImpl implements EventRegistry {
    /**
     * Singleton instance.
     */
    @NotNull
    private static final EventRegistryImpl INSTANCE = new EventRegistryImpl();

    /**
     * Holds the event handlers in a thread-safe manner (thread-safety provided by JDK ClassValue)
     * <p>
     * Static is safe since we use a singleton instance, but in future remove the static if multiple event registries.
     */
    @NotNull
    private static final ClassValue<EventHandler<? extends Event>> handlers = new ClassValue<>() {
        @Override
        @NotNull
        protected final EventHandler<? extends Event> computeValue(@NotNull final Class<?> type) {
            if (!Event.class.isAssignableFrom(type)) {
                throw new IllegalStateException(
                        "Class " + type.getName() + " does not extend Event"
                );
            }

            @SuppressWarnings("unchecked") // safe because we've already checked above
            final Class<? extends Event> eventClass = (Class<? extends Event>) type;

            return new EventHandlerImpl<>(eventClass);
        }
    };

    /**
     * Creates the singleton {@link EventRegistryImpl} instance.
     */
    private EventRegistryImpl() {
        super();
    }

    /**
     * Gets the singleton {@link EventRegistryImpl} instance.
     *
     * @return The singleton {@link EventRegistryImpl} instance.
     */
    @NotNull
    public static final EventRegistryImpl getInstance() {
        return EventRegistryImpl.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    @Override
    @NotNull
    public final <T extends Event> EventHandler<T> getEventHandler(@NotNull final Class<T> event) {
        return (EventHandler<T>) EventRegistryImpl.handlers.get(event);
    }

    @Override
    public final String toString() {
        return "EventRegistryImpl{" +
                "handlers=" + EventRegistryImpl.handlers +
                '}';
    }
}
