package studio.ui.search;

import org.fife.ui.rtextarea.SearchContext;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


public class SearchEngine {

    private final boolean wholeWord;
    private final Pattern pattern;

    public SearchEngine(SearchContext context) throws PatternSyntaxException {
        wholeWord = context.getWholeWord();

        int flags = context.getMatchCase() ? 0 : Pattern.CASE_INSENSITIVE;
        flags |= context.isRegularExpression() ? 0 : Pattern.LITERAL;
        pattern = Pattern.compile(context.getSearchFor(), flags);

    }

    public SearchResult search(String text) {
        return search(text, 0);
    }

    public SearchResult search(String text, int from) {
        Matcher matcher = pattern.matcher(text);

        int pos = from;
        while (pos<text.length()) {
            if (!matcher.find(pos)) return SearchResult.NOT_FOUND;
            int start = matcher.start();
            int end = matcher.end();

            if (!wholeWord) {
                return new SearchResult(matcher);
            } else {
                boolean startWord = true;
                if (start > 0) {
                    startWord = !Character.isLetterOrDigit(text.charAt(start - 1));
                }
                boolean endWord = true;
                if (end < text.length()) {
                    endWord = !Character.isLetterOrDigit(text.charAt(end));
                }
                if (startWord && endWord) return new SearchResult(matcher);
            }
            pos = end;
        }

        return SearchResult.NOT_FOUND;
    }

    public boolean containsIn(String text) {
        return search(text).found();
    }
}
