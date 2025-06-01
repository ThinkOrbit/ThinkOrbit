package tw.yukina.thinkorbit.shell;

import lombok.Getter;
import lombok.SneakyThrows;
import org.jline.builtins.Completers;
import org.jline.builtins.ssh.Ssh;
import org.jline.reader.Completer;
import org.jline.reader.Highlighter;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.EndOfFileException;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.AbstractTerminal;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.utils.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tw.yukina.thinkorbit.command.CommandRegistry;
import tw.yukina.thinkorbit.shell.highlighter.ShellHighlighter;
import tw.yukina.thinkorbit.shell.status.ShellStatusProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class ShellSession {
    private static final Logger logger = LoggerFactory.getLogger(ShellSession.class);
    private final Ssh.ShellParams params;
    private final Terminal terminal;
    private final ShellHighlighter shellHighlighter;
    private final ShellStatusProvider statusProvider;
    private final Consumer<String> commandHandler;
    private final CommandRegistry commandRegistry;

    @Getter
    private volatile boolean terminated = false;

    public ShellSession(Terminal terminal, ShellHighlighter shellHighlighter,
                        ShellStatusProvider statusProvider, Consumer<String> commandHandler,
                        CommandRegistry commandRegistry, Ssh.ShellParams params) {
        this.terminal = terminal;
        this.shellHighlighter = shellHighlighter;
        this.statusProvider = statusProvider;
        this.commandHandler = commandHandler;
        this.commandRegistry = commandRegistry;
        this.params = params;
    }

    public void start() {
        try {
            // Build completer based on registered commands
            Completer treeCompleter = buildCommandCompleter();

            Highlighter highlighter = shellHighlighter::highlight;

            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .highlighter(highlighter)
                    .completer(treeCompleter)
                    .option(LineReader.Option.MENU_COMPLETE, true)
                    .build();

            Status status = Status.getStatus(terminal);
            if (status != null && statusProvider != null) {
                statusProvider.start();
            }

            displayWelcomeMessage();

            while (!terminated) {
                try {
                    String line = reader.readLine("prompt> ");
                    if (line == null || line.trim().equalsIgnoreCase("exit")) {
                        logger.info("User requested exit");
                        stop();
                        break;
                    }
                    commandHandler.accept(line);
                } catch (UserInterruptException e) {
                    // Handle Ctrl+C
                    logger.info("User interrupted session (Ctrl+C)");
                    stop();
                    break;
                } catch (EndOfFileException e) {
                    // Handle Ctrl+D
                    logger.info("End of file reached (Ctrl+D)");
                    stop();
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("Error in shell session", e);
            terminated = true;
        }
    }

    private Completer buildCommandCompleter() {
        List<Completers.TreeCompleter.Node> nodes = new ArrayList<>();

        // Add registered commands
        Set<String> commandNames = commandRegistry.getCommandNames();
        for (String commandName : commandNames) {
            nodes.add(Completers.TreeCompleter.node(commandName));
        }

        // Add built-in exit command
        nodes.add(Completers.TreeCompleter.node("exit"));

        return new Completers.TreeCompleter(nodes.toArray(new Completers.TreeCompleter.Node[0]));
    }

    @SneakyThrows
    private void stop() {
        displayGoodbyeMessage();
        terminal.flush();
        Thread.sleep(100);

        terminated = true;
        if (statusProvider != null) {
            statusProvider.stop();
        }

        try {
            terminal.close();
            params.getSession().close();
        } catch (Exception e) {
            logger.error("Error closing terminal", e);
        }
    }

    private void displayWelcomeMessage() {
        AttributedStringBuilder builder = new AttributedStringBuilder();
        builder.append("Welcome to the SSH shell!")
                .style(AttributedStyle.BOLD.foreground(AttributedStyle.YELLOW));
        builder.append("\nType 'help' for available commands or 'exit' to close the session.")
                .style(AttributedStyle.BOLD.foreground(AttributedStyle.GREEN));
        builder.append("\n\n");
        terminal.writer().print(builder.toAttributedString());
    }

    private void displayGoodbyeMessage() {
        AttributedStringBuilder builder = new AttributedStringBuilder();
        builder.append("\nGoodbye! Connection closed.")
                .style(AttributedStyle.BOLD.foreground(AttributedStyle.YELLOW));
        builder.append("\n");
        terminal.writer().print(builder.toAttributedString());
    }
} 