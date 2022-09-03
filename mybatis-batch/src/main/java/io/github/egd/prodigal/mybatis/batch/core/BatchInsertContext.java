package io.github.egd.prodigal.mybatis.batch.core;

import io.github.egd.prodigal.mybatis.batch.annotations.BatchInsert;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.plugin.PluginException;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * 批量插入上下文，提供一些公用方法，存储sqlSessionFactory等信息
 */
public class BatchInsertContext {

    /**
     * BatchInsert注解缓存
     */
    private static final Map<String, BatchInsert> batchInsertAnnotationMap = new HashMap<>();

    /**
     * 当前应用的SqlSessionFactory
     */
    private static SqlSessionFactory sqlSessionFactory;

    /**
     * 是否在spring容器里，即是否依赖了mybatis-batch-starter并以spring应用启动
     */
    private static boolean inSpring = false;

    /**
     * 本插件关键字，标识单条插入的MappedStatement
     */
    public static final String EGD_SINGLE_INSERT = ".egd-singleInsert";

    /**
     * 设置当前环境为spring
     *
     * @see #inSpring
     */
    public static void setInSpring() {
        inSpring = true;
    }

    /**
     * 是否在spring环境里，即是否依赖了mybatis-batch-starter并以spring应用启动
     *
     * @return boolean
     * @see #inSpring
     */
    public static boolean isInSpring() {
        return inSpring;
    }

    /**
     * 是否是批量保存的MappedStatement
     *
     * @param id mappedStatementId
     * @return boolean
     */
    public static boolean isBatchInsertMappedStatement(String id) {
        return batchInsertAnnotationMap.containsKey(id);
    }

    /**
     * 添加批量保存MappedStatement，
     * spring容器里自动添加，非spring容器在初次执行方法时添加
     *
     * @param id          mappedStatementId
     * @param batchInsert 批量保存的注解
     */
    public synchronized static void addBatchInsertMappedStatement(String id, BatchInsert batchInsert) {
        batchInsertAnnotationMap.put(id, batchInsert);
    }

    /**
     * 获取批量保存注解配置
     *
     * @param id MappedStatementId
     * @return BatchInsert注解
     */
    public static BatchInsert getBatchInsertByMappedStatementId(String id) {
        return batchInsertAnnotationMap.get(id);
    }

    /**
     * 设置当前应用的sqlSessionFactory，spring环境里自动设置，非spring环境需要手动设置
     *
     * @param sqlSessionFactory sqlSessionFactory
     */
    public static void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
        BatchInsertContext.sqlSessionFactory = sqlSessionFactory;
    }

    /**
     * 获取当前应用的sqlSessionFactory
     *
     * @return SqlSessionFactory
     */
    public static SqlSessionFactory getSqlSessionFactory() {
        return sqlSessionFactory;
    }

    /**
     * 注册单条保存的MappedStatement，在批量的id后面增加.egd-singleInsert表示单个，
     * 如果BatchInsert注解已经配置了{@link BatchInsert#insert()}，则优先使用insert配置
     *
     * @param id          批量保存的MappedStatementId
     * @param batchInsert BatchInsert
     */
    public synchronized static void registerSingleInsertMappedStatement(String id, BatchInsert batchInsert) {
        Configuration configuration = sqlSessionFactory.getConfiguration();
        boolean hasInsert = !"".equals(batchInsert.insert());
        if (!hasInsert) {
            // 有insert表示这是指定其他方法的情况，不需要额外注册单条保存的MappedStatement了
            String singleMappedStatementInsertId = id + BatchInsertContext.EGD_SINGLE_INSERT;
            if (configuration.hasStatement(singleMappedStatementInsertId)) {
                // 多线程下防止重复注册
                return;
            }
            MappedStatement mappedStatement = configuration.getMappedStatement(id);
            SqlSource sqlSource = mappedStatement.getSqlSource();
            MappedStatement.Builder builder = new MappedStatement.Builder(configuration, singleMappedStatementInsertId,
                    sqlSource, SqlCommandType.INSERT);
            configuration.addMappedStatement(builder.build());
        }
    }

    /**
     * 注册单条保存的MappedStatement，在批量的id后面增加.egd-singleInsert表示单个
     *
     * @param method      批量保存的方法
     * @param batchInsert BatchInsert
     */
    public synchronized static void registerBatchInsertMappedStatement(Method method, BatchInsert batchInsert) {
        Configuration configuration = sqlSessionFactory.getConfiguration();
        String className = method.getDeclaringClass().getName();
        String id = className + "." + method.getName();
        if (configuration.hasStatement(id)) {
            // 多线程下防止重复注册
            return;
        }
        // 先根据配置找单条保存的MappedStatement
        String singleMappedStatementInsertId = className + "." + batchInsert.insert();
        if (!configuration.hasStatement(singleMappedStatementInsertId)) {
            // 没找到单条保存的MappedStatement，抛出异常
            throw new PluginException("cannot find insert method: [ " + batchInsert.insert()
                    + " ] in class: [ " + className + " ]");
        }
        // 这里的sqlSource是什么已经不重要了
        MappedStatement mappedStatement = configuration.getMappedStatement(singleMappedStatementInsertId);
        SqlSource sqlSource = mappedStatement.getSqlSource();
        MappedStatement.Builder builder = new MappedStatement.Builder(configuration, id,
                sqlSource, SqlCommandType.INSERT);
        configuration.addMappedStatement(builder.build());
    }

}
