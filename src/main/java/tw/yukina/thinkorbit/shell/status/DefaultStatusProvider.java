package tw.yukina.thinkorbit.shell.status;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.utils.Status;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultStatusProvider implements ShellStatusProvider {
    private final Status status;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger taskCount = new AtomicInteger(0);
    private Thread statusThread;

    public DefaultStatusProvider(Status status) {
        this.status = status;
    }

    @Override
    public AttributedString getStatus() {
        return new AttributedStringBuilder()
                .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE))
                .append("Connected to server | ")
                .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
                .append(Integer.toString(taskCount.get()))
                .append(" tasks running")
                .toAttributedString();
    }

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            statusThread = new Thread(() -> {
                try {
                    while (running.get()) {
                        Thread.sleep(2000);
                        taskCount.incrementAndGet();
                        if (status != null) {
                            status.update(Collections.singletonList(getStatus()));
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            statusThread.start();
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            if (statusThread != null) {
                statusThread.interrupt();
            }
        }
    }
} 