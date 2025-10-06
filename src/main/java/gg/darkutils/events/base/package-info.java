/**
 * A feature-complete, type-safe, and performant event system designed with
 * thread-safety, error handling, listener priorities, and cancellation semantics in mind.
 * <p>
 * <b>Defining an event</b><br>
 * To define a new event, simply declare a record implementing either
 * {@link gg.darkutils.events.base.NonCancellableEvent} or
 * {@link gg.darkutils.events.base.CancellableEvent}, and register it with the
 * central {@link gg.darkutils.events.base.EventRegistry}:
 * {@snippet :
 * public record MyEvent(@NotNull MyEventParam1 param1, @NotNull MyEventParam2 param2)
 *         implements NonCancellableEvent {
 *     static {
 *         EventRegistry.centralRegistry().registerEvent(MyEvent.class);
 *     }
 * }
 *}
 *
 * <b>Registering a listener</b><br>
 * Register listeners for events using {@link gg.darkutils.events.base.EventRegistry#addListener(gg.darkutils.events.base.EventListener, gg.darkutils.events.base.Event[])}:
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
 * <p>
 * <b>Triggering an event</b><br>
 * You can trigger an event anywhere using:
 * {@snippet :
 * EventRegistry.centralRegistry().triggerEvent(new MyEvent(new MyEventParam1(), new MyEventParam2()));
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
 *     static {
 *         EventRegistry.centralRegistry().registerEvent(MyCancellableEvent.class);
 *     }
 *
 *     public MyCancellableEvent(@NotNull MyEventParam1 param1, @NotNull MyEventParam2 param2) {
 *         this(CancellationState.ofFresh(), param1, param2);
 *     }
 * }
 *}
 * <p>
 * Listeners for cancellable events work the same way, except they may cancel the event:
 * {@snippet :
 * private void onMyEvent(@NotNull MyCancellableEvent event) {
 *     event.getCancellationState().cancel();
 * }
 *}
 * <p>
 * Triggering cancellable events allows checking the result:
 * {@snippet :
 * if (!EventRegistry.centralRegistry()
 *         .triggerEvent(new MyCancellableEvent(new MyEventParam1(), new MyEventParam2()))
 *         .isCancelled()) {
 *     // Event was not cancelled
 * }
 *}
 *
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
