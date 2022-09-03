package io.github.egd.prodigal.mybatis.batch.context;

import io.github.egd.prodigal.mybatis.batch.core.BatchInsertContext;
import io.github.egd.prodigal.mybatis.batch.core.BatchSqlSessionBuilder;
import org.apache.ibatis.plugin.PluginException;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;

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
        return sqlSessionTemplate.getSqlSessionFactory().openSession(ExecutorType.BATCH, false);
    }

}
