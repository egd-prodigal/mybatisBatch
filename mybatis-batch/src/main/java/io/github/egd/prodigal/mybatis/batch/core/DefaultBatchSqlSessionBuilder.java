package io.github.egd.prodigal.mybatis.batch.core;

import org.apache.ibatis.plugin.PluginException;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

/**
 * 默认批量SqlSession构造器，非spring环境里使用的
 */
public class DefaultBatchSqlSessionBuilder implements BatchSqlSessionBuilder {

    /**
     * 当前运行环境的SqlSessionFactory
     */
    private final SqlSessionFactory sqlSessionFactory;

    public DefaultBatchSqlSessionBuilder() {
        this.sqlSessionFactory = BatchInsertContext.getSqlSessionFactory();
        if (sqlSessionFactory == null) {
            throw new PluginException("you must invoke BatchInsertContext.setSqlSessionFactory first");
        }
    }


    /**
     * 构造SqlSession
     *
     * @param flushStatements 是否预提交
     * @return SqlSession
     */
    @Override
    public SqlSession build(boolean flushStatements) {
        // 非spring环境或者默认环境下，直接调用openSession方法，并指定Batch模式和不自动提交
        return sqlSessionFactory.openSession(ExecutorType.BATCH, false);
    }

}
