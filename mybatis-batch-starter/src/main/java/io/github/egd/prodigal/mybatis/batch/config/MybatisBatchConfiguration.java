package io.github.egd.prodigal.mybatis.batch.config;

import io.github.egd.prodigal.mybatis.batch.context.BatchInsertBeanProcessor;
import io.github.egd.prodigal.mybatis.batch.plugins.BatchInsertInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisBatchConfiguration {

    @Bean
    public BatchInsertInterceptor batchInsertInterceptor() {
        return new BatchInsertInterceptor();
    }

    @Bean
    public BatchInsertBeanProcessor batchInsertBeanProcessor(@Autowired BatchInsertInterceptor batchInsertInterceptor) {
        BatchInsertBeanProcessor batchInsertBeanProcessor = new BatchInsertBeanProcessor();
        batchInsertBeanProcessor.setBatchInsertInterceptor(batchInsertInterceptor);
        return batchInsertBeanProcessor;
    }

}
