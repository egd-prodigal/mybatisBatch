package io.github.egd.prodigal.mybatis.batch.core;

import org.apache.ibatis.session.SqlSession;

public interface BatchSqlSessionBuilder {

    SqlSession build();

    default void commit(SqlSession sqlSession) {
        sqlSession.commit();
    }

    default void clearCache(SqlSession sqlSession) {
        sqlSession.clearCache();
    }

}
