package studio.ui.search;

import org.fife.ui.rtextarea.SearchContext;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


public class SearchEngine {

    private String what;
    private final boolean wholeWord;
    private final boolean matchCase;
    private Pattern pattern = null;

    public SearchEngine(SearchContext context) throws PatternSyntaxException {
        what = context.getSearchFor();
        matchCase = context.getMatchCase();
        wholeWord = context.getWholeWord();

        if (context.isRegularExpression()) {
            pattern = Pattern.compile(what, matchCase ? 0 : Pattern.CASE_INSENSITIVE);
        } else {
            if (!matchCase) what = what.toLowerCase();
        }

    }

    public boolean containsIn(String text) {

        Matcher matcher = null;
        if (pattern != null) {
            matcher = pattern.matcher(text);
        } else {
            if (!matchCase) {
                text = text.toLowerCase();
            }
        }

        int pos = 0;
        while (pos<text.length()) {
            int start, end;
            if (matcher != null) {
                if (!matcher.find(pos)) return false;
                start = matcher.start();
                end = matcher.end();
            } else {
                start = text.indexOf(what, pos);
                if (start == -1) return false;
                end = start + what.length();
            }

            if (!wholeWord) {
                return true;
            } else {
                boolean startWord = true;
                if (start > 0) {
                    startWord = !Character.isLetterOrDigit(text.charAt(start - 1));
                }
                boolean endWord = true;
                if (end < text.length()) {
                    endWord = !Character.isLetterOrDigit(text.charAt(end));
                }
                if (startWord && endWord) return true;
            }
            pos = end;
        }

        return false;
    }
}
