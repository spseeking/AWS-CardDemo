package com.carddemo.batch.config;

import com.carddemo.batch.interest.IntCalcTasklet;
import com.carddemo.batch.interest.InterestCalculator;
import com.carddemo.batch.posting.PostTranTasklet;
import com.carddemo.batch.posting.TransactionPoster;
import com.carddemo.batch.posting.TransactionValidator;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableConfigurationProperties(BatchFilesProperties.class)
public class BatchJobConfiguration {

    public static final String POST_TRAN_JOB = "postTranJob";
    public static final String INT_CALC_JOB = "intCalcJob";

    @Bean
    public TransactionValidator transactionValidator() {
        return new TransactionValidator();
    }

    @Bean
    public TransactionPoster transactionPoster() {
        return new TransactionPoster();
    }

    @Bean
    public InterestCalculator interestCalculator() {
        return new InterestCalculator();
    }

    @Bean
    @StepScope
    public PostTranTasklet postTranTasklet(BatchFilesProperties files, TransactionValidator validator,
                                           TransactionPoster poster) {
        return new PostTranTasklet(files, validator, poster);
    }

    @Bean
    @StepScope
    public IntCalcTasklet intCalcTasklet(BatchFilesProperties files, InterestCalculator calculator,
                                         @Value("#{jobParameters['parameterDate'] ?: T(java.time.LocalDate).now()"
                                                 + ".toString()}") String parameterDate) {
        return new IntCalcTasklet(files, calculator, parameterDate);
    }

    @Bean
    public Step postTranStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                             PostTranTasklet postTranTasklet) {
        return new StepBuilder("postTranStep", jobRepository)
                .tasklet(postTranTasklet, transactionManager)
                .build();
    }

    @Bean
    public Step intCalcStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                            IntCalcTasklet intCalcTasklet) {
        return new StepBuilder("intCalcStep", jobRepository)
                .tasklet(intCalcTasklet, transactionManager)
                .build();
    }

    @Bean
    public Job postTranJob(JobRepository jobRepository, Step postTranStep) {
        return new JobBuilder(POST_TRAN_JOB, jobRepository).start(postTranStep).build();
    }

    @Bean
    public Job intCalcJob(JobRepository jobRepository, Step intCalcStep) {
        return new JobBuilder(INT_CALC_JOB, jobRepository).start(intCalcStep).build();
    }
}
