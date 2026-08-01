package com.carddemo.batch.statement;

import com.carddemo.batch.domain.Account;
import com.carddemo.batch.domain.Customer;
import com.carddemo.batch.domain.TransactionRecord;
import com.carddemo.batch.store.SequentialRecordWriter;

import java.math.BigDecimal;
import java.util.List;

/** The HTML-LINES half of CBSTM03A: one table per statement written to the HTMLFILE. */
class StatementHtmlWriter {

    private static final String CELL_SPAN = "<td colspan=\"3\" style=\"padding:0px 5px;background-color:#f2f2f2;\">";
    private static final String CELL_TITLE =
            "<td colspan=\"3\" style=\"padding:0px 5px;background-color:#33FFD1; text-align:center;\">";
    private static final String CELL_BANK =
            "<td colspan=\"3\" style=\"padding:0px 5px;background-color:#1d1d96b3;\">";
    private static final String CELL_ACCOUNT =
            "<td colspan=\"3\" style=\"padding:0px 5px;background-color:#FFAF33;\">";
    private static final String HEADER_ID =
            "<td style=\"width:25%; padding:0px 5px; background-color:#33FF5E; text-align:left;\">";
    private static final String HEADER_DETAILS =
            "<td style=\"width:55%; padding:0px 5px; background-color:#33FF5E; text-align:left;\">";
    private static final String HEADER_AMOUNT =
            "<td style=\"width:20%; padding:0px 5px; background-color:#33FF5E; text-align:right;\">";
    private static final String DATA_ID =
            "<td style=\"width:25%; padding:0px 5px; background-color:#f2f2f2; text-align:left;\">";
    private static final String DATA_DETAILS =
            "<td style=\"width:55%; padding:0px 5px; background-color:#f2f2f2; text-align:left;\">";
    private static final String DATA_AMOUNT =
            "<td style=\"width:20%; padding:0px 5px; background-color:#f2f2f2; text-align:right;\">";

    private final SequentialRecordWriter out;

    StatementHtmlWriter(SequentialRecordWriter out) {
        this.out = out;
    }

    void write(Customer customer, Account account, List<TransactionRecord> transactions) {
        header(account);
        row(CELL_SPAN, "<p style=\"font-size:16px\">" + customer.fullName() + "</p>");
        row(CELL_SPAN, "<p>" + customer.addressLine1().trim() + "</p>");
        row(CELL_SPAN, "<p>" + customer.addressLine2().trim() + "</p>");
        row(CELL_SPAN, "<p>" + (customer.addressLine3().trim() + " " + customer.stateCode().trim() + " "
                + customer.countryCode().trim() + " " + customer.zip().trim()).trim() + "</p>");
        row(CELL_TITLE, "<p style=\"font-size:16px\">Basic Details</p>");
        row(CELL_SPAN, "<p>Account ID: " + account.getAccountId() + "</p>");
        row(CELL_SPAN, "<p>Current Balance: " + account.getCurrentBalance().toPlainString() + "</p>");
        row(CELL_SPAN, "<p>FICO Score: " + customer.ficoScore() + "</p>");
        row(CELL_TITLE, "<p style=\"font-size:16px\">Transaction Summary</p>");

        out.write("<tr>");
        cell(HEADER_ID, "<p style=\"font-size:16px\">Tran ID</p>");
        cell(HEADER_DETAILS, "<p style=\"font-size:16px\">Tran Details</p>");
        cell(HEADER_AMOUNT, "<p style=\"font-size:16px\">Amount</p>");
        out.write("</tr>");

        BigDecimal total = BigDecimal.ZERO;
        for (TransactionRecord transaction : transactions) {
            out.write("<tr>");
            cell(DATA_ID, "<p>" + transaction.getId().trim() + "</p>");
            cell(DATA_DETAILS, "<p>" + transaction.getDescription().trim() + "</p>");
            cell(DATA_AMOUNT, "<p>" + transaction.getAmount().toPlainString() + "</p>");
            out.write("</tr>");
            total = total.add(transaction.getAmount());
        }
        row(CELL_SPAN, "<p>Total EXP: " + total.toPlainString() + "</p>");
        row(CELL_TITLE, "<h3>End of Statement</h3>");
        out.write("</table>");
        out.write("</body>");
        out.write("</html>");
    }

    private void header(Account account) {
        out.write("<!DOCTYPE html>");
        out.write("<html lang=\"en\">");
        out.write("<head>");
        out.write("<meta charset=\"utf-8\">");
        out.write("<title>HTML Table Layout</title>");
        out.write("</head>");
        out.write("<body style=\"margin:0px;\">");
        out.write("<table  align=\"center\" frame=\"box\" style=\"width:70%; font:12px Segoe UI,sans-serif;\">");
        out.write("<tr>");
        cell(CELL_BANK, "<p style=\"font-size:16px\">Bank of XYZ</p>", "<p>410 Terry Ave N</p>",
                "<p>Seattle WA 99999</p>");
        out.write("</tr>");
        out.write("<tr>");
        cell(CELL_ACCOUNT, "<h3>Statement for Account Number: " + account.getAccountId() + "</h3>");
        out.write("</tr>");
    }

    private void row(String cellStart, String content) {
        out.write("<tr>");
        cell(cellStart, content);
        out.write("</tr>");
    }

    private void cell(String cellStart, String... contents) {
        out.write(cellStart);
        for (String content : contents) {
            out.write(content);
        }
        out.write("</td>");
    }
}
