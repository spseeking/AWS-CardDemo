package com.carddemo.batch.store;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Append-only fixed-width output file (the transaction master and the daily rejects file). */
public class SequentialRecordWriter implements AutoCloseable {

    private final BufferedWriter writer;
    private int count;

    public SequentialRecordWriter(Path path) {
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            this.writer = Files.newBufferedWriter(path, StandardCharsets.ISO_8859_1,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to open " + path, e);
        }
    }

    public void write(String record) {
        try {
            writer.write(record);
            writer.newLine();
            count++;
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to write record", e);
        }
    }

    public int getCount() {
        return count;
    }

    @Override
    public void close() {
        try {
            writer.close();
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to close writer", e);
        }
    }
}
