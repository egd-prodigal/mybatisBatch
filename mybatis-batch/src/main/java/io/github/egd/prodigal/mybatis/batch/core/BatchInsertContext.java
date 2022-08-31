package io.github.egd.prodigal.mybatis.batch.core;

import io.github.egd.prodigal.mybatis.batch.annotations.BatchInsert;

import java.util.HashMap;
import java.util.Map;

public class BatchInsertContext {

    private static final Map<String, BatchInsert> batchInsertAnnotationMap = new HashMap<>();

    private static boolean inSpring = false;

    public static void setInSpring() {
        inSpring = true;
    }

    public static boolean isInSpring() {
        return inSpring;
    }

    public static boolean isBatchInsertMapperStatement(String id) {
        return batchInsertAnnotationMap.containsKey(id);
    }

    public static void addBatchInsertMapperStatement(String id, BatchInsert batchInsert) {
        batchInsertAnnotationMap.put(id, batchInsert);
    }

    public static BatchInsert getBatchInsertByMapperStatementId(String id) {
        return batchInsertAnnotationMap.get(id);
    }

}
