package com.example.demo.mybaits.plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.apache.ibatis.binding.MapperMethod.ParamMap;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import com.example.demo.mybaits.annotation.BatchInsert;

@Component
@Intercepts(@Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}))
public class MybatisBatchInsertInterceptor implements Interceptor {
	
	@Autowired
	@Lazy
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
		if (object != null && object instanceof ParamMap) {
			ParamMap<?> paramMap = (ParamMap<?>) object;
			return invokeSingleInsert(paramMap, batchInsert, mapperClass);
		}  else {
			return invocation.proceed();
		}
	}
	
	private Object invokeSingleInsert(ParamMap<?> paramMap, BatchInsert batchInsert, Class<?> mapperClass) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		List<?> pList = (List<?>) paramMap.values().stream().filter(v -> (v instanceof List)).map(v -> ((List<?>) v)).findAny().get();
		String insertMethodName = batchInsert.insert();
		Method insertMethod = ReflectionUtils.findMethod(mapperClass, insertMethodName, pList.get(0).getClass());
		SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH, false);
		Object beanObject = sqlSession.getMapper(mapperClass);
		int batchSize = batchInsert.batchSize();
		for (int i = 0; i < pList.size(); i++) {
			insertMethod.invoke(beanObject, pList.get(i));
			if ((i + 1) % batchSize == 0) {
				sqlSession.commit();
				sqlSession.clearCache();
			}
		}
		sqlSession.commit();
		sqlSession.clearCache();
		return pList.size();			
	}

}
