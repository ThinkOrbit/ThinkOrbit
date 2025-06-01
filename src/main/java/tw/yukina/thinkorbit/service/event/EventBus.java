package tw.yukina.thinkorbit.service.event;


import tw.yukina.thinkorbit.service.event.entity.EventEntity;

public interface EventBus {
    /**
     * Publishes an event to the event bus.
     *
     * @param event the event to publish
     */
    void publish(EventEntity event);

    /**
     * Subscribes a listener to events matching the specified filter.
     *
     * @param filter   the filter to match events
     * @param listener the listener to notify when events match
     */
    void subscribe(EventFilter filter, EventListener listener);

    /**
     * Replays events within a specified range to a listener.
     *
     * @param range    the range of events to replay
     * @param listener the listener to receive the replayed events
     */
    void replay(ReplayRange range, EventListener listener);

    /**
     * Closes the event bus and releases resources.
     */
    void close();

}
