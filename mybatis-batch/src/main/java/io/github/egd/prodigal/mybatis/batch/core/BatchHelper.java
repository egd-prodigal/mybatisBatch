package io.github.egd.prodigal.mybatis.batch.core;

public final class BatchHelper {

    private static final ThreadLocal<Boolean> threadLocal = ThreadLocal.withInitial(() -> Boolean.FALSE);

    public static void setBatch() {
        threadLocal.set(Boolean.TRUE);
    }

    public static boolean isBatch() {
        return threadLocal.get();
    }

    public static void clear() {
        threadLocal.remove();
    }

    public static AutoCloseable startBatch() {
        setBatch();
        return new AutoCloseable() {
            @Override
            public void close() throws Exception {
                clear();
            }
        };
    }


}
