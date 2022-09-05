package io.github.egd.prodigal.mybatis.batch.core;

import org.apache.ibatis.session.SqlSession;

/**
 * 批量SqlSession构造器
 */
public interface BatchSqlSessionBuilder {

    /**
     * 构造SqlSession
     *
     * @param flushStatements 是否预提交
     * @return SqlSession
     */
    SqlSession build(boolean flushStatements);

    /**
     * 提交事务
     *
     * @param sqlSession sqlSession
     * @param flushStatements 是否刷盘
     */
    default void commit(SqlSession sqlSession, boolean flushStatements) {
        if (flushStatements) {
            sqlSession.flushStatements();
        }
        sqlSession.commit();
    }

    /**
     * 关闭SqlSession
     *
     * @param sqlSession sqlSession
     */
    default void close(SqlSession sqlSession) {
        sqlSession.close();
    }

}
