package studio.utils;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class TmpfileOutputStream extends OutputStream {
    private static final Logger log = LogManager.getLogger();

    private final Path path;
    private final Path tmpFile;
    private final FileOutputStream outputStream;
    private boolean closed = false;

    public TmpfileOutputStream(Path path) throws IOException {
        this.path = path;
        tmpFile = path.resolveSibling(FilenameUtils.getName(path.toString())
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
        if (closed) return;

        outputStream.close();
        Files.move(tmpFile, path, REPLACE_EXISTING);
        log.debug("moved {} -> {}", tmpFile.toString(), path);
        closed = true;
    }

}
