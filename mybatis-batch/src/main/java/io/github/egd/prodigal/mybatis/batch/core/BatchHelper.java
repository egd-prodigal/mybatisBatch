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
public final class BatchHelper implements AutoCloseable {

    /**
     * 线程参数，标记是否是批量模式下
     */
    private static final ThreadLocal<Boolean> threadLocal = ThreadLocal.withInitial(() -> Boolean.FALSE);

    /**
     * batchExecutor线程对象
     */
    private static final ThreadLocal<Executor> executorThreadLocal = new ThreadLocal<>();

    private final boolean flushStatements;

    public BatchHelper(boolean flushStatements) {
        this.flushStatements = flushStatements;
    }

    /**
     * 开启批量模式
     */
    public static void startBatch() {
        threadLocal.set(Boolean.TRUE);
    }

    /**
     * 开启批量模式
     * @param flushStatements 是否自动刷盘
     * @return BatchHelper
     */
    public static BatchHelper startBatch(boolean flushStatements) {
        startBatch();
        return new BatchHelper(flushStatements);
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
     * 提交批量事务
     *
     * @return int[][]
     */
    public static int[][] flushStatements() {
        Executor executor = executorThreadLocal.get();
        if (executor != null) {
            try {
                List<BatchResult> batchResults = executor.flushStatements();
                int[][] result = new int[batchResults.size()][];
                for (int i = 0; i < batchResults.size(); i++) {
                    int[] updateCounts = batchResults.get(i).getUpdateCounts();
                    result[i] = updateCounts;
                }
                return result;
            } catch (SQLException e) {
                throw new PluginException(e);
            }
        }
        return new int[0][0];
    }

    /**
     * 关闭批量模式，并与执行sql
     */
    public static void closeBatch() {
        executorThreadLocal.remove();
        threadLocal.remove();
    }

    /**
     * 执行sql并关闭批量模式
     */
    public static void flushStatementsAndClose() {
        flushStatements();
        closeBatch();
    }

    public static void setBatchExecutor(Executor executor) {
        executorThreadLocal.set(executor);
    }

    public static Executor getBatchExecutor() {
        return executorThreadLocal.get();
    }

    @Override
    public void close() {
        if (flushStatements) {
            flushStatementsAndClose();
        } else {
            closeBatch();
        }
    }

}
