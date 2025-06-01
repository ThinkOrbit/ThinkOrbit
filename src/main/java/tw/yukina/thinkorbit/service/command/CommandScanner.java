package tw.yukina.thinkorbit.service.command;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * Scanner for automatically discovering and registering commands
 */
@Slf4j
@Component
public class CommandScanner implements ApplicationRunner {
    private final ApplicationContext applicationContext;
    private final CommandRegistry commandRegistry;

    public CommandScanner(ApplicationContext applicationContext, CommandRegistry commandRegistry) {
        this.applicationContext = applicationContext;
        this.commandRegistry = commandRegistry;
    }

    @Override
    public void run(ApplicationArguments args) {
        scanAndRegisterCommands();
    }

    private void scanAndRegisterCommands() {
        log.info("Starting command scanning...");

        // Scan for classes with @Command annotation
        scanClassLevelCommands();

        // Scan for methods with @Command annotation in Spring beans
        scanMethodLevelCommands();

        log.info("Command scanning completed. Total commands registered: {}",
                commandRegistry.getCommandNames().size());
    }

    private void scanClassLevelCommands() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Command.class));

        // Only scan the impl subpackage to avoid scanning system components
        Set<BeanDefinition> candidates = scanner.findCandidateComponents("tw.yukina.thinkorbit.command");

        for (BeanDefinition beanDefinition : candidates) {
            try {
                Class<?> clazz = Class.forName(beanDefinition.getBeanClassName());
                Command commandAnnotation = clazz.getAnnotation(Command.class);

                if (commandAnnotation != null && CommandHandler.class.isAssignableFrom(clazz)) {
                    // Try to get bean from Spring context
                    Object bean = null;
                    try {
                        bean = applicationContext.getBean(clazz);
                    } catch (Exception e) {
                        // If not a Spring bean, create new instance
                        bean = clazz.getDeclaredConstructor().newInstance();
                    }

                    if (bean instanceof CommandHandler) {
                        CommandRegistry.CommandWrapper wrapper = new CommandRegistry.CommandWrapper(
                                (CommandHandler) bean, commandAnnotation.description(), commandAnnotation.interactive());
                        commandRegistry.registerCommand(commandAnnotation.value(), wrapper,
                                commandAnnotation.aliases());
                        log.info("Registered class-level command: {} from class {} (interactive: {})",
                                commandAnnotation.value(), clazz.getSimpleName(), commandAnnotation.interactive());
                    }
                }
            } catch (Exception e) {
                log.error("Error registering class-level command from {}: {}",
                        beanDefinition.getBeanClassName(), e.getMessage(), e);
            }
        }
    }

    private void scanMethodLevelCommands() {
        String[] beanNames = applicationContext.getBeanDefinitionNames();

        for (String beanName : beanNames) {
            Object bean = applicationContext.getBean(beanName);
            Class<?> clazz = bean.getClass();

            // Skip system components in command package (except impl subpackage)
            String className = clazz.getName();
            if (className.startsWith("tw.yukina.thinkorbit.service.command")) {
                continue;
            }

            // Handle proxies
            if (clazz.getName().contains("$$")) {
                clazz = clazz.getSuperclass();
            }

            for (Method method : clazz.getDeclaredMethods()) {
                Command commandAnnotation = method.getAnnotation(Command.class);
                if (commandAnnotation != null) {
                    // Validate method signature
                    Class<?>[] paramTypes = method.getParameterTypes();
                    if (paramTypes.length == 2 &&
                            paramTypes[0] == String[].class &&
                            paramTypes[1] == org.jline.terminal.Terminal.class) {

                        method.setAccessible(true);
                        CommandRegistry.CommandWrapper wrapper = new CommandRegistry.CommandWrapper(
                                bean, method, commandAnnotation.description(), commandAnnotation.interactive());
                        commandRegistry.registerCommand(commandAnnotation.value(), wrapper,
                                commandAnnotation.aliases());
                        log.info("Registered method-level command: {} from {}.{} (interactive: {})",
                                commandAnnotation.value(), clazz.getSimpleName(), method.getName(),
                                commandAnnotation.interactive());
                    } else {
                        log.warn("Command method {}.{} has invalid signature. " +
                                        "Expected: (String[] args, Terminal terminal)",
                                clazz.getSimpleName(), method.getName());
                    }
                }
            }
        }
    }
} 