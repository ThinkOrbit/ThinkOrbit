package tw.yukina.thinkorbit.service.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import tw.yukina.thinkorbit.service.event.entity.EventContext;
import tw.yukina.thinkorbit.service.event.entity.EventEntity;
import tw.yukina.thinkorbit.service.event.entity.EventRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.time.Instant;

@Slf4j
@Service
public class StandardEventBus implements EventBus {

    private static final int PAGE_SIZE = 1000;

    private final EventRepository eventRepository;
    private final List<EventSubscription> subscriptions = new CopyOnWriteArrayList<>();
    private final List<EventEntity> eventHistory = new ArrayList<>();
    private boolean isClosed = false;

    @Autowired
    public StandardEventBus(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Override
    public void publish(EventEntity event) {
        if (isClosed) {
            throw new IllegalStateException("EventBus is closed");
        }

        event = eventRepository.save(event);

        eventHistory.add(event);

        for (EventSubscription subscription : subscriptions) {
            if (subscription.filter().matches(event)) {
                EventContext context = new EventContext();
                subscription.listener().onEvent(context, event);
            }
        }
    }

    @Override
    public void subscribe(EventFilter filter, EventListener listener) {
        if (isClosed) {
            throw new IllegalStateException("EventBus is closed");
        }

        log.info("Subscribing events to {}", filter);

        subscriptions.add(new EventSubscription(filter, listener));
    }

    @Override
    public void replay(ReplayRange range, EventListener listener) {
        if (isClosed) {
            throw new IllegalStateException("EventBus is closed");
        }

        EventContext replayContext = new EventContext();
        replayContext.setReplay(true);
        replayContext.setReplayLevel(range.getLevel());

        Long fromEventId = range.getFromEventId().orElse(null);
        Instant fromTime = range.getFromTime().orElse(null);

        int pageNumber = 0;
        boolean hasMore = true;

        while (hasMore) {
            Pageable pageable = PageRequest.of(pageNumber, PAGE_SIZE);
            Page<EventEntity> page;
            if (fromEventId != null && fromTime != null) {
                page = eventRepository.findByIdGreaterThanEqualAndOccurredAtGreaterThanEqual(fromEventId, fromTime, pageable);
            } else if (fromEventId != null) {
                page = eventRepository.findByIdGreaterThanEqual(fromEventId, pageable);
            } else if (fromTime != null) {
                page = eventRepository.findByOccurredAtGreaterThanEqual(fromTime, pageable);
            } else {
                page = eventRepository.findAll(pageable);
            }

            page.getContent().forEach(event -> listener.onEvent(replayContext, event));

            hasMore = page.hasNext();
            pageNumber++;
        }
    }

    @Override
    public void close() {
        isClosed = true;
        subscriptions.clear();
        eventHistory.clear();
    }

    private record EventSubscription(EventFilter filter, EventListener listener) {
    }
}
