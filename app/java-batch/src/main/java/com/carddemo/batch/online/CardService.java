package com.carddemo.batch.online;

import com.carddemo.batch.domain.Card;
import com.carddemo.batch.store.KeyedRecordStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** COCRDLIC (list), COCRDSLC (view) and COCRDUPC (update) against CARDDAT. */
@Service
public class CardService {

    /** The updatable part of the COCRDUPC screen. */
    public record CardUpdate(String embossedName, String expirationDate, String activeStatus) {
    }

    private final CardDemoRepository repository;

    public CardService(CardDemoRepository repository) {
        this.repository = repository;
    }

    /** COCRDLIC lists every card, or only the cards of one account when the filter is supplied. */
    public List<Card> list(String accountId) {
        Long filter = accountId == null || accountId.isBlank() ? null : validateAccountId(accountId);
        List<Card> cards = new ArrayList<>();
        for (Card card : repository.cards().values()) {
            if (filter == null || card.accountId() == filter) {
                cards.add(card);
            }
        }
        return cards;
    }

    public Card view(String cardNumber) {
        return repository.cards().find(validateCardNumber(cardNumber))
                .orElseThrow(() -> new BusinessRuleException("Card not found in CARDDAT"));
    }

    public Card update(String cardNumber, CardUpdate update) {
        KeyedRecordStore<Card> cards = repository.cards();
        Card current = cards.find(validateCardNumber(cardNumber))
                .orElseThrow(() -> new BusinessRuleException("Card not found in CARDDAT"));

        if (update.embossedName() == null || update.embossedName().isBlank()) {
            throw new BusinessRuleException("Card name can NOT be empty...");
        }
        if (!update.embossedName().matches("[A-Za-z ]+")) {
            throw new BusinessRuleException("Card name can only contain alphabets and spaces");
        }
        if (update.expirationDate() == null || !update.expirationDate().matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new BusinessRuleException("Card expiry date should be in format YYYY-MM-DD");
        }
        int month = Integer.parseInt(update.expirationDate().substring(5, 7));
        if (month < 1 || month > 12) {
            throw new BusinessRuleException("Card expiry month must be between 1 and 12");
        }
        if (!"Y".equalsIgnoreCase(update.activeStatus()) && !"N".equalsIgnoreCase(update.activeStatus())) {
            throw new BusinessRuleException("Card Active Status must be Y or N");
        }

        Card updated = new Card(current.cardNumber(), current.accountId(), current.cvvCode(),
                update.embossedName().toUpperCase(Locale.ROOT), update.expirationDate(),
                update.activeStatus().toUpperCase(Locale.ROOT), current.filler());
        if (updated.format().equals(current.format())) {
            throw new BusinessRuleException("Please modify to update ...");
        }
        cards.put(updated);
        cards.save();
        return updated;
    }

    private static String validateCardNumber(String cardNumber) {
        String value = cardNumber == null ? "" : cardNumber.trim();
        if (!value.matches("\\d{16}")) {
            throw new BusinessRuleException("Card number must be a 16 digit number");
        }
        return value;
    }

    private static long validateAccountId(String accountId) {
        String value = accountId.trim();
        if (!value.matches("\\d{1,11}") || Long.parseLong(value) == 0L) {
            throw new BusinessRuleException("Account number must be a non zero 11 digit number");
        }
        return Long.parseLong(value);
    }
}
