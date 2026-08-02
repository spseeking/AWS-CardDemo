---
name: testing-java-batch
description: How to run and test the CardDemo Java (Spring Boot 3) batch/REST module in app/java-batch — scratch data copies, generating usrsec.txt, running the REST app or a single batch job, and the record lengths to assert.
---

# Testing the CardDemo java-batch module

## Build
```
cd app/java-batch && mvn -B verify          # unit/integration tests
mvn -B package -DskipTests                  # jar at target/carddemo-batch-1.0.0-SNAPSHOT.jar
```

## Data
Never test against `app/data/ASCII` directly — the app rewrites the files. Copy them to a scratch
dir and override every `carddemo.*` path (see `BatchFilesProperties`) on the command line.
No `usrsec.txt` ships; generate 80-byte records:
`userId(8) + firstName(20) + lastName(20) + password(8) + type(1, A=admin/U=user) + filler(23)`.
A missing data file is treated as an empty dataset, so output files need no pre-creation.

## Running the REST API
```
java -jar target/carddemo-batch-1.0.0-SNAPSHOT.jar --carddemo.account-file=... (etc.)
```
The process only exits when `--spring.batch.job.name=` is passed; otherwise it stays up on 8080.
Every `/api` call except `POST /api/signon` needs the bearer token sign on returns:
`curl -s -XPOST localhost:8080/api/signon -H 'Content-Type: application/json' -d '{"userId":"ADMIN001","password":"PASSWORD"}'`
then `-H "Authorization: Bearer $TOKEN"`. `/api/users*` needs an admin (type `A`) token; anonymous
calls get 401 and non-admins 403. A clear USRSEC password is hashed into
`carddemo.user-credential-file` and blanked out of the record on first sign on, so it only works
once — sign on again with the same password (now verified against the hash), and point that
property at the scratch dir too.
Start it with `setsid nohup ... < /dev/null &` — plain background jobs get killed when the shell
tool session ends.

## Running one batch job from the CLI
```
java -jar target/carddemo-batch-1.0.0-SNAPSHOT.jar --server.port=0 \
  --spring.batch.job.enabled=true --spring.batch.job.name=postTranJob --carddemo.*=...
```
`--server.port=0` is required whenever the REST app is already using 8080.

## Record lengths to assert
account 300, card 150, transaction 350, reject 430, usrsec 80, tcatbal/xref/discgrp 50, customer 500.

## Devin Secrets Needed
None.
