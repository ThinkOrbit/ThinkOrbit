package tw.yukina.thinkorbit.service.task;

import lombok.Getter;

@Getter
public enum TaskEvent {
    CREATED_TASK("created_task");

    private final String eventName;

    TaskEvent(String eventName) {
        this.eventName = eventName;
    }
}
