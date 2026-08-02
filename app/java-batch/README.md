# CardDemo — Java conversion of the COBOL batch jobs and CICS transactions

Java 17 / Spring Boot 3 conversion: the batch programs become Spring Batch jobs, the CICS online
programs become REST services.

| JCL job  | COBOL program     | Spring Batch job              | Converted logic |
|:---------|:------------------|:------------------------------|:----------------|
| POSTTRAN | CBTRN02C          | `postTranJob`                 | Daily transaction validation, posting, rejects |
| INTCALC  | CBACT04C          | `intCalcJob`                  | Monthly interest accrual and account roll-up |
| TRANREPT | CBTRN03C          | `transactionReportJob`        | 133-byte transaction detail report with page/account/grand totals |
| CREASTMT | CBSTM03A/CBSTM03B | `statementJob`                | Plain text and HTML account statements |
| EXPORT   | CBEXPORT          | `customerExportJob`           | 500-byte branch migration records (DISPLAY + COMP + COMP-3) |
| IMPORT   | CBIMPORT          | `customerImportJob`           | Rebuilds the master files, counts unknown record types |
| PRTACCT  | CBACT01C          | `accountListingJob`           | Account master listing |
| PRTCARD  | CBACT02C          | `cardListingJob`              | Card master listing |
| PRTXREF  | CBACT03C          | `cardXrefListingJob`          | Cross reference listing |
| PRTCUST  | CBCUS01C          | `customerListingJob`          | Customer master listing |
| PRTTRAN  | CBTRN01C          | `dailyTransactionListingJob`  | Daily transaction listing |

| CICS program            | REST endpoint |
|:------------------------|:--------------|
| COSGN00C                | `POST /api/signon` |
| COMEN01C / COADM01C     | `GET /api/menu`, `GET /api/menu/{option}` |
| COACTVWC / COACTUPC     | `GET|PUT /api/accounts/{accountId}` |
| COCRDLIC/COCRDSLC/COCRDUPC | `GET /api/cards`, `GET|PUT /api/cards/{cardNumber}` |
| COTRN00C/COTRN01C/COTRN02C | `GET /api/transactions`, `GET /api/transactions/{id}`, `POST /api/transactions` |
| COBIL00C                | `GET|POST /api/billpay/{accountId}` |
| CORPT00C                | `POST /api/reports` (launches `transactionReportJob`) |
| COUSR00C-COUSR03C       | `GET|POST /api/users`, `GET|PUT|DELETE /api/users/{userId}` |

The screen validation messages are preserved verbatim (`Wrong Password. Try again ...`,
`Account number must be a non zero 11 digit number`, `You have nothing to pay...`) and returned as
`{"errorMessage": ...}` with HTTP 400.

The VSAM KSDS files are represented by the repository's fixed-width ASCII sample data
(`app/data/ASCII`), read and rewritten by `KeyedRecordStore`. That keeps the jobs runnable and
diff-comparable against mainframe output; the intended next step is to swap that store for a JDBC
implementation once the data layer is migrated.

## Business rules preserved

**Posting (`CBTRN02C` 1500-VALIDATE-TRAN / 2000-POST-TRANSACTION)**

| Reason | Condition |
|:-------|:----------|
| 100 | card number has no cross-reference record |
| 101 | cross-referenced account does not exist |
| 102 | `cycle credit − cycle debit + amount > credit limit` |
| 103 | account expiry date is before the transaction origin date (evaluated after 102, so it wins when both fail) |

Posted transactions add the amount to the transaction category balance (creating it when absent),
to `ACCT-CURR-BAL`, and to `ACCT-CURR-CYC-CREDIT` when non-negative or `ACCT-CURR-CYC-DEBIT`
otherwise. A run with rejects ends with exit status `COMPLETED WITH REJECTS`, mirroring
`RETURN-CODE 4`.

**Interest (`CBACT04C` 1200/1300)**

`monthly interest = category balance × annual rate ÷ 1200`, truncated to two decimals because the
COBOL receiving field is `PIC S9(09)V99` and the `COMPUTE` is not `ROUNDED`. The rate comes from
the disclosure group keyed by (account group, transaction type, transaction category), falling
back to the `DEFAULT` account group. Interest transactions are written as type `01`, category
`0005`, source `System`, id `<parm date><6-digit sequence>`. When the account changes, the accrued
interest is added to `ACCT-CURR-BAL` and both cycle totals are reset.

### Known divergence

`CBTRN03C` adds `TRAN-AMT` to the page and account totals once more at end of file, double counting
the last reported transaction (`carddemo.legacy-report-double-counts-last-amount`, default `true`),
and its date-range filter uses `NEXT SENTENCE`, which on the mainframe abandons the read loop at the
first out-of-range record rather than skipping it
(`carddemo.legacy-stop-report-at-out-of-range-record`, default `false`, i.e. the conversion skips).

`CBACT04C` never updates the account of the *last* group of category balances: the
`1050-UPDATE-ACCOUNT` call sits in an `ELSE` branch that the `PERFORM UNTIL` loop can never reach.
`carddemo.legacy-skip-final-account-update` (default `true`) reproduces that behaviour so output
matches the mainframe; set it to `false` to post the final account's interest as well.

## Running

```bash
mvn test                        # 81 tests: codecs, reject codes, interest formula, jobs, REST services
mvn package -DskipTests

mkdir -p target/data && cp ../data/ASCII/{acctdata,tcatbal}.txt target/data/
java -jar target/carddemo-batch-1.0.0-SNAPSHOT.jar \
  --spring.batch.job.enabled=true --spring.batch.job.name=postTranJob
java -jar target/carddemo-batch-1.0.0-SNAPSHOT.jar \
  --spring.batch.job.enabled=true --spring.batch.job.name=intCalcJob \
  --carddemo.transaction-file=target/data/intcalc-tran.txt
```

File assignments (the JCL DD equivalents) are the `carddemo.*` properties in
`src/main/resources/application.yml`.

## Data representation

`ZonedDecimal` decodes and encodes signed zoned decimal (`PIC S9(n)V99` DISPLAY) with the trailing
sign overpunch (`{`/`A`-`I` positive, `}`/`J`-`R` negative) into `BigDecimal`, so amounts stay
exact and records round-trip byte for byte — `CopybookRoundTripTest` asserts this against records
taken from the repository's own sample files.

Note: the shipped `acctdata.txt` leaves `ACCT-ADDR-ZIP` empty and starts the account group id ten
bytes early, so by copybook offsets the group id is read from the zip field and most accounts fall
back to the `DEFAULT` disclosure group.
