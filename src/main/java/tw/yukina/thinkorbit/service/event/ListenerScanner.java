package tw.yukina.thinkorbit.service.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import tw.yukina.thinkorbit.service.event.entity.EventContext;
import tw.yukina.thinkorbit.service.event.entity.EventEntity;
import tw.yukina.thinkorbit.service.event.entity.SemanticTier;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Component
public class ListenerScanner implements ApplicationRunner {

    private final EventBus eventBus;
    private final ApplicationContext applicationContext;
    private boolean hasScanned = false;

    @Autowired
    public ListenerScanner(EventBus eventBus, ApplicationContext applicationContext) {
        this.eventBus = eventBus;
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (hasScanned) {
            return;
        }
        hasScanned = true;

        scanAndRegisterEventListeners();
    }

    private void scanAndRegisterEventListeners() {
        String[] beanNames = applicationContext.getBeanDefinitionNames();

        for (String beanName : beanNames) {
            Object bean = applicationContext.getBean(beanName);
            Class<?> beanClass = bean.getClass();

            // Handle proxies
            if (beanClass.getName().contains("$$")) {
                beanClass = beanClass.getSuperclass();
            }

            for (Method method : beanClass.getDeclaredMethods()) {
                OnEvent onEventAnnotation = method.getAnnotation(OnEvent.class);
                if (onEventAnnotation != null) {
                    registerEventListener(bean, method, onEventAnnotation);
                }
            }
        }
    }

    private void registerEventListener(Object bean, Method method, OnEvent onEventAnnotation) {
        EventFilter filter = createEventFilter(onEventAnnotation);

        EventListener listener = createEventListener(bean, method);

        eventBus.subscribe(filter, listener);
    }

    private EventFilter createEventFilter(OnEvent onEventAnnotation) {
        return event -> {
            if (!onEventAnnotation.type().equals(event.getType())) {
                return false;
            }

            String sourceFilter = onEventAnnotation.source();
            if (!sourceFilter.isEmpty() && !sourceFilter.equals(event.getSource())) {
                return false;
            }

            SemanticTier[] tierFilters = onEventAnnotation.tier();
            if (tierFilters.length > 0) {
                Set<SemanticTier> allowedTiers = new HashSet<>(Arrays.asList(tierFilters));
                return allowedTiers.contains(event.getSemanticTier());
            }

            return true;
        };
    }

    private EventListener createEventListener(Object bean, Method method) {
        return (context, event) -> {
            try {
                Class<?>[] paramTypes = method.getParameterTypes();
                method.setAccessible(true);

                // No parameters
                if (paramTypes.length == 0) {
                    method.invoke(bean);

                    // One parameter
                } else if (paramTypes.length == 1) {
                    if (paramTypes[0].isAssignableFrom(EventEntity.class)) {
                        method.invoke(bean, event);
                    } else if (paramTypes[0].isAssignableFrom(EventContext.class)) {
                        method.invoke(bean, context);
                    }

                    // Two parameters
                } else if (paramTypes.length == 2) {
                    if (paramTypes[0].isAssignableFrom(EventContext.class)
                            && paramTypes[1].isAssignableFrom(EventEntity.class)) {
                        method.invoke(bean, context, event);
                    } else if (paramTypes[0].isAssignableFrom(EventEntity.class)
                            && paramTypes[1].isAssignableFrom(EventContext.class)) {
                        method.invoke(bean, event, context);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to invoke event listener method: " + method.getName(), e);
            }
        };
    }
}
