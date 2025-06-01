package tw.yukina.thinkorbit.service.event.entity;

import lombok.Getter;
import lombok.Setter;
import tw.yukina.thinkorbit.service.event.ReplayLevel;

@Getter
@Setter
public class EventContext {
    private boolean replay;
    private boolean dryRun;
    private ReplayLevel replayLevel;

    public EventContext() {
        this.replay = false;
        this.dryRun = false;
        this.replayLevel = null;
    }
}
