package io.github.egd.prodigal.mybatis.batch.plugins;

import io.github.egd.prodigal.mybatis.batch.core.BatchContext;
import io.github.egd.prodigal.mybatis.batch.core.BatchHelper;
import org.apache.ibatis.executor.BatchExecutor;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.Transaction;

import java.sql.SQLException;
import java.util.Properties;
import java.util.function.Predicate;

/**
 * 批量模式拦截器
 */
@Intercepts(@Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}))
public class BatchInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        if (BatchHelper.isBatch()) {
            // 先获取参数
            Object[] argsObjects = invocation.getArgs();
            // 根据配置，拦截的是 org.apache.ibatis.executor.Executor#update方法，依次获取参数
            // 第一额参数是MappedStatement
            MappedStatement mappedStatement = (MappedStatement) argsObjects[0];
            // 如果cache里检测到已经在批量模式里了，直接执行，并移除标记
            // 第二个参数是入参
            Object parameter = argsObjects[1];
            // 根据sql指令类型判断
            SqlCommandType sqlCommandType = mappedStatement.getSqlCommandType();
            Object result;
            switch (sqlCommandType) {
                case INSERT:
                case UPDATE:
                case DELETE:
                    result = doBatch((Executor) invocation.getTarget(), mappedStatement, parameter);
                    break;
                default:
                    result = invocation.proceed();
                    break;
            }
            return result;
        }

        return invocation.proceed();
    }

    /**
     * 执行批量模式下的sql
     *
     * @return Object
     */
    private int doBatch(Executor simpleExecutor, MappedStatement mappedStatement, Object parameter) throws SQLException {
        Executor executor = BatchHelper.getBatchExecutor();
        if (executor == null) {
            synchronized (BatchHelper.class) {
                if (BatchHelper.getBatchExecutor() == null) {
                    SqlSessionFactory sqlSessionFactory = BatchContext.getSqlSessionFactory();
                    Configuration configuration = sqlSessionFactory.getConfiguration();
                    Transaction transaction = simpleExecutor.getTransaction();
                    InterceptorChain interceptorChain = new InterceptorChain();
                    Predicate<Interceptor> predicate = interceptor -> !(interceptor instanceof BatchInterceptor);
                    configuration.getInterceptors().stream().filter(predicate).forEach(interceptorChain::addInterceptor);
                    executor = (Executor) interceptorChain.pluginAll(new BatchExecutor(configuration, transaction));
                    BatchHelper.setBatchExecutor(executor);
                } else {
                    executor = BatchHelper.getBatchExecutor();
                }
            }
        }

        return executor.update(mappedStatement, parameter);
    }


    @Override
    public Object plugin(Object target) {
        return Interceptor.super.plugin(target);
    }

    @Override
    public void setProperties(Properties properties) {
        Interceptor.super.setProperties(properties);
    }

}
