package io.github.egd.prodigal.mybatis.batch.core;

import org.apache.ibatis.executor.BatchResult;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.plugin.PluginException;

import java.sql.SQLException;
import java.util.List;

/**
 * 批量模式辅助类
 * 在线程中绑定参数标记
 */
public final class BatchHelper {

    /**
     * 线程参数，标记是否是批量模式下
     */
    private static final ThreadLocal<Boolean> threadLocal = ThreadLocal.withInitial(() -> Boolean.FALSE);

    /**
     * batchExecutor线程对象
     */
    private static final ThreadLocal<Executor> executorThreadLocal = new ThreadLocal<>();

    /**
     * 开启批量模式
     */
    public static void startBatch() {
        threadLocal.set(Boolean.TRUE);
    }

    /**
     * 判断是否是批量模式
     *
     * @return boolean
     */
    public static boolean isBatch() {
        return threadLocal.get();
    }

    /**
     * 关闭批量模式，并与执行sql
     */
    public static void close() {
        Executor executor = executorThreadLocal.get();
        if (executor != null) {
            try {
                List<BatchResult> batchResults = executor.flushStatements();
                for (BatchResult batchResult : batchResults) {
                    int[] updateCounts = batchResult.getUpdateCounts();
                    for (int updateCount : updateCounts) {
                        System.out.println(updateCount);
                    }
                }
            } catch (SQLException e) {
                throw new PluginException(e);
            }
        }
        executorThreadLocal.remove();
        threadLocal.remove();
    }

    public static void setBatchExecutor(Executor executor) {
        executorThreadLocal.set(executor);
    }

    public static Executor getBatchExecutor() {
        return executorThreadLocal.get();
    }

}
