package tw.yukina.thinkorbit.shell.highlighter;

import org.jline.reader.LineReader;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SqlHighlighter implements ShellHighlighter {
    private final Pattern SQL_KEYWORDS = Pattern.compile(
            "\\b(SELECT|FROM|WHERE|JOIN|ON|GROUP BY|ORDER BY|HAVING|INSERT|UPDATE|DELETE|CREATE|DROP|ALTER)\\b",
            Pattern.CASE_INSENSITIVE);

    @Override
    public AttributedString highlight(LineReader reader, String buffer) {
        AttributedStringBuilder builder = new AttributedStringBuilder();

        Matcher matcher = SQL_KEYWORDS.matcher(buffer);
        int lastEnd = 0;

        while (matcher.find()) {
            builder.append(buffer.substring(lastEnd, matcher.start()));
            builder.styled(
                    AttributedStyle.BOLD.foreground(AttributedStyle.BLUE),
                    buffer.substring(matcher.start(), matcher.end()));
            lastEnd = matcher.end();
        }

        if (lastEnd < buffer.length()) {
            builder.append(buffer.substring(lastEnd));
        }

        return builder.toAttributedString();
    }
} 