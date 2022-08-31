package cn.egd.prodigal.mybatis.batch.context;

import cn.egd.prodigal.mybatis.batch.annotations.BatchInsert;
import cn.egd.prodigal.mybatis.batch.core.BatchInsertContext;
import org.apache.ibatis.builder.BuilderException;
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
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
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
            for (Method declaredMethod : declaredMethods) {
                if (AnnotationUtils.getAnnotation(declaredMethod, BatchInsert.class) != null) {
                    methodList.add(declaredMethod);
                }
            }
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
            String id = method.getDeclaringClass().getName() + "." + method.getName();
            BatchInsert batchInsert = AnnotationUtils.findAnnotation(method, BatchInsert.class);
            if (batchInsert == null) {
                continue;
            }
            String sql = batchInsert.sql();
            if (StringUtils.hasText(sql)) {
                RawSqlSource rawSqlSource = new RawSqlSource(configuration, sql, batchInsert.paramType());
                MappedStatement.Builder builder = new MappedStatement.Builder(configuration, id, rawSqlSource, SqlCommandType.INSERT);
                configuration.addMappedStatement(builder.build());
                BatchInsertContext.addBatchInsertMapperStatement(id, batchInsert);
                builder = new MappedStatement.Builder(configuration, id + ".singleInsert", rawSqlSource, SqlCommandType.INSERT);
                configuration.addMappedStatement(builder.build());
            } else {
                throw new BuilderException("batchInsert.sql() must not be blank, please check method: " + id);
            }
        }
        methodList.clear();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
