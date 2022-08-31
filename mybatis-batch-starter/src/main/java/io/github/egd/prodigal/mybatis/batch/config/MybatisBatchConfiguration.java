package io.github.egd.prodigal.mybatis.batch.config;

import io.github.egd.prodigal.mybatis.batch.context.BatchInsertBeanProcessor;
import io.github.egd.prodigal.mybatis.batch.plugins.BatchInsertInterceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
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
    public SmartInitializingSingleton initBatchInsertInterceptor(@Autowired SqlSessionFactory sqlSessionFactory,
                                                                 @Autowired BatchInsertInterceptor interceptor) {
        return () -> interceptor.setSqlSessionFactory(sqlSessionFactory);
    }

    @Bean
    public BatchInsertBeanProcessor batchInsertBeanProcessor() {
        return new BatchInsertBeanProcessor();
    }

}
