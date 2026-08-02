package com.carddemo.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Arrays;

@SpringBootApplication
public class CardDemoBatchApplication {

    /**
     * A batch run exits with the job's status like the JCL step it replaces; without a job name the
     * process stays up to serve the converted CICS transactions over HTTP.
     */
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(CardDemoBatchApplication.class, args);
        if (Arrays.stream(args).anyMatch(argument -> argument.startsWith("--spring.batch.job.name="))) {
            System.exit(SpringApplication.exit(context));
        }
    }
}
