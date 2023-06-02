package studio.ui.rstextarea;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.Gutter;

import java.awt.*;

public class StudioRSyntaxTextArea extends RSyntaxTextArea {

    private Gutter gutter = null;

    public StudioRSyntaxTextArea(String text) {
        super(text);
    }

    @Override
    protected void handleReplaceSelection(String content) {
        //Probably this is a dirty hack, but here is we convert character 160 to space
        //which is pasted by Skype for Business and result in 'char error from kdb
        content = content.replace((char)160,' ');
        super.handleReplaceSelection(content);
    }

    public void setGutter(Gutter gutter) {
        this.gutter = gutter;
    }

    @Override
    public void setFont(Font font) {
        super.setFont(font);
        if (gutter != null) gutter.setLineNumberFont(font);
    }
}
