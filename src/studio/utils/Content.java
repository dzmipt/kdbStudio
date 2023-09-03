package studio.utils;

import studio.kdb.Config;

public class Content {
    private final String content;
    private LineEnding lineEnding = Config.getInstance().getEnum(Config.DEFAULT_LINE_ENDING);
    private boolean mixedLineEnding;
    private boolean tabsReplaced = false;

    public static Content NO_CONTENT = new Content("");

    public static Content newContent(String text, LineEnding lineEnding) {
        Content content = new Content(text);
        content.lineEnding = lineEnding;
        content.mixedLineEnding = false;
        return content;
    }

    public Content(String text) {
        boolean shouldReplaceTabs = Config.getInstance().getBoolean(Config.AUTO_REPLACE_TAB_ON_OPEN);
        char[] tab;
        if (shouldReplaceTabs) {
            int count = Config.getInstance().getInt(Config.EDITOR_TAB_SIZE);
            tab = new char[count];
            for (int i=0; i<count; tab[i++]=' ');
        } else {
            tab = new char[] {'\t'};
        }

        StringBuilder builder = new StringBuilder();
        int unixEndings = 0;
        int winEndings = 0;
        int macEndings = 0;

        boolean wasCR = false;
        int size = text.length();
        int index = 0;
        while(index < size) {
            char ch = text.charAt(index);

            if (wasCR) {
                wasCR = false;
                builder.append('\n');
                if (ch == '\n') {
                    winEndings++;
                } else {
                    macEndings++;
                    continue;
                }
            } else {
                if (ch == '\r') {
                    wasCR = true;
                } else {
                    if (ch == '\n') {
                        unixEndings++;
                    }

                    if (ch == '\t') {
                        tabsReplaced |= shouldReplaceTabs;
                        builder.append(tab);
                    } else {
                        builder.append(ch);
                    }
                }
            }
            index++;
        }
        if (wasCR) {
            builder.append('\n');
            macEndings++;
        }

        content = builder.toString();

        int count = 0;
        if (unixEndings > 0) count++;
        if (winEndings > 0) count++;
        if (macEndings > 0) count++;

        mixedLineEnding = count > 1;

        if (count>0) { // if no ending of line; lineEnding will leave default
            if (unixEndings >= winEndings && unixEndings >= macEndings)  lineEnding = LineEnding.Unix;
            else if (winEndings >= unixEndings && winEndings >= macEndings) lineEnding = LineEnding.Windows;
            else lineEnding = LineEnding.MacOS9;
        }
    }

    public String getContent() {
        return content;
    }

    public LineEnding getLineEnding() {
        return lineEnding;
    }

    public boolean hasMixedLineEnding() {
        return mixedLineEnding;
    }

    public boolean isModified() {
        return mixedLineEnding | tabsReplaced;
    }

    public static String convert(String unixText, LineEnding lineEnding) {
        return unixText.replace("\n", lineEnding.getChars());
    }
}
