package com.carddemo.batch.domain;

import com.carddemo.batch.copybook.FixedWidth;
import com.carddemo.batch.copybook.ZonedDecimal;

import java.math.BigDecimal;

/**
 * TRAN-RECORD / DALYTRAN-RECORD (copybooks CVTRA05Y and CVTRA06Y, RECLN 350).
 *
 * <p>Both copybooks describe an identical layout, so one type covers the daily input file, the
 * transaction master and the reject record payload.
 */
public class TransactionRecord {

    public static final int LENGTH = 350;

    private String id = "";
    private String typeCode = "";
    private int categoryCode;
    private String source = "";
    private String description = "";
    private BigDecimal amount = BigDecimal.ZERO.setScale(2);
    private long merchantId;
    private String merchantName = "";
    private String merchantCity = "";
    private String merchantZip = "";
    private String cardNumber = "";
    private String originTimestamp = "";
    private String processTimestamp = "";
    private String filler = "";

    public static TransactionRecord parse(String record) {
        TransactionRecord tran = new TransactionRecord();
        tran.id = FixedWidth.alphanumeric(record, 0, 16);
        tran.typeCode = FixedWidth.alphanumeric(record, 16, 2);
        tran.categoryCode = (int) FixedWidth.number(record, 18, 4);
        tran.source = FixedWidth.alphanumeric(record, 22, 10);
        tran.description = FixedWidth.alphanumeric(record, 32, 100);
        tran.amount = FixedWidth.zoned(record, 132, 11, 2);
        tran.merchantId = FixedWidth.number(record, 143, 9);
        tran.merchantName = FixedWidth.alphanumeric(record, 152, 50);
        tran.merchantCity = FixedWidth.alphanumeric(record, 202, 50);
        tran.merchantZip = FixedWidth.alphanumeric(record, 252, 10);
        tran.cardNumber = FixedWidth.alphanumeric(record, 262, 16);
        tran.originTimestamp = FixedWidth.alphanumeric(record, 278, 26);
        tran.processTimestamp = FixedWidth.alphanumeric(record, 304, 26);
        tran.filler = FixedWidth.alphanumeric(record, 330, 20);
        return tran;
    }

    public String format() {
        return FixedWidth.chars(id, 16)
                + FixedWidth.chars(typeCode, 2)
                + FixedWidth.digits(categoryCode, 4)
                + FixedWidth.chars(source, 10)
                + FixedWidth.chars(description, 100)
                + ZonedDecimal.format(amount, 11, 2)
                + FixedWidth.digits(merchantId, 9)
                + FixedWidth.chars(merchantName, 50)
                + FixedWidth.chars(merchantCity, 50)
                + FixedWidth.chars(merchantZip, 10)
                + FixedWidth.chars(cardNumber, 16)
                + FixedWidth.chars(originTimestamp, 26)
                + FixedWidth.chars(processTimestamp, 26)
                + FixedWidth.chars(filler, 20);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public void setTypeCode(String typeCode) {
        this.typeCode = typeCode;
    }

    public int getCategoryCode() {
        return categoryCode;
    }

    public void setCategoryCode(int categoryCode) {
        this.categoryCode = categoryCode;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(long merchantId) {
        this.merchantId = merchantId;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public String getMerchantCity() {
        return merchantCity;
    }

    public void setMerchantCity(String merchantCity) {
        this.merchantCity = merchantCity;
    }

    public String getMerchantZip() {
        return merchantZip;
    }

    public void setMerchantZip(String merchantZip) {
        this.merchantZip = merchantZip;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getOriginTimestamp() {
        return originTimestamp;
    }

    public void setOriginTimestamp(String originTimestamp) {
        this.originTimestamp = originTimestamp;
    }

    public String getProcessTimestamp() {
        return processTimestamp;
    }

    public void setProcessTimestamp(String processTimestamp) {
        this.processTimestamp = processTimestamp;
    }

    public String getFiller() {
        return filler;
    }

    public void setFiller(String filler) {
        this.filler = filler;
    }

    /** The CBTRN01C DISPLAY of the daily transaction record. */
    @Override
    public String toString() {
        return "TRAN-ID=" + id.trim()
                + " TYPE-CD=" + typeCode
                + " CAT-CD=" + categoryCode
                + " SOURCE=" + source.trim()
                + " DESC=" + description.trim()
                + " AMT=" + amount
                + " MERCHANT-ID=" + merchantId
                + " MERCHANT-NAME=" + merchantName.trim()
                + " MERCHANT-CITY=" + merchantCity.trim()
                + " MERCHANT-ZIP=" + merchantZip.trim()
                + " CARD-NUM=" + cardNumber.trim()
                + " ORIG-TS=" + originTimestamp.trim()
                + " PROC-TS=" + processTimestamp.trim();
    }
}
