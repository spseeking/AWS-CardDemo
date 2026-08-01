# CardDemo Batch — Spring Batch conversion of POSTTRAN and INTCALC

Java 17 / Spring Boot 3 / Spring Batch conversion of two CardDemo COBOL batch programs:

| JCL job  | COBOL program | Spring Batch job | Converted logic |
|:---------|:--------------|:-----------------|:----------------|
| POSTTRAN | CBTRN02C      | `postTranJob`    | Daily transaction validation, posting, rejects |
| INTCALC  | CBACT04C      | `intCalcJob`     | Monthly interest accrual and account roll-up |

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

`CBACT04C` never updates the account of the *last* group of category balances: the
`1050-UPDATE-ACCOUNT` call sits in an `ELSE` branch that the `PERFORM UNTIL` loop can never reach.
`carddemo.legacy-skip-final-account-update` (default `true`) reproduces that behaviour so output
matches the mainframe; set it to `false` to post the final account's interest as well.

## Running

```bash
mvn test                        # 41 tests: codecs, reject codes, interest formula, both jobs
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
