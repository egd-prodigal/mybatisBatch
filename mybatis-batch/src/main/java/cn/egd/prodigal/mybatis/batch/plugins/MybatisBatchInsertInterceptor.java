package cn.egd.prodigal.mybatis.batch.plugins;

import cn.egd.prodigal.mybatis.batch.annotations.BatchInsert;
import org.apache.ibatis.binding.MapperMethod.ParamMap;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Intercepts(@Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}))
public class MybatisBatchInsertInterceptor implements Interceptor {

    private SqlSessionFactory sqlSessionFactory;

    private final Map<String, Class<?>> mapperClassMap = new HashMap<>();
    private final Map<String, Method> mapperMethodMap = new HashMap<>();
    private final Map<String, BatchInsert> batchInsertMap = new HashMap<>();

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] argsObjects = invocation.getArgs();
        MappedStatement mappedStatement = (MappedStatement) argsObjects[0];
        String id = mappedStatement.getId();
        Class<?> mapperClass = findMapperClass(id);
        Method mapperMethod = findMapperMethod(id, mapperClass);
        if (mapperMethod == null) {
            return invocation.proceed();
        }
        BatchInsert batchInsert = findBatchInsert(id);
        if (batchInsert == null) {
            return invocation.proceed();
        }
        Object object = argsObjects[1];
        if (object instanceof ParamMap) {
            List<?> parameterList = generateParameterList((ParamMap<?>) object, batchInsert);
            return invokeSingleInsert(mappedStatement, batchInsert, parameterList);
        } else {
            return invocation.proceed();
        }
    }

    private Object invokeSingleInsert(MappedStatement mappedStatement, BatchInsert batchInsert, List<?> parameterList) {
        SqlSession sqlSession = getSqlSessionFactory().openSession(ExecutorType.BATCH, false);
        int batchSize = batchInsert.batchSize();
        int index = 1;
        for (Object argument : parameterList) {
            sqlSession.insert(mappedStatement.getId(), argument);
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
            parameterList = paramMap.values().stream()
                    .filter(v -> (v instanceof List))
                    .map(v -> ((List<?>) v))
                    .findAny().orElseThrow(() -> new PluginException("cannot find argument instance of List"));
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

    private BatchInsert findBatchInsert(String id) throws ClassNotFoundException {
        if (batchInsertMap.containsKey(id)) {
            return batchInsertMap.get(id);
        }
        Class<?> mapperClass = findMapperClass(id);
        Method mapperMethod = findMapperMethod(id, mapperClass);
        BatchInsert batchInsert = AnnotationUtils.findAnnotation(mapperMethod, BatchInsert.class);
        batchInsertMap.put(id, batchInsert);
        return batchInsert;
    }

}
