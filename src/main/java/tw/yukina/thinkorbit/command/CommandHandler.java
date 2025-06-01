package tw.yukina.thinkorbit.command;

import org.jline.terminal.Terminal;

/**
 * Interface for command handlers.
 * Commands can implement this interface for class-level commands.
 */
public interface CommandHandler {
    /**
     * Execute the command
     * @param args command arguments
     * @param terminal the terminal for output
     * @return execution result
     */
    boolean execute(String[] args, Terminal terminal);
} 