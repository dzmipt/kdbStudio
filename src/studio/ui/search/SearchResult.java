package studio.ui.search;

import java.util.regex.Matcher;

public class SearchResult {

    private final Matcher matcher;

    public final static SearchResult NOT_FOUND = new SearchResult(null);

    public SearchResult(Matcher matcher) {
        this.matcher = matcher;
    }

    public boolean found() {
        return matcher != null;
    }

    public Matcher matcher() {
        if (! found()) throw new IllegalStateException("No matcher for the SearchResult");
        return matcher;
    }

}
