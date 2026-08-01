package com.carddemo.batch.store;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
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
    private final Map<String, T> records = new LinkedHashMap<>();

    public KeyedRecordStore(Path path, Function<String, T> parser, Function<T, String> keyExtractor,
                            Function<T, String> formatter) {
        this.path = path;
        this.keyExtractor = keyExtractor;
        this.formatter = formatter;
        load(parser);
    }

    private void load(Function<String, T> parser) {
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

    public int size() {
        return records.size();
    }

    public Iterable<T> values() {
        return records.values();
    }

    public void save() {
        StringBuilder content = new StringBuilder();
        for (T record : records.values()) {
            content.append(formatter.apply(record)).append(System.lineSeparator());
        }
        try {
            Files.writeString(path, content.toString(), StandardCharsets.ISO_8859_1);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to write " + path, e);
        }
    }
}
