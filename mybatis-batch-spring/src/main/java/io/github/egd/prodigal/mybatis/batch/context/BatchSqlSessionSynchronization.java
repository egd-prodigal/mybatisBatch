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
        if (TransactionSynchronization.STATUS_ROLLED_BACK == status) {
            // 回滚操作，但是在这里回滚，已经晚了，之前执行的flushStatements已经把部分数据刷入数据库
            try {
                sqlSession.rollback();
            } catch (Exception ignored) {
            }
        }
        TransactionSynchronizationManager.unbindResourceIfPossible(sqlSessionTemplate);
        sqlSession.close();
    }

}
