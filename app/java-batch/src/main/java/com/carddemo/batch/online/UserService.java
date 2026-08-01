package com.carddemo.batch.online;

import com.carddemo.batch.domain.SecurityUser;
import com.carddemo.batch.store.KeyedRecordStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** COUSR00C list, COUSR01C add, COUSR02C update and COUSR03C delete of the USRSEC records. */
@Service
public class UserService {

    private final CardDemoRepository repository;

    public UserService(CardDemoRepository repository) {
        this.repository = repository;
    }

    public List<SecurityUser> list() {
        List<SecurityUser> users = new ArrayList<>();
        repository.users().values().forEach(users::add);
        return users;
    }

    public SecurityUser find(String userId) {
        return repository.users().find(key(userId))
                .orElseThrow(() -> new BusinessRuleException("User ID NOT found..."));
    }

    public SecurityUser add(String userId, String firstName, String lastName, String password, String type) {
        validate(userId, firstName, lastName, password, type);
        KeyedRecordStore<SecurityUser> users = repository.users();
        if (users.find(key(userId)).isPresent()) {
            throw new BusinessRuleException("User ID already exist...");
        }
        SecurityUser user = new SecurityUser(key(userId), firstName, lastName, password, type.toUpperCase(
                Locale.ROOT), "");
        users.put(user);
        users.save();
        return user;
    }

    /** COUSR02C rewrites the record only when at least one field actually changed. */
    public SecurityUser update(String userId, String firstName, String lastName, String password, String type) {
        validate(userId, firstName, lastName, password, type);
        KeyedRecordStore<SecurityUser> users = repository.users();
        SecurityUser current = users.find(key(userId))
                .orElseThrow(() -> new BusinessRuleException("User ID NOT found..."));
        SecurityUser updated = new SecurityUser(current.userId(), firstName, lastName, password,
                type.toUpperCase(Locale.ROOT), current.filler());
        if (updated.format().equals(current.format())) {
            throw new BusinessRuleException("Please modify to update ...");
        }
        users.put(updated);
        users.save();
        return updated;
    }

    public void delete(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new BusinessRuleException("User ID can NOT be empty...");
        }
        KeyedRecordStore<SecurityUser> users = repository.users();
        SecurityUser user = users.find(key(userId))
                .orElseThrow(() -> new BusinessRuleException("User ID NOT found..."));
        users.remove(key(user.userId()));
        users.save();
    }

    private static void validate(String userId, String firstName, String lastName, String password,
                                 String type) {
        if (isEmpty(firstName)) {
            throw new BusinessRuleException("First Name can NOT be empty...");
        }
        if (isEmpty(lastName)) {
            throw new BusinessRuleException("Last Name can NOT be empty...");
        }
        if (isEmpty(userId)) {
            throw new BusinessRuleException("User ID can NOT be empty...");
        }
        if (isEmpty(password)) {
            throw new BusinessRuleException("Password can NOT be empty...");
        }
        if (isEmpty(type)) {
            throw new BusinessRuleException("User Type can NOT be empty...");
        }
        // the 3270 map could not send more than the copybook field widths; REST callers can
        if (userId.trim().length() > 8) {
            throw new BusinessRuleException("User ID must be 8 characters or less...");
        }
        if (password.trim().length() > 8) {
            throw new BusinessRuleException("Password must be 8 characters or less...");
        }
        if (firstName.trim().length() > 20 || lastName.trim().length() > 20) {
            throw new BusinessRuleException("Name must be 20 characters or less...");
        }
        if (type.trim().length() > 1) {
            throw new BusinessRuleException("User Type must be A or U...");
        }
    }

    private static boolean isEmpty(String value) {
        return value == null || value.isBlank();
    }

    private static String key(String userId) {
        return userId == null ? "" : userId.trim().toUpperCase(Locale.ROOT);
    }
}
