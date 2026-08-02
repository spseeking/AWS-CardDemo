package com.carddemo.batch.listing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * The sequential read-and-print programs: CBACT01C (accounts), CBACT02C (cards), CBACT03C (card
 * cross reference), CBCUS01C (customers) and CBTRN01C (daily transactions with their cross
 * reference). Each parses every record of one master file and displays it, so a single tasklet
 * parameterised by file and record codec covers them all.
 */
public class MasterFileListingTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(MasterFileListingTasklet.class);

    private final String programName;
    private final Supplier<Path> file;
    private final Function<String, ?> parser;

    public MasterFileListingTasklet(String programName, Supplier<Path> file, Function<String, ?> parser) {
        this.programName = programName;
        this.file = file;
        this.parser = parser;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        Path path = file.get();
        log.info("START OF EXECUTION OF PROGRAM {}", programName);
        int records = 0;
        try (Stream<String> lines = Files.lines(path, StandardCharsets.ISO_8859_1)) {
            for (String line : (Iterable<String>) lines.map(l -> l.replace("\r", ""))
                    .filter(l -> !l.isBlank())::iterator) {
                log.info("{}", parser.apply(line));
                records++;
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read " + path, e);
        }
        log.info("END OF EXECUTION OF PROGRAM {} - {} RECORDS", programName, records);
        chunkContext.getStepContext().getStepExecution().getExecutionContext().putInt("records", records);
        return RepeatStatus.FINISHED;
    }
}
