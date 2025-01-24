package studio.utils;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class TmpfileOutputStream extends OutputStream {
    private static final Logger log = LogManager.getLogger();

    private final String filename;
    private final Path tmpFile;
    private final FileOutputStream outputStream;

    public TmpfileOutputStream(String filename) throws IOException {
        this.filename = filename;
        tmpFile = Paths.get(filename).resolveSibling(FilenameUtils.getName(filename)
                        + "." + System.currentTimeMillis() + ".tmp");
        log.debug("Saving to tmp file {}", tmpFile.toString());
        Files.deleteIfExists(tmpFile);
        outputStream = new FileOutputStream(tmpFile.toFile());
    }

    @Override
    public void write(byte[] b) throws IOException {
        outputStream.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        outputStream.write(b, off, len);
    }

    @Override
    public void write(int b) throws IOException {
        outputStream.write(b);
    }

    @Override
    public void flush() throws IOException {
        outputStream.flush();
    }

    @Override
    public void close() throws IOException {
        outputStream.close();
    }

    public void writeCompleted() throws IOException {
        outputStream.close();
        Files.move(tmpFile, Paths.get(filename), REPLACE_EXISTING);
        log.debug("moved {} -> {}", tmpFile.toString(), filename);
    }

}
