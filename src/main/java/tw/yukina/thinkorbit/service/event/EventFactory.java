package tw.yukina.thinkorbit.service.event;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;
import tw.yukina.thinkorbit.service.event.entity.EventEntity;
import tw.yukina.thinkorbit.service.event.entity.SemanticTier;

import java.time.Instant;
import java.util.Map;

@AllArgsConstructor
@RequiredArgsConstructor
public class EventFactory {

    private final String source;

    @Nullable
    private String traceId;

    @Nullable
    private String causeId;

    public EventEntity createEvent(String type, SemanticTier semanticTier) {
        EventEntity event = new EventEntity();
        event.setOccurredAt(Instant.now());
        event.setType(type);
        event.setSource(source);
        event.setSemanticTier(semanticTier);

        event.setTraceId(traceId);
        event.setCauseId(causeId);

        return event;
    }

    public EventEntity createEvent(String type, SemanticTier semanticTier, String causeId) {
        EventEntity event = createEvent(type, semanticTier);
        event.setCauseId(causeId);

        return event;
    }

    public EventEntity createEvent(String type, SemanticTier semanticTier, Map<String, Object> payload) {
        EventEntity event = createEvent(type, semanticTier);
        event.setPayload(payload);

        return event;
    }

    public EventFactory withTraceId(String traceId) {
        EventFactory factory = new EventFactory(this.source);
        factory.traceId = traceId;

        return factory;
    }

    public EventFactory withTraceIdAndCauseId(String traceId, String causeId) {
        EventFactory factory = new EventFactory(this.source);
        factory.traceId = traceId;
        factory.causeId = causeId;

        return factory;
    }
}
