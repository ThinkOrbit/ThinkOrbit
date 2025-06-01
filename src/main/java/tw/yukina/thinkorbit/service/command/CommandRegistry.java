package tw.yukina.thinkorbit.service.command;

import lombok.extern.slf4j.Slf4j;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
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
    private final Map<String, CommandWrapper> commands = new HashMap<>();
    
    /**
     * Register a class-level command
     */
    public void registerCommand(String name, CommandHandler handler, String description) {
        registerCommand(name, handler, description, false);
    }
    
    /**
     * Register a class-level command with interactive flag
     */
    public void registerCommand(String name, CommandHandler handler, String description, boolean interactive) {
        registerCommand(name, new CommandWrapper(handler, description, interactive));
    }
    
    /**
     * Register a method-level command
     */
    public void registerCommand(String name, Object instance, Method method, String description) {
        registerCommand(name, instance, method, description, false);
    }
    
    /**
     * Register a method-level command with interactive flag
     */
    public void registerCommand(String name, Object instance, Method method, String description, boolean interactive) {
        registerCommand(name, new CommandWrapper(instance, method, description, interactive));
    }
    
    /**
     * Register command with aliases
     */
    @SuppressWarnings("ClassEscapesDefinedScope")
    public void registerCommand(String name, CommandWrapper wrapper, String... aliases) {
        commands.put(name.toLowerCase(), wrapper);
        log.info("Registered command with alias: {}", name);
        
        for (String alias : aliases) {
            commands.put(alias.toLowerCase(), wrapper);
            log.info("Registered alias '{}' for command '{}'", alias, name);
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
            // Handle interactive commands
            if (wrapper.isInteractive() && wrapper.handler instanceof InteractiveCommandHandler) {
                return executeInteractiveCommand((InteractiveCommandHandler) wrapper.handler, args, terminal);
            } else {
                return wrapper.execute(args, terminal);
            }
        } catch (Exception e) {
            log.error("Error executing command '{}': {}", commandName, e.getMessage(), e);
            terminal.writer().println("Error executing command: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Execute an interactive command
     */
    private boolean executeInteractiveCommand(InteractiveCommandHandler handler, String[] args, Terminal terminal) {
        try {
            // First execute the command with initial arguments
            boolean initialized = handler.execute(args, terminal);
            if (!initialized) {
                return false;
            }

            // Enter interactive mode
            handler.onEnterInteractive(terminal);

            // Create a LineReader for interactive input
            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .build();

            // Interactive loop
            while (!handler.shouldExit()) {
                try {
                    String input = reader.readLine("> ");
                    if (input != null) {
                        handler.handleInput(input, terminal);
                    }
                } catch (UserInterruptException e) {
                    // Handle Ctrl+C
                    terminal.writer().println("\nInterrupted. Exiting interactive mode.");
                    break;
                } catch (EndOfFileException e) {
                    // Handle Ctrl+D
                    terminal.writer().println("\nEnd of input. Exiting interactive mode.");
                    break;
                }
            }

            // Exit interactive mode
            handler.onExitInteractive(terminal);
            return true;
            
        } catch (Exception e) {
            log.error("Error in interactive command execution: {}", e.getMessage(), e);
            terminal.writer().println("Error in interactive mode: " + e.getMessage());
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
        private final boolean interactive;
        
        // For class-level commands
        CommandWrapper(CommandHandler handler, String description, boolean interactive) {
            this.handler = handler;
            this.instance = null;
            this.method = null;
            this.description = description;
            this.interactive = interactive;
        }
        
        // For method-level commands
        CommandWrapper(Object instance, Method method, String description, boolean interactive) {
            this.handler = null;
            this.instance = instance;
            this.method = method;
            this.description = description;
            this.interactive = interactive;
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
        
        boolean isInteractive() {
            return interactive;
        }
    }
} 