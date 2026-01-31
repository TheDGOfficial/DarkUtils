package gg.darkutils.events.base.impl;

import gg.darkutils.events.base.Event;
import gg.darkutils.events.base.EventHandler;
import gg.darkutils.events.base.EventRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.IdentityHashMap;
import java.util.Map;

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

            return new BasicEventHandler<>(eventClass);
        }
    };

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
    @Override
    @NotNull
    public final <T extends Event> EventHandler<T> getEventHandler(@NotNull final Class<T> event) {
        return (EventHandler<T>) BasicEventRegistry.handlers.get(event);
    }

    @Override
    public final String toString() {
        return "BasicEventRegistry{" +
                "handlers=" + BasicEventRegistry.handlers +
                '}';
    }
}
