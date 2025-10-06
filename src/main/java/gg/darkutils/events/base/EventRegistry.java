package gg.darkutils.events.base;

import gg.darkutils.events.base.impl.BasicEventRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    /**
     * Registers an {@link Event}.
     * <p>
     * An {@link Event} can't have listeners added before being registered.
     * <p>
     * You should call this method from your event class's static initializer block
     * to ensure it's always registered before listeners are added for it.
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
     * @param doNotPassThisParameter Do not pass this parameter, it is automatically
     *                               passed by the compiler and used for type inference.
     * @param <T>                    The type of the event.
     */
    @SuppressWarnings("unchecked")
    default <T extends Event> void addListener(@NotNull final EventListener<T> listener, @Nullable final T... doNotPassThisParameter) {
        if (null == doNotPassThisParameter || 0 != doNotPassThisParameter.length) {
            throw new IllegalArgumentException("second parameter must not be manually passed");
        }

        final var eventType = doNotPassThisParameter.getClass().getComponentType();

        if (Event.class == eventType) {
            throw new IllegalStateException("type inference failed");
        }

        this.getEventHandler((Class<T>) eventType).addListener(listener);
    }

    /**
     * Triggers a {@link CancellableEvent}, which will run all its listeners in the order of {@link EventPriority}
     * and handling {@link CancellationState}, taking into account {@link EventListener#receiveCancelled()} and returning
     * a final {@link CancellationState}.
     *
     * @param event The event.
     * @param <T>   The type of the event.
     * @return The {@link CancellationState}.
     */
    @SuppressWarnings("unchecked")
    @NotNull
    default <T extends CancellableEvent> CancellationState triggerEvent(@NotNull final T event) {
        return this.getEventHandler((Class<T>) event.getClass()).triggerEvent(event);
    }

    /**
     * Triggers a {@link NonCancellableEvent}, which will run all its listeners in the order of {@link EventPriority}.
     *
     * @param event The event.
     * @param <T>   The type of the event.
     */
    @SuppressWarnings("unchecked")
    default <T extends NonCancellableEvent> void triggerEvent(@NotNull final T event) {
        this.getEventHandler((Class<T>) event.getClass()).triggerEvent(event);
    }
}
