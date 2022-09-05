package io.github.egd.prodigal.mybatis.batch.core;

import org.apache.ibatis.session.SqlSession;

/**
 * 批量SqlSession构造器
 */
public interface BatchSqlSessionBuilder {

    /**
     * 构造SqlSession
     *
     * @return SqlSession
     */
    SqlSession build();

    /**
     * 提交事务
     *
     * @param sqlSession sqlSession
     */
    default void commit(SqlSession sqlSession) {
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
