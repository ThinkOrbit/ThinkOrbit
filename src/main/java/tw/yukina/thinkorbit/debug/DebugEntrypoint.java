package tw.yukina.thinkorbit.debug;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import tw.yukina.thinkorbit.service.event.ReplayLevel;
import tw.yukina.thinkorbit.service.event.ReplayRange;
import tw.yukina.thinkorbit.service.event.StandardEventBus;
import tw.yukina.thinkorbit.service.event.entity.EventEntity;
import tw.yukina.thinkorbit.service.event.entity.SemanticTier;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class DebugEntrypoint implements ApplicationRunner {

    private final StandardEventBus eventBus;

    public DebugEntrypoint(StandardEventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("thinkorbit debug entrypoint started");

        // Create a test event
        EventEntity testEvent = new EventEntity();
        testEvent.setType("TEST_EVENT");
        testEvent.setSource("DebugEntrypoint");
        testEvent.setSemanticTier(SemanticTier.INTERNAL);
        testEvent.setOccurredAt(Instant.now());

        Map<String, Object> payload = new HashMap<>();
        payload.put("message", "Hello, ThinkOrbit!");
        testEvent.setPayload(payload);

        // Subscribe to the event
        eventBus.subscribe(event -> event.getType().equals("TEST_EVENT"), (context, event) -> {
            log.info("Received event: {}", event.getPayload().get("message"));
            if (context.isReplay()) {
                log.info("This is a replayed event, replay level: {}", context.getReplayLevel());
            }
        });

        // Publish the event
        log.info("Publishing test event");
        eventBus.publish(testEvent);

        // Test replay functionality
        log.info("Start replaying events");
        ReplayRange range = new ReplayRange(null,  // Start from the latest ID
                Instant.now().minusSeconds(60),  // Start from 60 seconds ago
                ReplayLevel.SEMANTIC_REPLAY  // Use semantic replay level
        );

        eventBus.replay(range, (context, event) -> {
            log.info("Replayed event: {}", event.getPayload().get("message"));
        });
    }
}
