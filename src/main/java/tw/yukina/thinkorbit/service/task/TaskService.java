package tw.yukina.thinkorbit.service.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tw.yukina.thinkorbit.service.event.EventBus;
import tw.yukina.thinkorbit.service.event.OnEvent;
import tw.yukina.thinkorbit.service.event.entity.EventContext;
import tw.yukina.thinkorbit.service.event.entity.EventEntity;
import tw.yukina.thinkorbit.service.intent.Intent;
import tw.yukina.thinkorbit.service.intent.IntentMapping;
import tw.yukina.thinkorbit.service.task.intent.CreateTask;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
class TaskService {

    private final EventBus eventBus;

    TaskService(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @IntentMapping
    private void createTaskRoute(Intent<CreateTask> intent) {
        log.info("Creating task with intent: {}", intent.getPayload().getName());

        Task task = new Task();
        task.setId("task-" + System.currentTimeMillis());
        task.setName(intent.getPayload().getName());

        EventEntity event = EventEntity.builder()
                .type("created_task")
                .source("order-service")
                .payload(Map.of("taskId", task.getId(), "taskName", task.getName()))
                .build();
        eventBus.publish(event);
    }

    @OnEvent(type = "created_task")
    public void handleOrderCreated(EventContext context, EventEntity event) {
        System.out.println("Received task created event: " + event.getPayload());
        if (context.isReplay()) {
            System.out.println("This is a replayed event");
        }
    }
}
