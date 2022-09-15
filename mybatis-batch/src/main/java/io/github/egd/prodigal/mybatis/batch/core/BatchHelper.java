package io.github.egd.prodigal.mybatis.batch.core;

import org.apache.ibatis.session.SqlSession;

/**
 * 批量模式辅助类
 * 在线程中绑定参数标记
 */
public final class BatchHelper {

    /**
     * 线程参数，标记是否是批量模式下
     */
    private static final ThreadLocal<Boolean> threadLocal = ThreadLocal.withInitial(() -> Boolean.FALSE);

    /**
     * 批量模式下执行使用的SqlSession，绑定线程
     */
    private static final ThreadLocal<SqlSession> sqlSessionThreadLocal = new ThreadLocal<>();

    /**
     * 开启批量模式
     */
    public static void startBatch() {
        threadLocal.set(Boolean.TRUE);
    }

    /**
     * 判断是否是批量模式
     *
     * @return boolean
     */
    public static boolean isBatch() {
        return threadLocal.get();
    }

    /**
     * 关闭批量模式，并与执行sql
     */
    public static void close() {
        if (BatchContext.isInSpring()) {
            // 交给SpringBatchSqlSessionBuilder执行flushStatements
            SqlSession sqlSession = BatchContext.getBatchSqlSessionBuilder().build(false);
            BatchContext.getBatchSqlSessionBuilder().commit(sqlSession, true);
        }
        SqlSession sqlSession = sqlSessionThreadLocal.get();
        if (sqlSession != null) {
            sqlSession.flushStatements();
            sqlSession.commit();
            sqlSessionThreadLocal.remove();
        }
        threadLocal.remove();
    }

    /**
     * 绑定批量模式的SqlSession
     *
     * @param sqlSession sqlSession
     */
    public static void boundSqlSession(SqlSession sqlSession) {
        if (sqlSessionThreadLocal.get() == null) {
            sqlSessionThreadLocal.set(sqlSession);
        }
    }

    /**
     * 获取批量模式的SqlSession
     *
     * @return SqlSession
     */
    public static SqlSession getSqlSession() {
        return sqlSessionThreadLocal.get();
    }

}
