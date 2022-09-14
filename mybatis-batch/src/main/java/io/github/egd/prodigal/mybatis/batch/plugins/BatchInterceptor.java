package io.github.egd.prodigal.mybatis.batch.plugins;

import io.github.egd.prodigal.mybatis.batch.core.BatchHelper;
import io.github.egd.prodigal.mybatis.batch.core.BatchInsertContext;
import io.github.egd.prodigal.mybatis.batch.core.BatchSqlSessionBuilder;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.SqlSession;

import java.util.Properties;
import java.util.function.Function;

@Intercepts(@Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}))
public class BatchInterceptor implements Interceptor {

    /**
     * 批量SqlSession构造器，通过它打开一个批量模式的SqlSession
     */
    private BatchSqlSessionBuilder batchSqlSessionBuilder;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        if (BatchHelper.isBatch()) {
            // 先获取参数
            Object[] argsObjects = invocation.getArgs();
            // 根据配置，拦截的是 org.apache.ibatis.executor.Executor#update方法，依次获取参数
            // 第一额参数是MappedStatement
            MappedStatement mappedStatement = (MappedStatement) argsObjects[0];
            // 第二个参数是入参
            Object parameter = argsObjects[1];
            String statement = mappedStatement.getId();
            // 根据sql指令类型判断
            SqlCommandType sqlCommandType = mappedStatement.getSqlCommandType();
            if (sqlCommandType == SqlCommandType.INSERT) {
                return doBatch(sqlSession -> sqlSession.insert(statement, parameter));
            } else if (sqlCommandType == SqlCommandType.UPDATE) {
                return doBatch(sqlSession -> sqlSession.insert(statement, parameter));
            } else if (sqlCommandType == SqlCommandType.DELETE) {
                return doBatch(sqlSession -> sqlSession.delete(statement, parameter));
            }
        }
        if (BatchInsertContext.isInSpring()) {
            doBatch(SqlSession::flushStatements);
        }
        return invocation.proceed();
    }

    private Object doBatch(Function<SqlSession, Object> function) {
        SqlSession sqlSession = getBatchSqlSessionBuilder().build(true);
        try {
            return function.apply(sqlSession);
        } finally {
            getBatchSqlSessionBuilder().close(sqlSession);
        }
    }

    @Override
    public Object plugin(Object target) {
        return Interceptor.super.plugin(target);
    }

    @Override
    public void setProperties(Properties properties) {
        Interceptor.super.setProperties(properties);
    }

    /**
     * 获取批量SqlSession构造器
     *
     * @return BatchSqlSessionBuilder
     */
    public BatchSqlSessionBuilder getBatchSqlSessionBuilder() {
        return batchSqlSessionBuilder;
    }

    /**
     * 设置批量SqlSession构造器
     *
     * @param batchSqlSessionBuilder 批量SqlSession构造器
     */
    public void setBatchSqlSessionBuilder(BatchSqlSessionBuilder batchSqlSessionBuilder) {
        this.batchSqlSessionBuilder = batchSqlSessionBuilder;
    }

}
