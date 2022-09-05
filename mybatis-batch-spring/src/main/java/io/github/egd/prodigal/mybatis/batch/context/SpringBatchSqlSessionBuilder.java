package io.github.egd.prodigal.mybatis.batch.context;

import io.github.egd.prodigal.mybatis.batch.core.BatchInsertContext;
import io.github.egd.prodigal.mybatis.batch.core.BatchSqlSessionBuilder;
import org.apache.ibatis.plugin.PluginException;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.SqlSessionUtils;
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
     * @param flushStatements 是否预提交
     * @return SqlSession
     */
    @Override
    public SqlSession build(boolean flushStatements) {
        SqlSessionFactory sqlSessionFactory = sqlSessionTemplate.getSqlSessionFactory();
        if (flushStatements) {
            SqlSessionUtils.getSqlSession(sqlSessionFactory).flushStatements();
        }
        // 重新开启一个SqlSession，基于sqlSessionTemplate创建
        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH, false);
        // 如果存在spring管理的事务，则交给spring管理
        boolean synchronizationActive = TransactionSynchronizationManager.isSynchronizationActive();
        if (synchronizationActive) {
            Object resource = TransactionSynchronizationManager.getResource(sqlSessionTemplate);
            if (resource == null) {
                new BatchSqlSessionSynchronization(this.sqlSessionTemplate, sqlSession).register();
            }
        }
        return sqlSession;
    }

    /**
     * 提交事务
     *
     * @param sqlSession sqlSession
     * @param flushStatements 是否刷盘
     */
    @Override
    public void commit(SqlSession sqlSession, boolean flushStatements) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            BatchSqlSessionBuilder.super.commit(sqlSession, flushStatements);
        } else {
            if (flushStatements) {
                // 批量处理的SqlSession和常规的SqlSession都刷入数据库，使会话能够共享执行结果
                // 在实际使用中，建议批量的操作在业务的最后执行，最好是异步实现
                sqlSession.flushStatements();
                if (TransactionSynchronizationManager.getResource(sqlSessionTemplate.getSqlSessionFactory()) != null) {
                    SqlSession session = SqlSessionUtils.getSqlSession(sqlSessionTemplate.getSqlSessionFactory());
                    if (session != null) {
                        session.flushStatements();
                    }
                }
            }
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
        }
    }

}
