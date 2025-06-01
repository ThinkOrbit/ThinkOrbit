package tw.yukina.thinkorbit.service.event;

import lombok.Getter;
import java.time.Instant;
import java.util.Optional;

@Getter
public class ReplayRange {
    private final Long fromEventId;
    private final Instant fromTime;
    private final ReplayLevel level;

    public ReplayRange(Long fromEventId, Instant fromTime, ReplayLevel level) {
        this.fromEventId = fromEventId;
        this.fromTime = fromTime;
        this.level = level;
    }

    public Optional<Long> getFromEventId() {
        return Optional.ofNullable(fromEventId);
    }

    public Optional<Instant> getFromTime() {
        return Optional.ofNullable(fromTime);
    }
}
