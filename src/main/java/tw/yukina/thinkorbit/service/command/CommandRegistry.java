package tw.yukina.thinkorbit.service.command;

import lombok.extern.slf4j.Slf4j;
import org.jline.terminal.Terminal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Registry for managing shell commands
 */
@Slf4j
@Component
public class CommandRegistry {
    private static final Logger logger = LoggerFactory.getLogger(CommandRegistry.class);
    
    private final Map<String, CommandWrapper> commands = new HashMap<>();
    
    /**
     * Register a class-level command
     */
    public void registerCommand(String name, CommandHandler handler, String description) {
        registerCommand(name, new CommandWrapper(handler, description));
    }
    
    /**
     * Register a method-level command
     */
    public void registerCommand(String name, Object instance, Method method, String description) {
        registerCommand(name, new CommandWrapper(instance, method, description));
    }
    
    /**
     * Register command with aliases
     */
    public void registerCommand(String name, CommandWrapper wrapper, String... aliases) {
        commands.put(name.toLowerCase(), wrapper);
        log.info("Registered command with alias: {}", name);
        
        for (String alias : aliases) {
            commands.put(alias.toLowerCase(), wrapper);
            logger.info("Registered alias '{}' for command '{}'", alias, name);
        }
    }
    
    private void registerCommand(String name, CommandWrapper wrapper) {
        commands.put(name.toLowerCase(), wrapper);
        log.info("Registered command: {}", name);
    }
    
    /**
     * Execute a command
     */
    public boolean executeCommand(String commandLine, Terminal terminal) {
        String[] parts = commandLine.trim().split("\\s+");
        if (parts.length == 0) {
            return false;
        }
        
        String commandName = parts[0].toLowerCase();
        CommandWrapper wrapper = commands.get(commandName);
        
        if (wrapper == null) {
            terminal.writer().println("Unknown command: " + commandName);
            return false;
        }
        
        String[] args = new String[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, args.length);
        
        try {
            return wrapper.execute(args, terminal);
        } catch (Exception e) {
            logger.error("Error executing command '{}': {}", commandName, e.getMessage(), e);
            terminal.writer().println("Error executing command: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get all registered command names
     */
    public Set<String> getCommandNames() {
        return commands.keySet();
    }
    
    /**
     * Get command description
     */
    public String getCommandDescription(String commandName) {
        CommandWrapper wrapper = commands.get(commandName.toLowerCase());
        return wrapper != null ? wrapper.getDescription() : null;
    }
    
    /**
     * Wrapper class for command information
     */
    static class CommandWrapper {
        private final CommandHandler handler;
        private final Object instance;
        private final Method method;
        private final String description;
        
        // For class-level commands
        CommandWrapper(CommandHandler handler, String description) {
            this.handler = handler;
            this.instance = null;
            this.method = null;
            this.description = description;
        }
        
        // For method-level commands
        CommandWrapper(Object instance, Method method, String description) {
            this.handler = null;
            this.instance = instance;
            this.method = method;
            this.description = description;
        }
        
        boolean execute(String[] args, Terminal terminal) throws Exception {
            if (handler != null) {
                return handler.execute(args, terminal);
            } else if (method != null) {
                Class<?>[] paramTypes = method.getParameterTypes();
                if (paramTypes.length == 2 && 
                    paramTypes[0] == String[].class && 
                    paramTypes[1] == Terminal.class) {
                    Object result = method.invoke(instance, args, terminal);
                    return result instanceof Boolean ? (Boolean) result : true;
                } else {
                    throw new IllegalArgumentException(
                        "Command method must have signature: (String[] args, Terminal terminal)");
                }
            }
            return false;
        }
        
        String getDescription() {
            return description;
        }
    }
} 