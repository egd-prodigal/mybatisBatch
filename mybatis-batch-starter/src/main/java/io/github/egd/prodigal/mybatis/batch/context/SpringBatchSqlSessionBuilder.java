package io.github.egd.prodigal.mybatis.batch.context;

import io.github.egd.prodigal.mybatis.batch.core.BatchSqlSessionBuilder;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.MyBatisExceptionTranslator;
import org.mybatis.spring.SqlSessionUtils;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;

public class SpringBatchSqlSessionBuilder implements BatchSqlSessionBuilder {

    private final SqlSessionFactory sqlSessionFactory;

    private final MyBatisExceptionTranslator exceptionTranslator = new MyBatisExceptionTranslator(SQLErrorCodeSQLExceptionTranslator::new, false);

    public SpringBatchSqlSessionBuilder(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    @Override
    public SqlSession build() {
        return SqlSessionUtils.getSqlSession(sqlSessionFactory, ExecutorType.BATCH, exceptionTranslator);
    }
}
