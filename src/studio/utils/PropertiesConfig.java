package studio.utils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

// The purposes at the moment is to have sorted keys in storing properties to disk
public class PropertiesConfig extends Properties {

    private final static Charset CHARSET = StandardCharsets.ISO_8859_1;
    public PropertiesConfig() {}

    @Override
    public void store(OutputStream out, String comments) throws IOException {
        byte[] lineSeparator = System.getProperty("line.separator").getBytes(CHARSET);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        super.store(buffer, comments);

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(buffer.toByteArray()), CHARSET) );

        List<String> lines = new ArrayList<>();
        for(;;) {
            String line = reader.readLine();
            if (line == null) break;
            if (line.startsWith("#")) {
                out.write(line.getBytes(CHARSET));
                out.write(lineSeparator);
            } else {
                lines.add(line);
            }
        }
        Collections.sort(lines);

        for (String line: lines) {
            out.write(line.getBytes(CHARSET));
            out.write(lineSeparator);
        }
    }

}
