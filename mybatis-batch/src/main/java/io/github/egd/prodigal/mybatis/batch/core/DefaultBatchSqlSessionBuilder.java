package io.github.egd.prodigal.mybatis.batch.core;

import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

public class DefaultBatchSqlSessionBuilder implements BatchSqlSessionBuilder {

    private final SqlSessionFactory sqlSessionFactory;

    public DefaultBatchSqlSessionBuilder(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    @Override
    public SqlSession build() {
        return sqlSessionFactory.openSession(ExecutorType.BATCH, false);
    }

}
