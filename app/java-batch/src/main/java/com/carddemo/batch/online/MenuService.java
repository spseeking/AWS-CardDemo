package com.carddemo.batch.online;

import org.springframework.stereotype.Service;

import java.util.List;

/** COMEN01C and COADM01C: the option lists of copybooks COMEN02Y and COADM02Y. */
@Service
public class MenuService {

    /** One 3270 menu line: option number, description and the program it transferred to. */
    public record MenuOption(int number, String name, String program) {
    }

    private static final List<MenuOption> MAIN = List.of(
            new MenuOption(1, "Account View", "COACTVWC"),
            new MenuOption(2, "Account Update", "COACTUPC"),
            new MenuOption(3, "Credit Card List", "COCRDLIC"),
            new MenuOption(4, "Credit Card View", "COCRDSLC"),
            new MenuOption(5, "Credit Card Update", "COCRDUPC"),
            new MenuOption(6, "Transaction List", "COTRN00C"),
            new MenuOption(7, "Transaction View", "COTRN01C"),
            new MenuOption(8, "Transaction Add", "COTRN02C"),
            new MenuOption(9, "Transaction Reports", "CORPT00C"),
            new MenuOption(10, "Bill Payment", "COBIL00C"),
            new MenuOption(11, "Pending Authorization View", "COPAUS0C"));

    private static final List<MenuOption> ADMIN = List.of(
            new MenuOption(1, "User List (Security)", "COUSR00C"),
            new MenuOption(2, "User Add (Security)", "COUSR01C"),
            new MenuOption(3, "User Update (Security)", "COUSR02C"),
            new MenuOption(4, "User Delete (Security)", "COUSR03C"),
            new MenuOption(5, "Transaction Type List/Update (Db2)", "COTRTLIC"),
            new MenuOption(6, "Transaction Type Maintenance (Db2)", "COTRTUPC"));

    public List<MenuOption> menuFor(String userType) {
        return SignOnService.ADMIN_TYPE.equalsIgnoreCase(userType) ? ADMIN : MAIN;
    }

    /** COMEN01C rejects anything outside the option range with the same message for both menus. */
    public MenuOption select(String userType, int option) {
        return menuFor(userType).stream()
                .filter(menuOption -> menuOption.number() == option)
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("Please enter a valid option number..."));
    }
}
