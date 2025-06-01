package tw.yukina.thinkorbit.service.shell;

import org.jline.builtins.ssh.Ssh;
import org.jline.terminal.Terminal;
import org.jline.utils.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tw.yukina.thinkorbit.service.command.CommandRegistry;
import tw.yukina.thinkorbit.service.shell.highlighter.SqlHighlighter;
import tw.yukina.thinkorbit.service.shell.status.DefaultStatusProvider;

import java.io.IOException;

/**
 * Shell Service, responsible for handling shell session logic
 */
@Service
public class ShellService {
    private static final Logger logger = LoggerFactory.getLogger(ShellService.class);

    private final CommandRegistry commandRegistry;

    public ShellService(CommandRegistry commandRegistry) {
        this.commandRegistry = commandRegistry;
    }

    /**
     * Handle shell session
     *
     * @param params Shell parameters
     */
    public void handleShellSession(Ssh.ShellParams params) {
        Terminal terminal = null;
        ShellSession session;

        try {
            terminal = params.getTerminal();
            Terminal finalTerminal = terminal;
            session = new ShellSession(
                    terminal,
                    new SqlHighlighter(),
                    new DefaultStatusProvider(Status.getStatus(terminal)),
                    command -> handleCommand(command, finalTerminal),
                    commandRegistry,
                    params
            );

            // Start the shell session
            session.start();

            // Session has ended (user typed exit or disconnected)
            logger.info("Shell session ended normally");

        } catch (Exception e) {
            logger.error("Error handling shell session", e);
        } finally {
            // Clean up resources
            if (terminal != null) {
                try {
                    // Ensure all output is flushed before closing
                    terminal.flush();

                    // Close the terminal
                    terminal.close();
                    logger.info("Terminal closed successfully");
                } catch (IOException e) {
                    logger.error("Error closing terminal", e);
                }
            }
        }
    }

    /**
     * Handle command
     *
     * @param command  Command string
     * @param terminal Terminal for output
     */
    private void handleCommand(String command, Terminal terminal) {
        logger.info("Received command: {}", command);

        if (command == null || command.trim().isEmpty()) {
            return;
        }

        // Execute command through CommandRegistry
        boolean success = commandRegistry.executeCommand(command, terminal);

        if (!success) {
            logger.warn("Command execution failed or unknown command: {}", command);
        }
    }
} 