package io.github.egd.prodigal.mybatis.batch.plugins;

import io.github.egd.prodigal.mybatis.batch.core.BatchContext;
import io.github.egd.prodigal.mybatis.batch.core.BatchHelper;
import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.impl.PerpetualCache;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.SqlSession;

import java.util.Properties;
import java.util.function.Function;

/**
 * 批量模式拦截器
 */
@Intercepts(@Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}))
public class BatchInterceptor implements Interceptor {

    private final ThreadLocal<Boolean> threadLocal = ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        if (BatchHelper.isBatch()) {
            // 先获取参数
            Object[] argsObjects = invocation.getArgs();
            // 根据配置，拦截的是 org.apache.ibatis.executor.Executor#update方法，依次获取参数
            // 第一额参数是MappedStatement
            MappedStatement mappedStatement = (MappedStatement) argsObjects[0];
            // 如果cache里检测到已经在批量模式里了，直接执行，并移除标记
            if (threadLocal.get()) {
                threadLocal.set(Boolean.FALSE);
                return invocation.proceed();
            }
            // 第二个参数是入参
            Object parameter = argsObjects[1];
            String statement = mappedStatement.getId();
            // 根据sql指令类型判断
            SqlCommandType sqlCommandType = mappedStatement.getSqlCommandType();
            if (sqlCommandType == SqlCommandType.INSERT) {
                return doBatch(sqlSession -> sqlSession.insert(statement, parameter));
            } else if (sqlCommandType == SqlCommandType.UPDATE) {
                return doBatch(sqlSession -> sqlSession.update(statement, parameter));
            } else if (sqlCommandType == SqlCommandType.DELETE) {
                return doBatch(sqlSession -> sqlSession.delete(statement, parameter));
            } else if (sqlCommandType == SqlCommandType.FLUSH) {
                doBatch(SqlSession::flushStatements);
                return invocation.proceed();
            }
        }

        return invocation.proceed();
    }

    /**
     * 执行批量模式下的sql
     *
     * @param function 执行sql语句方法
     * @return Object
     */
    private Object doBatch(Function<SqlSession, Object> function) {
        SqlSession sqlSession = BatchContext.getBatchSqlSessionBuilder().build(false);
        try {
            // 设置批量标记
            threadLocal.set(Boolean.TRUE);
            return function.apply(sqlSession);
        } finally {
            BatchContext.getBatchSqlSessionBuilder().close(sqlSession);
        }
    }

    /**
     * 初始化cache并获取
     *
     * @param mappedStatement mappedStatement
     * @return Cache
     */
    private Cache initAndGetCache(MappedStatement mappedStatement) {
        Cache cache = mappedStatement.getCache();
        if (cache == null) {
            // 没有就指定一个
            MetaObject metaObject = MetaObject.forObject(mappedStatement, BatchContext.objectFactory,
                    BatchContext.objectWrapperFactory, BatchContext.reflectorFactory);
            cache = new PerpetualCache("egd-batch");
            metaObject.setValue("cache", cache);
        }
        return cache;
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
