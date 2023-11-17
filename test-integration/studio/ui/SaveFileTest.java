package studio.ui;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class SaveFileTest extends StudioTest {

    @Test
    public void saveTest() throws IOException {

        File file = File.createTempFile("kdbStudioSaveFile", ".q");
        Files.delete(file.toPath());
        FileChooser.mock(file);

        frameFixture.textBox("editor1").enterText("s");
        frameFixture.menuItem("Save").click();

        String content = Files.lines(file.toPath()).collect(Collectors.joining("\n"));
        assertEquals("s", content);

        frameFixture.textBox("editor1").enterText("q");
        frameFixture.menuItem("Save").click();

        content = Files.lines(file.toPath()).collect(Collectors.joining("\n"));
        assertEquals("sq", content);

    }
}
