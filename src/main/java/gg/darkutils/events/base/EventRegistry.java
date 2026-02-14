package gg.darkutils.events.base;

import gg.darkutils.events.base.impl.EventRegistryImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

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
        return EventRegistryImpl.getInstance();
    }

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
    default <T extends Event> void addListener(@NotNull final Consumer<T> listener, @NotNull final EventPriority priority, final boolean receiveCancelled, @Nullable final T... doNotPassThisParameter) {
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
    default <T extends Event> void addListener(@NotNull final Consumer<T> listener, @Nullable final T... doNotPassThisParameter) {
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

        this.getEventHandler((Class<T>) eventType).addListener((Consumer<? super Event>) (Object) listener);
    }

    /**
     * Triggers a {@link CancellableEvent}, which will run all its listeners in the order of {@link EventPriority}
     * and handling {@link CancellationState}, taking into account {@link EventListener#receiveCancelled()} and returning
     * a {@link CancellationResult}.
     *
     * @param event The event.
     * @param <T>   The type of the event.
     * @return The {@link CancellationResult}.
     */
    @SuppressWarnings("unchecked")
    @NotNull
    default <T extends CancellableEvent> CancellationResult triggerEvent(@NotNull final T event) {
        return this.getEventHandler((Class<T>) event.getClass()).triggerCancellableEvent(event);
    }

    /**
     * Triggers a {@link NonCancellableEvent}, which will run all its listeners in the order of {@link EventPriority}.
     *
     * @param event The event.
     * @param <T>   The type of the event.
     */
    @SuppressWarnings("unchecked")
    default <T extends NonCancellableEvent> void triggerEvent(@NotNull final T event) {
        this.getEventHandler((Class<T>) event.getClass()).triggerNonCancellableEvent(event);
    }
}
