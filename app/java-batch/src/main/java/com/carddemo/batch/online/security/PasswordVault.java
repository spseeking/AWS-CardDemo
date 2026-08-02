package com.carddemo.batch.online.security;

import com.carddemo.batch.config.BatchFilesProperties;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hashed sign on credentials held beside USRSEC.
 *
 * <p>SEC-USR-PWD is an 8 byte clear text field, so a hash cannot live in the copybook record
 * without changing its layout. The hashes are kept in a sidecar file keyed on SEC-USR-ID instead:
 * USRSEC stays byte compatible with the mainframe extract, and a user whose hash is written has
 * the clear password blanked out of the record.
 */
@Component
public class PasswordVault {

    private final Path path;
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    private final Map<String, String> hashes = new ConcurrentHashMap<>();

    public PasswordVault(BatchFilesProperties files) {
        this.path = files.getUserCredentialFile();
        load();
    }

    private void load() {
        if (!Files.exists(path)) {
            return;
        }
        try {
            for (String line : Files.readAllLines(path, StandardCharsets.ISO_8859_1)) {
                int separator = line.indexOf(':');
                if (separator > 0) {
                    hashes.put(line.substring(0, separator), line.substring(separator + 1));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to load " + path, e);
        }
    }

    public boolean hasHash(String userId) {
        return hashes.containsKey(userId);
    }

    public boolean matches(String userId, String password) {
        String hash = hashes.get(userId);
        return hash != null && encoder.matches(password, hash);
    }

    public void store(String userId, String password) {
        hashes.put(userId, encoder.encode(password));
        save();
    }

    public void remove(String userId) {
        if (hashes.remove(userId) != null) {
            save();
        }
    }

    private void save() {
        List<String> lines = hashes.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .toList();
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.write(path, lines, StandardCharsets.ISO_8859_1);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to write " + path, e);
        }
    }
}
