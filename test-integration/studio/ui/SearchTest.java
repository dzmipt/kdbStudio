package studio.ui;

import org.assertj.swing.fixture.JPanelFixture;
import org.assertj.swing.fixture.JTextComponentFixture;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.junit.Before;
import org.junit.Test;
import studio.ui.search.Position;

import javax.swing.text.BadLocationException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.assertj.swing.edt.GuiActionRunner.execute;
import static org.junit.Assert.assertEquals;

public class SearchTest  extends StudioTest {

    static class Selection {
        Position start, end;
        Selection(int startRow, int startColumn, int endRow, int endColumn) {
            start = new Position(startRow, startColumn);
            end = new Position(endRow, endColumn);
        }
        Selection(Position start, Position end) {
            this.start = start;
            this.end = end;
        }
        @Override
        public boolean equals(Object obj) {
            if (! (obj instanceof Selection)) return false;
            Selection s = (Selection) obj;
            return start.equals(s.start) && end.equals(s.end);
        }
    }

    private Position getPositionFromOffset(RSyntaxTextArea textArea, int offset) throws BadLocationException {
        int line = textArea.getLineOfOffset(offset);
        int col = offset - textArea.getLineStartOffset(line);
        return new Position(line, col);
    }

    private Selection getSelection(RSyntaxTextArea textArea) throws BadLocationException {
        int startOffset = textArea.getSelectionStart();
        int endOffset = textArea.getSelectionEnd();
        return new Selection(
                getPositionFromOffset(textArea, startOffset),
                getPositionFromOffset(textArea, endOffset) );
    }

    private Selection getSelection(JTextComponentFixture editor) throws BadLocationException {
        return execute( () -> getSelection((RSyntaxTextArea) editor.target()) );
    }

    private String content;
    private JPanelFixture searchPanel;
    private JTextComponentFixture editor;

    @Before
    public void openFile() throws IOException {
        File textFile = new File(getClass().getClassLoader().getResource("searchText.q").getFile());
        content = new String(Files.readAllBytes(textFile.toPath()), StandardCharsets.UTF_8);

        openFile(textFile);
        editor = frameFixture.textBox("editor2");
        frameFixture.menuItem("Replace...").click();
        searchPanel = frameFixture.panel("SearchPanel");
    }

    @Test
    public void testSequentialSearch() throws BadLocationException {
        searchPanel.textBox("FindField").enterText("select");
        Selection selection = getSelection(editor);
        assertEquals(new Selection(1,0,1,6), selection);

        searchPanel.button("FindButton").click();
        selection = getSelection(editor);
        assertEquals(new Selection(3,0,3,6), selection);

        searchPanel.button("FindButton").click();
        selection = getSelection(editor);
        assertEquals(new Selection(4,4,4,10), selection);

        searchPanel.button("FindButton").click();
        selection = getSelection(editor);
        assertEquals(new Selection(1,0,1,6), selection);
    }

    @Test
    public void testSequentialSearchBack() throws BadLocationException {
        editor.selectText(70, 70);

        JPanelFixture searchPanel = frameFixture.panel("SearchPanel");
        searchPanel.textBox("FindField").setText("select");

        searchPanel.button("FindBackButton").click();
        Selection selection = getSelection(editor);
        assertEquals(new Selection(1,0,1,6), selection);

        searchPanel.button("FindBackButton").click();
        selection = getSelection(editor);
        assertEquals(new Selection(4,4,4,10), selection);

        searchPanel.button("FindBackButton").click();
        selection = getSelection(editor);
        assertEquals(new Selection(3,0,3,6), selection);

        searchPanel.button("FindBackButton").click();
        selection = getSelection(editor);
        assertEquals(new Selection(1,0,1,6), selection);
    }


    private String replace(String src, String what, String to, int at) {
        int index = src.indexOf(what, at);
        if (index == -1) return src;
        return src.substring(0, index) + to + src.substring(index + what.length());
    }

    @Test
    public void testSequentialReplace() throws BadLocationException, IOException {
        editor.selectText(70, 70);

        searchPanel.textBox("FindField").setText("select");
        searchPanel.textBox("ReplaceField").setText("xxx");

        searchPanel.button("ReplaceButton").click();
        content = replace(content, "select", "xxx", 70);
        editor.requireText(content);

        searchPanel.button("ReplaceButton").click();
        content = replace(content, "select", "xxx", 70);
        editor.requireText(content);

        searchPanel.button("ReplaceButton").click();
        content = replace(content, "select", "xxx", 0);
        editor.requireText(content);

        searchPanel.button("ReplaceButton").click();
        editor.requireText(content);
    }

    @Test(timeout = 3000)
    public void testSequentialReplaceAllRecursive() throws InterruptedException {
        frameFixture.menuItem("Replace...").click();

        searchPanel.textBox("FindField").setText("select");
        searchPanel.textBox("ReplaceField").setText("select i,");

        searchPanel.button("ReplaceAllButton").click();
        editor.requireText(content.replace("select", "select i,"));
    }

}
