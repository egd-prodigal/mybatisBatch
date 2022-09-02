package io.github.egd.prodigal.mybatis.batch.context;

import io.github.egd.prodigal.mybatis.batch.core.BatchSqlSessionBuilder;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;

public class SpringBatchSqlSessionBuilder implements BatchSqlSessionBuilder {

    private final SqlSessionTemplate sqlSessionTemplate;


    public SpringBatchSqlSessionBuilder(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionTemplate = new SqlSessionTemplate(sqlSessionFactory, ExecutorType.BATCH);
    }

    @Override
    public SqlSession build() {
        // 重新开启一个SqlSession
        return sqlSessionTemplate.getSqlSessionFactory().openSession(ExecutorType.BATCH, false);
    }
}
