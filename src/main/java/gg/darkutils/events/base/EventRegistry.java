package gg.darkutils.events.base;

import gg.darkutils.events.base.impl.BasicEventRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandles;

/**
 * Defines a {@link EventRegistry}.
 */
public interface EventRegistry {
    /**
     * Returns a basic central {@link EventRegistry} that can be used to register events.
     *
     * @return A basic central {@link EventRegistry} that can be used to register events.
     */
    @NotNull
    static EventRegistry centralRegistry() {
        return BasicEventRegistry.getInstance();
    }

    @SuppressWarnings("unchecked")
    @NotNull
    private static <T extends Event> Class<T> uncheckedCastToEventClass(@NotNull final Class<?> clazz) {
        return (Class<T>) clazz;
    }

    @NotNull
    private static <T extends Event> Class<T> getEventClass(@NotNull final T event) {
        return EventRegistry.uncheckedCastToEventClass(event.getClass());
    }

    @NotNull
    private <T extends Event> EventHandler<T> getEventHandler(@NotNull final T event) {
        return this.getEventHandler(EventRegistry.getEventClass(event));
    }

    /**
     * Registers an {@link Event}.
     * <p>
     * An {@link Event} can't have listeners added before being registered.
     * <p>
     * You should call this method from your event class's static initializer block
     * to ensure it's always registered before listeners are added for it.
     * <p>
     * If the event is already registered, an {@link IllegalStateException} will be thrown.
     * <p>
     * A default {@link EventHandler} suitable for this {@link EventRegistry} will be created and used.
     *
     * @param event The event class.
     * @param <T>   The type of the event class.
     */
    <T extends Event> void registerEvent(@NotNull final Class<T> event);

    /**
     * Registers an {@link Event} with a custom {@link EventHandler} implementation.
     * <p>
     * An {@link Event} can't have listeners added before being registered.
     * <p>
     * You should call this method from your event class's static initializer block
     * to ensure it's always registered before listeners are added for it.
     * <p>
     * If the event is already registered, an {@link IllegalStateException} will be thrown.
     *
     * @param event   The event class.
     * @param handler The custom {@link EventHandler}.
     * @param <T>     The type of the event class.
     */
    <T extends Event> void registerEvent(@NotNull final Class<T> event, @NotNull final EventHandler<T> handler);

    /**
     * Gets the {@link EventHandler} for an event, allowing you to add listeners.
     *
     * @param event The event class.
     * @param <T>   The type of the event.
     * @return The {@link EventHandler} for the event.
     */
    @NotNull
    <T extends Event> EventHandler<T> getEventHandler(@NotNull final Class<T> event);

    /**
     * Adds the given listener to be run for the compiler inferred event type.
     *
     * @param listener               The listener.
     * @param priority               The priority.
     * @param receiveCancelled       Whether the listener should receive canceled events or not.
     * @param doNotPassThisParameter Do not pass this parameter, it is automatically
     *                               passed by the compiler and used for type inference.
     * @param <T>                    The type of the event.
     */
    @SuppressWarnings("unchecked")
    default <T extends Event> void addListener(@NotNull final EventListener<T> listener, @NotNull final EventPriority priority, final boolean receiveCancelled, @Nullable final T... doNotPassThisParameter) {
        this.addListener(EventListener.create(listener, priority, receiveCancelled), doNotPassThisParameter); // Passing it here is OK
    }

    /**
     * Adds the given listener to be run for the compiler inferred event type.
     *
     * @param listener               The listener.
     * @param doNotPassThisParameter Do not pass this parameter, it is automatically
     *                               passed by the compiler and used for type inference.
     * @param <T>                    The type of the event.
     */
    @SuppressWarnings("unchecked")
    default <T extends Event> void addListener(@NotNull final EventListener<T> listener, @Nullable final T... doNotPassThisParameter) {
        if (null == doNotPassThisParameter || 0 != doNotPassThisParameter.length) {
            throw new IllegalArgumentException("second parameter must not be manually passed");
        }

        // Infer event type from the compiler-generated synthetic array
        final var eventType = doNotPassThisParameter.getClass().getComponentType();

        if (null == eventType) {
            throw new IllegalStateException("failed to infer event type (component type is null)");
        }

        if (Object.class == eventType || Event.class == eventType || NonCancellableEvent.class == eventType || CancellableEvent.class == eventType) {
            throw new IllegalStateException("type inference failed for unexpected type " + eventType.getName());
        }

        if (!Event.class.isAssignableFrom(eventType)) {
            throw new IllegalArgumentException("listener method has wrong parameter with type " + eventType.getName());
        }

        // Force static initializer to run so that the event can be registered before we try to add a listener for it
        try {
            MethodHandles.publicLookup().ensureInitialized(eventType);
        } catch (final IllegalAccessException iae) {
            throw new IllegalStateException("event class " + eventType.getName() + " is not accessible by event registry", iae);
        } catch (final Throwable error) {
            throw new IllegalStateException("failed to force initialization for event class " + eventType.getName(), error);
        }

        this.getEventHandler(EventRegistry.<T>uncheckedCastToEventClass(eventType)).addListener(listener);
    }

    /**
     * Triggers a {@link CancellableEvent}, which will run all its listeners in the order of {@link EventPriority}
     * and handling {@link CancellationState}, taking into account {@link EventListener#receiveCancelled()} and returning
     * a {@link FinalCancellationState}.
     *
     * @param event The event.
     * @param <T>   The type of the event.
     * @return The {@link FinalCancellationState}.
     */
    @NotNull
    default <T extends CancellableEvent> FinalCancellationState triggerEvent(@NotNull final T event) {
        return this.getEventHandler(event).triggerEvent(event);
    }

    /**
     * Triggers a {@link NonCancellableEvent}, which will run all its listeners in the order of {@link EventPriority}.
     *
     * @param event The event.
     * @param <T>   The type of the event.
     */
    default <T extends NonCancellableEvent> void triggerEvent(@NotNull final T event) {
        this.getEventHandler(event).triggerEvent(event);
    }
}
