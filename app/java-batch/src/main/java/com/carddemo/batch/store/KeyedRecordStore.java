package com.carddemo.batch.store;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Stand-in for a VSAM KSDS: a fixed-width flat file loaded into a key-ordered map, updated in
 * place and rewritten on {@link #save()}. Keeps the converted batch jobs runnable against the
 * repository's sample data without a database; the intent is that a JDBC-backed implementation
 * replaces it once the data layer is migrated.
 */
public class KeyedRecordStore<T> {

    private final Path path;
    private final Function<T, String> keyExtractor;
    private final Function<T, String> formatter;
    private final Map<String, T> records = new HashMap<>();

    public KeyedRecordStore(Path path, Function<String, T> parser, Function<T, String> keyExtractor,
                            Function<T, String> formatter) {
        this.path = path;
        this.keyExtractor = keyExtractor;
        this.formatter = formatter;
        load(parser);
    }

    /** A file that does not exist yet is an empty dataset, like a newly defined VSAM cluster. */
    private void load(Function<String, T> parser) {
        if (!Files.exists(path)) {
            return;
        }
        try (Stream<String> lines = Files.lines(path, StandardCharsets.ISO_8859_1)) {
            lines.map(line -> line.replace("\r", ""))
                    .filter(line -> !line.isBlank())
                    .map(parser)
                    .forEach(record -> records.put(keyExtractor.apply(record), record));
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to load " + path, e);
        }
    }

    public Optional<T> find(String key) {
        return Optional.ofNullable(records.get(key));
    }

    public void put(T record) {
        records.put(keyExtractor.apply(record), record);
    }

    public void remove(String key) {
        records.remove(key);
    }

    public int size() {
        return records.size();
    }

    /**
     * Records in key sequence, the order a VSAM browse returns them. Every one of these clusters is
     * keyed on the leading field of its record, so ordering on the formatted record orders on the
     * key without each caller having to describe the key layout.
     */
    public Iterable<T> values() {
        List<T> ordered = new ArrayList<>(records.values());
        ordered.sort((left, right) -> formatter.apply(left).compareTo(formatter.apply(right)));
        return ordered;
    }

    public void save() {
        StringBuilder content = new StringBuilder();
        for (T record : values()) {
            content.append(formatter.apply(record)).append(System.lineSeparator());
        }
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.writeString(path, content.toString(), StandardCharsets.ISO_8859_1);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to write " + path, e);
        }
    }
}
