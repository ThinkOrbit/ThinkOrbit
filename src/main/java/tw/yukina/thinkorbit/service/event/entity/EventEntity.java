package tw.yukina.thinkorbit.service.event.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Entity
@Getter
@Setter
@ToString
public class EventEntity {
    @Id
    @GeneratedValue
    private Long id;

    private Instant occurredAt;

    private String type;

    private String source;

    @Enumerated(EnumType.STRING)
    private SemanticTier semanticTier;

    private String traceId;

    private String causeId;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = JsonMapConverter.class)
    private Map<String, Object> payload;

    public Optional<String> getTraceId() {
        return Optional.ofNullable(traceId);
    }

    public Optional<String> getCauseId() {
        return Optional.ofNullable(causeId);
    }
}
