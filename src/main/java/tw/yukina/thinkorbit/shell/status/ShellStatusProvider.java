package tw.yukina.thinkorbit.shell.status;

import org.jline.utils.AttributedString;

public interface ShellStatusProvider {
    AttributedString getStatus();
    void start();
    void stop();
} 