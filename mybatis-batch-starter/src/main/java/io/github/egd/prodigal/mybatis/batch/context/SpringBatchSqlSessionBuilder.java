package io.github.egd.prodigal.mybatis.batch.context;

import io.github.egd.prodigal.mybatis.batch.core.BatchInsertContext;
import io.github.egd.prodigal.mybatis.batch.core.BatchSqlSessionBuilder;
import org.apache.ibatis.plugin.PluginException;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Spring批量SqlSession构造器
 */
public class SpringBatchSqlSessionBuilder implements BatchSqlSessionBuilder {

    private final SqlSessionTemplate sqlSessionTemplate;

    public SpringBatchSqlSessionBuilder() {
        SqlSessionFactory sqlSessionFactory = BatchInsertContext.getSqlSessionFactory();
        if (sqlSessionFactory == null) {
            throw new PluginException("you must call BatchInsertContext.setSqlSessionFactory(sqlSessionFactory) first");
        }
        this.sqlSessionTemplate = new SqlSessionTemplate(sqlSessionFactory, ExecutorType.BATCH);
    }

    /**
     * 构造SqlSession
     *
     * @return SqlSession
     */
    @Override
    public SqlSession build() {
        // 重新开启一个SqlSession，基于sqlSessionTemplate创建
        SqlSession sqlSession = sqlSessionTemplate.getSqlSessionFactory().openSession(ExecutorType.BATCH, false);
        // 如果存在spring管理的事务，则交给spring管理
        boolean synchronizationActive = TransactionSynchronizationManager.isSynchronizationActive();
        if (synchronizationActive) {
            TransactionSynchronizationManager.bindResource(sqlSessionTemplate, sqlSession);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void beforeCommit(boolean readOnly) {
                    sqlSession.commit();
                }

                @Override
                public void afterCompletion(int status) {
                    sqlSession.close();
                }
            });
        }
        return sqlSession;
    }

    /**
     * 提交事务
     *
     * @param sqlSession sqlSession
     */
    @Override
    public void commit(SqlSession sqlSession) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            BatchSqlSessionBuilder.super.commit(sqlSession);
        }
    }

    /**
     * 关闭SqlSession
     *
     * @param sqlSession sqlSession
     */
    @Override
    public void close(SqlSession sqlSession) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            BatchSqlSessionBuilder.super.close(sqlSession);
        } else {
            TransactionSynchronizationManager.unbindResourceIfPossible(sqlSessionTemplate);
        }
    }
}
