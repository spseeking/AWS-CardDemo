package com.carddemo.batch.config;

import com.carddemo.batch.domain.Account;
import com.carddemo.batch.domain.Card;
import com.carddemo.batch.domain.CardXref;
import com.carddemo.batch.domain.Customer;
import com.carddemo.batch.domain.TransactionRecord;
import com.carddemo.batch.export.CustomerExportTasklet;
import com.carddemo.batch.export.CustomerImportTasklet;
import com.carddemo.batch.interest.IntCalcTasklet;
import com.carddemo.batch.listing.MasterFileListingTasklet;
import com.carddemo.batch.report.TransactionReportTasklet;
import com.carddemo.batch.statement.StatementTasklet;
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
import org.springframework.batch.core.step.tasklet.Tasklet;
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
    public static final String TRANSACTION_REPORT_JOB = "transactionReportJob";
    public static final String STATEMENT_JOB = "statementJob";
    public static final String CUSTOMER_EXPORT_JOB = "customerExportJob";
    public static final String CUSTOMER_IMPORT_JOB = "customerImportJob";

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

    @Bean
    public Job transactionReportJob(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                                    BatchFilesProperties files) {
        return job(jobRepository, TRANSACTION_REPORT_JOB, transactionManager,
                new TransactionReportTasklet(files));
    }

    @Bean
    public Job statementJob(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                            BatchFilesProperties files) {
        return job(jobRepository, STATEMENT_JOB, transactionManager, new StatementTasklet(files));
    }

    @Bean
    public Job customerExportJob(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                                 BatchFilesProperties files,
                                 @Value("${carddemo.export-branch-id:0001}") String branchId,
                                 @Value("${carddemo.export-region-code:NORTH}") String regionCode) {
        return job(jobRepository, CUSTOMER_EXPORT_JOB, transactionManager,
                new CustomerExportTasklet(files, branchId, regionCode));
    }

    @Bean
    public Job customerImportJob(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                                 BatchFilesProperties files) {
        return job(jobRepository, CUSTOMER_IMPORT_JOB, transactionManager, new CustomerImportTasklet(files));
    }

    @Bean
    public Job accountListingJob(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                                 BatchFilesProperties files) {
        return job(jobRepository, "accountListingJob", transactionManager,
                new MasterFileListingTasklet("CBACT01C", files::getAccountFile, Account::parse));
    }

    @Bean
    public Job cardListingJob(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                              BatchFilesProperties files) {
        return job(jobRepository, "cardListingJob", transactionManager,
                new MasterFileListingTasklet("CBACT02C", files::getCardFile, Card::parse));
    }

    @Bean
    public Job cardXrefListingJob(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                                  BatchFilesProperties files) {
        return job(jobRepository, "cardXrefListingJob", transactionManager,
                new MasterFileListingTasklet("CBACT03C", files::getCardXrefFile, CardXref::parse));
    }

    @Bean
    public Job customerListingJob(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                                  BatchFilesProperties files) {
        return job(jobRepository, "customerListingJob", transactionManager,
                new MasterFileListingTasklet("CBCUS01C", files::getCustomerFile, Customer::parse));
    }

    @Bean
    public Job dailyTransactionListingJob(JobRepository jobRepository,
                                          PlatformTransactionManager transactionManager,
                                          BatchFilesProperties files) {
        return job(jobRepository, "dailyTransactionListingJob", transactionManager,
                new MasterFileListingTasklet("CBTRN01C", files::getDailyTransactionFile,
                        TransactionRecord::parse));
    }

    private static Job job(JobRepository jobRepository, String name,
                           PlatformTransactionManager transactionManager, Tasklet tasklet) {
        Step step = new StepBuilder(name.replace("Job", "Step"), jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
        return new JobBuilder(name, jobRepository).start(step).build();
    }
}
