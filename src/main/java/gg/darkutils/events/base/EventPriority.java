package gg.darkutils.events.base;

/**
 * Priority levels for events. Higher value = runs earlier.
 */
public enum EventPriority {
    /**
     * Runs before all other event listeners with lower priority. If you want other
     * event listeners to not see cancelled events, or run code
     * before others for some other reason, use this.
     */
    HIGHEST(4),
    /**
     * Runs before {@link EventPriority#ABOVE_NORMAL} but after {@link EventPriority#HIGHEST}.
     */
    HIGH(3),
    /**
     * Runs before {@link EventPriority#NORMAL} but after {@link EventPriority#HIGH}.
     */
    ABOVE_NORMAL(2),
    /**
     * The default priority, which often relies on registration order.
     */
    NORMAL(1),
    /**
     * Runs before {@link EventPriority#LOW} but after {@link EventPriority#NORMAL}.
     */
    BELOW_NORMAL(0),
    /**
     * Runs before {@link EventPriority#LOWEST} but after {@link EventPriority#BELOW_NORMAL}.
     */
    LOW(-1),
    /**
     * Runs after all other event listeners with higher priority. If you want to decide
     * the final {@link CancellationState} of an event or if you want
     * other listeners to see the event while still preventing it from happening,
     * or otherwise you want to run code after others for some other reason, use this.
     */
    LOWEST(-2);

    private final int value;

    private EventPriority(final int value) {
        this.value = value;
    }

    /**
     * Returns an integer suitable for comparing {@link EventPriority} values.
     *
     * @return An integer suitable for comparing {@link EventPriority} values.
     */
    public final int getValue() {
        return this.value;
    }
}
