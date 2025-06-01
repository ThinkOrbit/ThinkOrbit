package tw.yukina.thinkorbit.shell;


import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.jline.builtins.Completers;
import org.jline.builtins.ssh.Ssh;
import org.jline.reader.Completer;
import org.jline.reader.Highlighter;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.utils.Status;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jline.builtins.Completers.TreeCompleter.node;

public class TestShell {
    public static void main(String[] args) throws Exception {
        // Create a highlighter for SQL keywords
        Highlighter sqlHighlighter = new Highlighter() {
            // Pattern to match SQL keywords (case insensitive)
            private final Pattern SQL_KEYWORDS = Pattern.compile(
                    "\\b(SELECT|FROM|WHERE|JOIN|ON|GROUP BY|ORDER BY|HAVING|INSERT|UPDATE|DELETE|CREATE|DROP|ALTER)\\b",
                    Pattern.CASE_INSENSITIVE);

            @Override
            public AttributedString highlight(LineReader reader, String buffer) {
                AttributedStringBuilder builder = new AttributedStringBuilder();

                // Find all SQL keywords in the buffer
                Matcher matcher = SQL_KEYWORDS.matcher(buffer);
                int lastEnd = 0;

                while (matcher.find()) {
                    // Add text before the keyword with default style
                    builder.append(buffer.substring(lastEnd, matcher.start()));

                    // Add the keyword with bold blue style
                    builder.styled(
                            AttributedStyle.BOLD.foreground(AttributedStyle.BLUE),
                            buffer.substring(matcher.start(), matcher.end()));

                    lastEnd = matcher.end();
                }

                // Add any remaining text
                if (lastEnd < buffer.length()) {
                    builder.append(buffer.substring(lastEnd));
                }

                return builder.toAttributedString();
            }
        };


        // Create a shell consumer
        Consumer<Ssh.ShellParams> shellConsumer = (params) -> {
            // This code runs for each client that connects
            try {
                Terminal terminal = params.getTerminal();

                Completer treeCompleter = new Completers.TreeCompleter(
                        node("help", node("commands"), node("syntax")),
                        node(
                                "set",
                                node("color", node("red", "green", "blue")),
                                node("size", node("small", "medium", "large"))));

                LineReader reader = LineReaderBuilder.builder()
                        .terminal(terminal)
                        .highlighter(sqlHighlighter)
                        .completer(treeCompleter)
                        .option(LineReader.Option.MENU_COMPLETE, true)
                        .build();

                Status status = Status.getStatus(terminal);

                // Start a background thread to update the status
                new Thread(() -> {
                    try {
                        int taskCount = 0;
                        while (true) {
                            Thread.sleep(2000);
                            taskCount = (taskCount + 1) % 10;

                            if (status != null) {
                                status.update(Collections.singletonList(new AttributedStringBuilder()
                                        .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE))
                                        .append("Connected to server | ")
                                        .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
                                        .append(Integer.toString(taskCount))
                                        .append(" tasks running")
                                        .toAttributedString()));
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();

                // Read input normally
                while (true) {
                    AttributedStringBuilder builder = new AttributedStringBuilder();
                    builder.append("Welcome to the SSH shell!").style(AttributedStyle.BOLD.foreground(AttributedStyle.YELLOW));
                    builder.append("\nType 'exit' to close the session.").style(AttributedStyle.BOLD.foreground(AttributedStyle.GREEN));
                    builder.append("\n\n");

                    terminal.writer().print(builder.toAttributedString());

                    String line = reader.readLine("prompt> ");
                    System.out.println("You entered: " + line);

                    if (line.equals("exit")) {
                        params.getSession().close();
                        break;
                    }
                }
            } catch (Exception e) {
                Logger logger = org.slf4j.LoggerFactory.getLogger(TestShell.class);
                logger.error("Error in shell consumer", e);
            }
        };

        System.out.println("Starting SSH server...");
        Ssh ssh = new Ssh(
                shellConsumer,
                null,
                () -> {
                    SshServer server = SshServer.setUpDefaultServer();
                    server.setPasswordAuthenticator(
                            (username, password, session) -> "admin".equals(username) && "password".equals(password));
                    server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
                    return server;
                },
                null);

        ssh.sshd(System.out, System.err, new String[]{
                "--ip=127.0.0.1", // Default is 127.0.0.1 (localhost only)
                "start"
        });

        Thread.currentThread().join();
    }
}