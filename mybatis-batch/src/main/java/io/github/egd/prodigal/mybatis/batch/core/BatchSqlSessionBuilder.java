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

}
