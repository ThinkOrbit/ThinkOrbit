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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class TaskService {

    private final List<Task> tasks = new ArrayList<>();

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

        tasks.add(task);

        EventEntity event = EventEntity.builder()
                .type(TaskEvent.CREATED_TASK.getEventName())
                .source(this.getClass().getSimpleName())
                .payload(Map.of("taskId", task.getId(), "taskName", task.getName()))
                .build();

        eventBus.publish(event);
    }

    public List<Task> getTasks() {
        return List.copyOf(tasks);
    }

    @OnEvent(type = "created_task")
    public void handleOrderCreated(EventContext context, EventEntity event) {
        System.out.println("Received task created event: " + event.getPayload());
        if (context.isReplay()) {
            System.out.println("This is a replayed event");
        }
    }
}
