package tw.yukina.thinkorbit.command;

import org.jline.terminal.Terminal;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.springframework.stereotype.Component;
import tw.yukina.thinkorbit.service.command.Command;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * System-related commands
 */
@Component
public class SystemCommands {
    
    private static final DateTimeFormatter DATE_TIME_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    @Command(value = "date", description = "Display current date and time")
    public boolean dateCommand(String[] args, Terminal terminal) {
        LocalDateTime now = LocalDateTime.now();
        AttributedStringBuilder builder = new AttributedStringBuilder();
        builder.append("Current date and time: ", AttributedStyle.BOLD);
        builder.append(now.format(DATE_TIME_FORMATTER), 
                      AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));
        terminal.writer().println(builder.toAttributedString());
        return true;
    }
    
    @Command(value = "memory", description = "Display memory usage information", aliases = {"mem"})
    public boolean memoryCommand(String[] args, Terminal terminal) {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
        
        AttributedStringBuilder builder = new AttributedStringBuilder();
        builder.append("\nMemory Usage Information\n", 
                      AttributedStyle.BOLD.foreground(AttributedStyle.CYAN));
        builder.append("========================\n\n", 
                      AttributedStyle.BOLD.foreground(AttributedStyle.CYAN));
        
        // Heap memory
        builder.append("Heap Memory:\n", AttributedStyle.BOLD);
        builder.append(String.format("  Used:      %,d MB\n", heapUsage.getUsed() / 1024 / 1024));
        builder.append(String.format("  Committed: %,d MB\n", heapUsage.getCommitted() / 1024 / 1024));
        builder.append(String.format("  Max:       %,d MB\n", heapUsage.getMax() / 1024 / 1024));
        
        // Non-heap memory
        builder.append("\nNon-Heap Memory:\n", AttributedStyle.BOLD);
        builder.append(String.format("  Used:      %,d MB\n", nonHeapUsage.getUsed() / 1024 / 1024));
        builder.append(String.format("  Committed: %,d MB\n", nonHeapUsage.getCommitted() / 1024 / 1024));
        
        terminal.writer().print(builder.toAttributedString());
        return true;
    }
    
    @Command(value = "clear", description = "Clear the terminal screen", aliases = {"cls"})
    public boolean clearCommand(String[] args, Terminal terminal) {
        terminal.puts(org.jline.utils.InfoCmp.Capability.clear_screen);
        terminal.flush();
        return true;
    }
    
    @Command(value = "echo", description = "Echo the input text")
    public boolean echoCommand(String[] args, Terminal terminal) {
        if (args.length == 0) {
            terminal.writer().println();
        } else {
            terminal.writer().println(String.join(" ", args));
        }
        return true;
    }
} 