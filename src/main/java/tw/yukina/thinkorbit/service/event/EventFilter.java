package tw.yukina.thinkorbit.service.event;

import tw.yukina.thinkorbit.service.event.entity.EventEntity;

public interface EventFilter {
    /**
     * Checks if the event matches the filter criteria.
     *
     * @param event the event to check
     * @return true if the event matches, false otherwise
     */
    boolean matches(EventEntity event);

}
