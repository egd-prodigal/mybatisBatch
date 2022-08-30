package cn.egd.prodigal.mybatis.batch.plugins;

import cn.egd.prodigal.mybatis.batch.annotations.BatchInsert;
import org.apache.ibatis.binding.MapperMethod.ParamMap;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.List;

@Intercepts(@Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}))
public class MybatisBatchInsertInterceptor implements Interceptor {

    private SqlSessionFactory sqlSessionFactory;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] argsObjects = invocation.getArgs();
        MappedStatement mappedStatement = (MappedStatement) argsObjects[0];
        String id = mappedStatement.getId();
        String mapperClassName = id.substring(0, id.lastIndexOf("."));
        String methodName = id.substring(id.lastIndexOf(".") + 1);
        Class<?> mapperClass = ClassUtils.forName(mapperClassName, getClass().getClassLoader());
        Method method = ReflectionUtils.findMethod(mapperClass, methodName, List.class);
        if (method == null) {
            return invocation.proceed();
        }
        BatchInsert batchInsert = AnnotationUtils.findAnnotation(method, BatchInsert.class);
        if (batchInsert == null) {
            return invocation.proceed();
        }
        Object object = argsObjects[1];
        if (object instanceof ParamMap) {
            return invokeSingleInsert((ParamMap<?>) object, batchInsert, mapperClass);
        } else {
            return invocation.proceed();
        }
    }

    private Object invokeSingleInsert(ParamMap<?> paramMap, BatchInsert batchInsert, Class<?> mapperClass) throws Throwable {
        List<?> pList;
        String listParamName = batchInsert.listParamName();
        if (paramMap.containsKey(listParamName)) {
            Object o = paramMap.get(listParamName);
            pList = (List<?>) o;
        } else {
            pList = paramMap.values().stream()
                    .filter(v -> (v instanceof List))
                    .map(v -> ((List<?>) v))
                    .findAny().orElseThrow(() -> new PluginException("cannot find argument instance of List"));
        }
        String insertMethodName = batchInsert.insert();
        Method insertMethod = ReflectionUtils.findMethod(mapperClass, insertMethodName, pList.get(0).getClass());
        if (insertMethod == null) {
            throw new PluginException("cannot find insert method by name: " + insertMethodName);
        }
        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH, false);
        Object beanObject = sqlSession.getMapper(mapperClass);
        int batchSize = batchInsert.batchSize();
        int index = 1;
        for (Object argument : pList) {
            AopUtils.invokeJoinpointUsingReflection(beanObject, insertMethod, new Object[]{argument});
            if (index % batchSize == 0) {
                commitAndClearCache(sqlSession);
            }
            index++;
        }
        commitAndClearCache(sqlSession);
        sqlSession.close();
        return pList.size();
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

}
