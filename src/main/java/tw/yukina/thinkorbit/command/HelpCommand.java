package tw.yukina.thinkorbit.command;

import org.jline.terminal.Terminal;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import tw.yukina.thinkorbit.service.command.Command;
import tw.yukina.thinkorbit.service.command.CommandHandler;
import tw.yukina.thinkorbit.service.command.CommandRegistry;

import java.util.Set;
import java.util.TreeSet;

/**
 * Help command to list all available commands
 */
@Component
@Command(value = "help", description = "Display help information", aliases = {"?", "h"})
public class HelpCommand implements CommandHandler {
    
    private final CommandRegistry commandRegistry;
    
    public HelpCommand(@Lazy CommandRegistry commandRegistry) {
        this.commandRegistry = commandRegistry;
    }
    
    @Override
    public boolean execute(String[] args, Terminal terminal) {
        if (args.length > 0) {
            // Show help for specific command
            String commandName = args[0];
            String description = commandRegistry.getCommandDescription(commandName);
            
            if (description != null) {
                terminal.writer().println("Command: " + commandName);
                terminal.writer().println("Description: " + description);
            } else {
                terminal.writer().println("Unknown command: " + commandName);
            }
        } else {
            // Show all commands
            AttributedStringBuilder builder = new AttributedStringBuilder();
            builder.append("\nAvailable Commands:\n", AttributedStyle.BOLD.foreground(AttributedStyle.CYAN));
            builder.append("==================\n\n", AttributedStyle.BOLD.foreground(AttributedStyle.CYAN));
            
            Set<String> commandNames = new TreeSet<>(commandRegistry.getCommandNames());
            for (String commandName : commandNames) {
                String description = commandRegistry.getCommandDescription(commandName);
                
                builder.append(String.format("  %-15s", commandName), 
                             AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));
                if (description != null && !description.isEmpty()) {
                    builder.append(" - " + description, AttributedStyle.DEFAULT);
                }
                builder.append("\n");
            }
            
            builder.append("\nType 'help <command>' for more information about a specific command.\n",
                          AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
            
            terminal.writer().print(builder.toAttributedString());
        }
        
        return true;
    }
} 