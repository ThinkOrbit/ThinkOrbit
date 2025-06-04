package tw.yukina.thinkorbit.service.shell;

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
import org.jline.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tw.yukina.thinkorbit.service.command.CommandRegistry;
import tw.yukina.thinkorbit.service.shell.highlighter.ShellHighlighter;
import tw.yukina.thinkorbit.service.shell.status.ShellStatusProvider;

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

//            Status status = Status.getStatus(terminal);
//            if (status != null && statusProvider != null) {
//                statusProvider.start();
//            }

            terminal.puts(InfoCmp.Capability.clear_screen);
            getWelcomeMessage().println(terminal);

            while (!terminated) {
                try {
                    String line = reader.readLine("BIOS> ");

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
        terminal.flush();

        terminated = true;
        if (statusProvider != null) {
            statusProvider.stop();
        }

        try {
            params.getSession().close();
            terminal.close();
        } catch (Exception e) {
            logger.error("Error closing terminal", e);
        }
    }

    private AttributedString getWelcomeMessage() {
        AttributedStringBuilder builder = new AttributedStringBuilder();
        builder.style(AttributedStyle.BOLD.foreground(AttributedStyle.YELLOW)).append("Welcome to the SSH shell!");
        builder.style(AttributedStyle.BOLD.foreground(AttributedStyle.GREEN)).append("\nType 'help' for available commands or 'exit' to close the session.");
        return builder.toAttributedString();
    }
}