package io.github.egd.prodigal.mybatis.batch.context;

import io.github.egd.prodigal.mybatis.batch.annotations.BatchInsert;
import io.github.egd.prodigal.mybatis.batch.core.BatchInsertContext;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.scripting.defaults.RawSqlSource;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BatchInsertBeanProcessor implements BeanPostProcessor, SmartInitializingSingleton, ApplicationContextAware {
    private ApplicationContext applicationContext;
    private final List<Method> methodList = new ArrayList<>();

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof MapperFactoryBean) {
            MapperFactoryBean<?> mapperFactoryBean = (MapperFactoryBean<?>) bean;
            Class<?> mapperInterface = mapperFactoryBean.getMapperInterface();
            Method[] declaredMethods = ReflectionUtils.getDeclaredMethods(mapperInterface);
            Arrays.stream(declaredMethods).filter(method -> AnnotationUtils.getAnnotation(method, BatchInsert.class) != null)
                    .filter(method -> AnnotationUtils.getAnnotation(method, Insert.class) != null).forEach(methodList::add);
        }
        return bean;
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (methodList.isEmpty()) {
            return;
        }
        BatchInsertContext.setInSpring();
        SqlSessionFactory sqlSessionFactory = applicationContext.getBean(SqlSessionFactory.class);
        Configuration configuration = sqlSessionFactory.getConfiguration();
        for (Method method : methodList) {
            BatchInsert batchInsert = AnnotationUtils.findAnnotation(method, BatchInsert.class);
            if (batchInsert == null) {
                continue;
            }
            Insert insert = AnnotationUtils.findAnnotation(method, Insert.class);
            if (insert == null) {
                continue;
            }
            String id = method.getDeclaringClass().getName() + "." + method.getName();
            if (configuration.hasStatement(id)) {
                String sql = String.join(" ", insert.value());
                BatchInsertContext.addBatchInsertMapperStatement(id, batchInsert);
                RawSqlSource rawSqlSource = new RawSqlSource(configuration, sql, batchInsert.paramType());
                String singleInsertId = id + BatchInsertContext.EGD_SINGLE_INSERT;
                MappedStatement.Builder builder = new MappedStatement.Builder(configuration, singleInsertId, rawSqlSource, SqlCommandType.INSERT);
                configuration.addMappedStatement(builder.build());
            }
        }
        methodList.clear();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
