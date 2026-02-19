/**
 * A feature-complete, type-safe, and performant event system designed with
 * thread-safety, error handling, listener priorities, cancellation semantics and re-entrance in mind.
 * <p>
 * <b>Defining an event</b><br>
 * To define a new event, simply declare a record implementing either
 * {@link gg.darkutils.events.base.NonCancellableEvent} or
 * {@link gg.darkutils.events.base.CancellableEvent}.
 * <p>
 * Event handlers are created lazily and automatically the first time an event
 * class is used. No manual registration or static initializer blocks are required.
 * {@snippet :
 * public record MyEvent(@NotNull MyEventParam1 param1, @NotNull MyEventParam2 param2)
 *         implements NonCancellableEvent {
 * }
 *}
 *
 * <b>Registering a listener</b><br>
 * Register listeners for events using {@link gg.darkutils.events.base.EventRegistry#addListener(java.util.function.Consumer, gg.darkutils.events.base.Event[])}:
 * {@snippet :
 * // Don't forget to call .init()!
 * public void init() {
 *     EventRegistry.centralRegistry().addListener(this::onMyEvent);
 * }
 *
 * private void onMyEvent(@NotNull MyEvent event) {
 *     // This runs whenever MyEvent is triggered
 *     var param1 = event.param1();
 *     var param2 = event.param2();
 * }
 *}
 * Remember to invoke your {@code init()} method from your mod or feature entrypoint.
 *
 * <p>
 * <b>Listener priority</b><br>
 * Listeners may optionally be registered with a priority using
 * {@link gg.darkutils.events.base.EventRegistry#addListener(java.util.function.Consumer, gg.darkutils.events.base.EventPriority, gg.darkutils.events.base.Event[])}.
 * <p>
 * Priorities control the order in which listeners are executed (higher priority runs earlier).
 * See {@link gg.darkutils.events.base.EventPriority} for the full ordering guarantees.
 * {@snippet :
 * EventRegistry.centralRegistry().addListener(
 *         this::onMyEvent,
 *         EventPriority.ABOVE_NORMAL
 * );
 *}
 *
 * <p>
 * <b>Triggering an event</b><br>
 * You can trigger an event anywhere using:
 * {@snippet :
 * new MyEvent(new MyEventParam1(), new MyEventParam2()).trigger();
 *}
 *
 * <b>Working with cancellable events</b><br>
 * To define a cancellable event, implement {@link gg.darkutils.events.base.CancellableEvent}
 * and include a {@link gg.darkutils.events.base.CancellationState} record component:
 * {@snippet :
 * public record MyCancellableEvent(@NotNull CancellationState cancellationState,
 *                                  @NotNull MyEventParam1 param1,
 *                                  @NotNull MyEventParam2 param2)
 *         implements CancellableEvent {
 *
 *     public MyCancellableEvent(@NotNull MyEventParam1 param1, @NotNull MyEventParam2 param2) {
 *         this(CancellationState.ofFresh(), param1, param2);
 *     }
 * }
 *}
 * <p>
 * Listeners for cancellable events work the same way, except they may cancel
 * or uncancel the event:
 * {@snippet :
 * private void onMyEvent(@NotNull MyCancellableEvent event) {
 *     event.cancellationState().cancel();
 *     // or:
 *     event.cancellationState().uncancel(); // you need receiveCancelled = true on your addListener and event has to be cancelled by a previously ran listener for this to have any effect
 * }
 *}
 * <p>
 * Triggering cancellable events allows checking the result:
 * {@snippet :
 * if (new MyCancellableEvent(new MyEventParam1(), new MyEventParam2()).triggerAndNotCancelled()) { // a triggerAndCancelled method also exists for convenience so you don't have to invert the result manually with !
 *     // Event was not cancelled
 * }
 *}
 *
 * <p>
 * <b>Receiving cancelled events</b><br>
 * Listeners can optionally be registered to receive events that have already
 * been cancelled by earlier listeners using
 * {@link gg.darkutils.events.base.EventRegistry#addListener(java.util.function.Consumer, gg.darkutils.events.base.EventPriority, boolean, gg.darkutils.events.base.Event[])}:
 * {@snippet :
 * EventRegistry.centralRegistry().addListener(
 *         this::onMyEvent,
 *         EventPriority.ABOVE_NORMAL,
 *         true
 * );
 *}
 * The {@code receiveCancelled} parameter determines whether the listener should still be
 * invoked if the event has already been cancelled by a previously executed listener.
 * <p>
 * Cancellation is not terminal. If a listener with {@code receiveCancelled = true}
 * receives a cancelled event and subsequently uncancels it, then the remaining
 * listeners (those with lower priority, or later registration order if
 * {@link gg.darkutils.events.base.EventPriority#NORMAL}) will receive the event
 * normally, even if they were registered with {@code receiveCancelled = false}.
 *
 * <p>
 * <b>Advanced usage</b><br>
 * For advanced cases, you can implement your own
 * {@link gg.darkutils.events.base.EventRegistry} to customize event routing,
 * or provide a custom {@link gg.darkutils.events.base.EventHandler} implementation
 * to control how listeners are stored, prioritized, or executed.
 * <p>
 * All listener dispatching is thread-safe, but listeners themselves must
 * ensure thread-safety if they modify shared state.
 */
package gg.darkutils.events.base;

