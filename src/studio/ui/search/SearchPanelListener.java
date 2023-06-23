package studio.ui.search;

import org.fife.ui.rtextarea.SearchContext;

public interface SearchPanelListener {

    void search(SearchContext context, SearchAction action);
    void closeSearchPanel();
}
