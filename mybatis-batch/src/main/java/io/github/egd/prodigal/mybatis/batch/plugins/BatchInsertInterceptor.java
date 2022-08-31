package io.github.egd.prodigal.mybatis.batch.plugins;

import io.github.egd.prodigal.mybatis.batch.annotations.BatchInsert;
import io.github.egd.prodigal.mybatis.batch.core.BatchInsertContext;
import org.apache.ibatis.binding.MapperMethod.ParamMap;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Intercepts(@Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}))
public class BatchInsertInterceptor implements Interceptor {

    private SqlSessionFactory sqlSessionFactory;

    private final Map<String, Class<?>> mapperClassMap = new HashMap<>();
    private final Map<String, Method> mapperMethodMap = new HashMap<>();

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] argsObjects = invocation.getArgs();
        MappedStatement mappedStatement = (MappedStatement) argsObjects[0];
        if (mappedStatement.getSqlCommandType() != SqlCommandType.INSERT) {
            return invocation.proceed();
        }
        String id = mappedStatement.getId();
        BatchInsert batchInsert;
        if (BatchInsertContext.isInSpring()) {
            if (BatchInsertContext.isBatchInsertMapperStatement(id)) {
                batchInsert = BatchInsertContext.getBatchInsertByMapperStatementId(id);
            } else {
                return invocation.proceed();
            }
        } else {
            Class<?> mapperClass = findMapperClass(id);
            Method mapperMethod = findMapperMethod(id, mapperClass);
            if (mapperMethod == null) {
                return invocation.proceed();
            }
            batchInsert = findBatchInsert(id, mapperMethod);
            if (batchInsert == null) {
                return invocation.proceed();
            }
        }
        Object object = argsObjects[1];
        if (!(object instanceof ParamMap)) {
            return invocation.proceed();
        }
        List<?> parameterList = generateParameterList((ParamMap<?>) object, batchInsert);
        return invokeSingleInsert(mappedStatement, batchInsert, parameterList);
    }

    private Object invokeSingleInsert(MappedStatement mappedStatement, BatchInsert batchInsert, List<?> parameterList) {
        SqlSession sqlSession = getSqlSessionFactory().openSession(ExecutorType.BATCH, false);
        int batchSize = batchInsert.batchSize();
        int index = 1;
        String paramName = batchInsert.paramName();
        for (Object argument : parameterList) {
            if (StringUtils.hasText(paramName)) {
                ParamMap<Object> paramMap = new ParamMap<>();
                paramMap.put(paramName, argument);
                argument = paramMap;
            }
            if (BatchInsertContext.isInSpring()) {
                sqlSession.insert(mappedStatement.getId() + ".singleInsert", argument);
            } else {
                sqlSession.insert(mappedStatement.getId(), argument);
            }
            if (index % batchSize == 0) {
                commitAndClearCache(sqlSession);
            }
            index++;
        }
        commitAndClearCache(sqlSession);
        sqlSession.close();
        return parameterList.size();
    }

    private List<?> generateParameterList(ParamMap<?> paramMap, BatchInsert batchInsert) {
        List<?> parameterList;
        String listParamName = batchInsert.listParamName();
        if (paramMap.containsKey(listParamName)) {
            Object o = paramMap.get(listParamName);
            parameterList = (List<?>) o;
        } else {
            Stream<? extends List<?>> stream = paramMap.values().stream().filter(v -> (v instanceof List)).map(v -> ((List<?>) v));
            parameterList = stream.findAny().orElseThrow(() -> new PluginException("cannot find argument instance of List"));
        }
        return parameterList;
    }

    private void commitAndClearCache(SqlSession sqlSession) {
        sqlSession.commit();
        sqlSession.clearCache();
    }

    public SqlSessionFactory getSqlSessionFactory() {
        return sqlSessionFactory;
    }

    public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    private Class<?> findMapperClass(String id) throws ClassNotFoundException {
        Class<?> aClass = mapperClassMap.get(id);
        if (aClass == null) {
            String mapperClassName = id.substring(0, id.lastIndexOf("."));
            aClass = ClassUtils.forName(mapperClassName, getClass().getClassLoader());
            mapperClassMap.put(id, aClass);
        }
        return aClass;
    }

    private Method findMapperMethod(String id, Class<?> mapperClass) {
        Method method = mapperMethodMap.get(id);
        if (method == null) {
            String methodName = id.substring(id.lastIndexOf(".") + 1);
            Method[] methods = ReflectionUtils.getDeclaredMethods(mapperClass);
            for (Method m : methods) {
                if (methodName.equals(m.getName())) {
                    method = m;
                    mapperMethodMap.put(id, method);
                    break;
                }
            }
        }
        return method;
    }

    private BatchInsert findBatchInsert(String id, Method mapperMethod) {
        if (BatchInsertContext.isBatchInsertMapperStatement(id)) {
            return BatchInsertContext.getBatchInsertByMapperStatementId(id);
        }
        BatchInsert batchInsert = AnnotationUtils.findAnnotation(mapperMethod, BatchInsert.class);
        BatchInsertContext.addBatchInsertMapperStatement(id, batchInsert);
        return batchInsert;
    }

}
