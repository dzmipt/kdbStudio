package studio.qeditor;

import javax.swing.text.Document;
import org.netbeans.editor.BaseDocument;
import org.netbeans.editor.Syntax;
import org.netbeans.editor.SyntaxSupport;
import org.netbeans.editor.ext.Completion;
import org.netbeans.editor.ext.ExtEditorUI;
import org.netbeans.editor.ext.ExtKit;


public class QKit extends ExtKit {

    public static final String CONTENT_TYPE = "text/q";

    public String getContentType() {
        return CONTENT_TYPE; // NOI18N
    }

    public Syntax createSyntax(Document document) {
        return new QSyntax();
    }
    
    public SyntaxSupport createSyntaxSupport(BaseDocument doc) {
        return new QSyntaxSupport(doc);
    }

    public Completion createCompletion(ExtEditorUI extEditorUI) {
        return new QCompletion(extEditorUI);
    }
}
