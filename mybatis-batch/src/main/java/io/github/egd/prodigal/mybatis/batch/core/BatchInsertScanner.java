package io.github.egd.prodigal.mybatis.batch.core;

import io.github.egd.prodigal.mybatis.batch.annotations.BatchInsert;
import io.github.egd.prodigal.mybatis.batch.plugins.BatchInsertInterceptor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.PluginException;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 批量保存注册扫描器
 */
public class BatchInsertScanner {

    /**
     * Mapper类集合
     */
    private static final Set<Class<?>> mapperClasses = new HashSet<>();

    /**
     * 批量保存方法集合
     */
    private static final Set<Method> mapperMethods = new HashSet<>();

    /**
     * 添加Mapper接口类
     *
     * @param clazz 接口类
     * @param classes 允许传多个类
     */
    public static void addClass(Class<?> clazz, Class<?>... classes) {
        if (clazz.isInterface()) {
            mapperClasses.add(clazz);
        } else {
            throw new PluginException("clazz is not interface");
        }
        if (classes != null && classes.length > 0) {
            for (Class<?> aClass : classes) {
                if (aClass.isInterface()) {
                    mapperClasses.add(aClass);
                } else {
                    throw new PluginException("clazz is not interface");
                }
            }
        }
    }

    /**
     * 添加批量保存方法
     *
     * @param method 批量保存方法
     * @param methods 允许传多个方法
     */
    public static void addMethod(Method method, Method... methods) {
        if (method.getAnnotation(BatchInsert.class) != null) {
            mapperMethods.add(method);
        }
        for (Method m : methods) {
            if (m.getAnnotation(BatchInsert.class) != null) {
                mapperMethods.add(m);
            }
        }
    }

    /**
     * 扫描
     */
    public static void scan() {
        // 先从注册的Mapper接口类获取批量保存的方法
        if (!mapperClasses.isEmpty()) {
            mapperClasses.forEach(clazz -> {
                Method[] declaredMethods = clazz.getDeclaredMethods();
                Arrays.stream(declaredMethods).filter(method -> method.getAnnotation(BatchInsert.class) != null).forEach(mapperMethods::add);
            });
        }
        mapperClasses.clear();
        SqlSessionFactory sqlSessionFactory = BatchInsertContext.getSqlSessionFactory();
        Configuration configuration = sqlSessionFactory.getConfiguration();
        // 遍历方法注册MappedStatement
        for (Method mapperMethod : mapperMethods) {
            BatchInsert batchInsert = mapperMethod.getAnnotation(BatchInsert.class);
            // 不可能为空
            if (batchInsert == null) {
                continue;
            }
            // 当前批量保存的mappedStatementId
            String mappedStatementId = mapperMethod.getDeclaringClass().getName() + "." + mapperMethod.getName();
            if (configuration.hasStatement(mappedStatementId)) {
                // 已经有了，说明本方法已经装配成MappedStatement了
                MappedStatement mappedStatement = configuration.getMappedStatement(mappedStatementId);
                if (SqlCommandType.INSERT.equals(mappedStatement.getSqlCommandType())) {
                    // 只注册保存的语句
                    BatchInsertContext.addBatchInsertMappedStatement(mappedStatementId, batchInsert);
                    BatchInsertContext.registerSingleInsertMappedStatement(mappedStatementId, batchInsert);
                }
            } else {
                // 没有，说明本方法可能没有Insert注解，但是指定了insert方法，这里需要注册批量保存方法
                BatchInsertContext.addBatchInsertMappedStatement(mappedStatementId, batchInsert);
                BatchInsertContext.registerBatchInsertMappedStatement(mapperMethod, batchInsert);
            }
        }
        mapperMethods.clear();
        if (!BatchInsertContext.isInSpring()) {
            // 非spring环境下初始化拦截器的SqlSessionBuilder
            checkAndInitSqlSessionBuilder();
        }
    }

    /**
     * 初始化拦截器的SqlSessionBuilder
     */
    private static void checkAndInitSqlSessionBuilder() {
        // 防止没有手动设置拦截器的BatchSqlSessionBuilder，这里检查并设值
        List<Interceptor> interceptors = BatchInsertContext.getSqlSessionFactory().getConfiguration().getInterceptors();
        for (Interceptor interceptor : interceptors) {
            if (interceptor instanceof BatchInsertInterceptor) {
                BatchInsertInterceptor batchInsertInterceptor = (BatchInsertInterceptor) interceptor;
                if (batchInsertInterceptor.getBatchSqlSessionBuilder() == null) {
                    batchInsertInterceptor.setBatchSqlSessionBuilder(new DefaultBatchSqlSessionBuilder());
                }
            }
        }
    }


}
