package cn.egd.prodigal.mybatis.batch.config;

import cn.egd.prodigal.mybatis.batch.context.BatchInsertScanner;
import cn.egd.prodigal.mybatis.batch.plugins.MybatisBatchInsertInterceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisBatchConfiguration {

    @Bean
    public MybatisBatchInsertInterceptor mybatisBatchInsertInterceptor() {
        return new MybatisBatchInsertInterceptor();
    }

    @Bean
    public SmartInitializingSingleton initMybatisBatchInsertInterceptor(@Autowired SqlSessionFactory sqlSessionFactory,
                                                                        @Autowired MybatisBatchInsertInterceptor interceptor) {
        return () -> interceptor.setSqlSessionFactory(sqlSessionFactory);
    }

    @Bean
    public BatchInsertScanner batchInsertScanner() {
        return new BatchInsertScanner();
    }

}
