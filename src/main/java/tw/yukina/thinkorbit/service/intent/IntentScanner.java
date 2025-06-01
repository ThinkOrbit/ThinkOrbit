package tw.yukina.thinkorbit.service.intent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Slf4j
@Component
public class IntentScanner implements ApplicationRunner {
    private final ApplicationContext applicationContext;
    private final IntentRouteRegistry intentRouteRegistry;

    public IntentScanner(ApplicationContext applicationContext, IntentRouteRegistry intentRouteRegistry) {
        this.applicationContext = applicationContext;
        this.intentRouteRegistry = intentRouteRegistry;
    }

    @Override
    public void run(ApplicationArguments args) {
        scanAndRegisterIntentRoute();
    }

    private void scanAndRegisterIntentRoute() {
        String[] beanNames = applicationContext.getBeanDefinitionNames();

        for (String beanName : beanNames) {
            Object bean = applicationContext.getBean(beanName);
            Class<?> clazz = bean.getClass();

            // Handle proxies
            if (clazz.getName().contains("$$")) {
                clazz = clazz.getSuperclass();
            }

            for (Method method : clazz.getDeclaredMethods()) {
                IntentMapping methodAnnotation = method.getAnnotation(IntentMapping.class);
                if (methodAnnotation != null) {
                    // Validate method signature
                    Class<?>[] paramTypes = method.getParameterTypes();
                    if (paramTypes.length == 1 && Intent.class.isAssignableFrom(paramTypes[0])) {
                        @SuppressWarnings("unchecked") Class<? extends Intent<?>> intentType = (Class<? extends Intent<?>>) paramTypes[0];

                        method.setAccessible(true);
                        intentRouteRegistry.registerIntent(intentType, bean, method);
                        log.info("Registered intent handler from {}.{}", clazz.getSimpleName(), method.getName());
                    } else {
                        log.warn("Intent handler method {}.{} has invalid signature. " +
                                "Expected: (Intent<?> intent)", clazz.getSimpleName(), method.getName());
                    }
                }
            }
        }
    }
}
