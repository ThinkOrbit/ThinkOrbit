package tw.yukina.thinkorbit.service.shell.highlighter;

import org.jline.reader.LineReader;
import org.jline.utils.AttributedString;

public interface ShellHighlighter {
    AttributedString highlight(LineReader reader, String buffer);
} 