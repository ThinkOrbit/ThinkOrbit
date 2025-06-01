package tw.yukina.thinkorbit.service.intent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class IntentRouteRegistry {
    private final Map<String, IntentWrapper> intentRoutes = new HashMap<>();

    /**
     * Register a method-level intent handler.
     */
    public void registerIntent(Class<? extends Intent<?>> intentClass, Object instance, Method method) {
        String intentName = intentClass.getSimpleName().toLowerCase();
        IntentWrapper wrapper = new IntentWrapper(intentClass, instance, method);
        intentRoutes.put(intentName, wrapper);
        log.info("Registered intent handler for: {}", intentName);
    }

    /**
     * Execute the intent handler for the given intent.
     */
    public <T extends Intent<?>> void executeIntent(T intent) {
        String intentName = intent.getClass().getSimpleName().toLowerCase();
        IntentWrapper wrapper = intentRoutes.get(intentName);

        if (wrapper == null) {
            log.warn("No intent handler registered for: {}", intentName);
            return;
        }

        log.info("Executing intent handler for: {}", intentName);
        wrapper.execute(intent);
    }

    @Data
    @AllArgsConstructor
    static class IntentWrapper {
        private final Class<? extends Intent<?>> intentClass;
        private final Object instance;
        private final Method method;

        void execute(Intent<?> intent) {
            try {
                method.invoke(instance, intent);
            } catch (Exception e) {
                throw new RuntimeException("Failed to execute intent", e);
            }
        }
    }
}

