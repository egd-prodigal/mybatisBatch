package io.github.egd.prodigal.mybatis.batch.context;

import org.apache.ibatis.session.SqlSession;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 批量sqlSession事务同步器，交给Spring事务管理
 */
public class BatchSqlSessionSynchronization implements TransactionSynchronization {

    private final SqlSessionTemplate sqlSessionTemplate;
    private final SqlSession sqlSession;

    public BatchSqlSessionSynchronization(SqlSessionTemplate sqlSessionTemplate, SqlSession sqlSession) {
        this.sqlSessionTemplate = sqlSessionTemplate;
        this.sqlSession = sqlSession;
        TransactionSynchronizationManager.bindResource(sqlSessionTemplate, sqlSession);
    }

    /**
     * 把自己注册到事务同步器
     */
    public void register() {
        TransactionSynchronizationManager.registerSynchronization(this);
    }

    @Override
    public void flush() {
        sqlSession.flushStatements();
    }

    @Override
    public void beforeCommit(boolean readOnly) {
        sqlSession.commit();
    }

    @Override
    public void afterCommit() {
        TransactionSynchronization.super.afterCommit();
    }

    @Override
    public void beforeCompletion() {
        TransactionSynchronization.super.beforeCompletion();
    }

    @Override
    public void afterCompletion(int status) {
        TransactionSynchronizationManager.unbindResourceIfPossible(sqlSessionTemplate);
        sqlSession.close();
    }

}
