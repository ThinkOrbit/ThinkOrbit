package tw.yukina.thinkorbit.service.command;

import org.jline.terminal.Terminal;

/**
 * Interface for interactive command handlers.
 * Interactive commands handle their own input loop until explicitly exited.
 */
public interface InteractiveCommandHandler extends CommandHandler {
    
    /**
     * Handle interactive input.
     * This method will be called repeatedly with user input until shouldExit() returns true.
     * 
     * @param input the user input line
     * @param terminal the terminal for input/output operations
     */
    void handleInput(String input, Terminal terminal);
    
    /**
     * Check if the interactive session should exit.
     * 
     * @return true if the interactive session should end
     */
    boolean shouldExit();
    
    /**
     * Called when entering interactive mode.
     * Can be used to display welcome message or initialize state.
     * 
     * @param terminal the terminal for output
     */
    default void onEnterInteractive(Terminal terminal) {
        // Optional: override to provide entry behavior
    }
    
    /**
     * Called when exiting interactive mode.
     * Can be used to display goodbye message or cleanup.
     * 
     * @param terminal the terminal for output
     */
    default void onExitInteractive(Terminal terminal) {
        // Optional: override to provide exit behavior
    }
} 