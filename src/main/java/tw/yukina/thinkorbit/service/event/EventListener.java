package tw.yukina.thinkorbit.service.event;

import tw.yukina.thinkorbit.service.event.entity.EventContext;
import tw.yukina.thinkorbit.service.event.entity.EventEntity;

public interface EventListener {
    void onEvent(EventContext context, EventEntity event);
}
