package tw.yukina.thinkorbit.service.intent;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Intent<T> {
    private final T payload;
    private final String traceId;

    public Intent(T payload) {
        this(payload, null);
    }
}
