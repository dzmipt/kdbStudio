package studio.qeditor;

import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.TokenMaker;
import studio.kdb.config.ColorToken;

import java.util.Optional;
import java.util.stream.Stream;

public class QTokenMakerFactory extends AbstractTokenMakerFactory {

    private final static String CONTENT_TYPE_PREFIX = RSTokenMaker.CONTENT_TYPE + "-";

    public final static QTokenMakerFactory INSTANCE = new QTokenMakerFactory();

    @Override
    protected void initTokenMakerMap() {
    }

    @Override
    protected TokenMaker getTokenMakerImpl(String key) {
        if (key.equals(RSTokenMaker.CONTENT_TYPE)) return new RSTokenMaker();
        if (! key.startsWith(CONTENT_TYPE_PREFIX)) return null;

        String colorDescription = key.substring(CONTENT_TYPE_PREFIX.length());

        Optional<ColorToken> optColorToken = Stream.of(ColorToken.values())
                .filter(token -> token.getDescription().equals(colorDescription))
                .findFirst();

        if (optColorToken.isEmpty()) return null;
        ColorToken colorToken = optColorToken.get();

        Optional<RSToken> optRSToken = Stream.of(RSToken.values())
                .filter(token -> token.getColorToken() == colorToken)
                .findFirst();
        if (optRSToken.isEmpty()) return null;

        return new OneTokenMaker(optRSToken.get());
    }

    public static String getContentType(ColorToken colorToken) {
        return CONTENT_TYPE_PREFIX + colorToken.getDescription();
    }
}
