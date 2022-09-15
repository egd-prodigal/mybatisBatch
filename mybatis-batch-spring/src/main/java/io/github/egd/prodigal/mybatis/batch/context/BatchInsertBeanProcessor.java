package io.github.egd.prodigal.mybatis.batch.context;

import io.github.egd.prodigal.mybatis.batch.annotations.BatchInsert;
import io.github.egd.prodigal.mybatis.batch.core.BatchContext;
import io.github.egd.prodigal.mybatis.batch.core.BatchScanner;
import io.github.egd.prodigal.mybatis.batch.plugins.BatchInsertInterceptor;
import io.github.egd.prodigal.mybatis.batch.plugins.BatchInterceptor;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * 批量保存Bean处理器，
 * Spring自动装配使用，先获取方法拥有{@link BatchInsert}注解的Mapper接口内方法，再解析方法注解，
 * 注册单个或者批量保存的MappedStatement
 */
public class BatchInsertBeanProcessor implements BeanPostProcessor, SmartInitializingSingleton {

    /**
     * 批量保存拦截器
     */
    private BatchInsertInterceptor batchInsertInterceptor;

    /**
     * 批量操作拦截器
     */
    private BatchInterceptor batchInterceptor;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof MapperFactoryBean) {
            // mybatis自动装配的Bean都是MapperFactoryBean
            MapperFactoryBean<?> mapperFactoryBean = (MapperFactoryBean<?>) bean;
            Class<?> mapperInterface = mapperFactoryBean.getMapperInterface();
            // 遍历Mapper接口类的所有方法并缓存拥有BatchInsert注解的方法
            Method[] declaredMethods = ReflectionUtils.getAllDeclaredMethods(mapperInterface);
            Arrays.stream(declaredMethods).filter(method -> AnnotationUtils.getAnnotation(method, BatchInsert.class) != null).forEach(BatchScanner::addMethod);
        }
        if (bean instanceof SqlSessionFactory) {
            // 批量保存上下文设置SqlSessionFactory
            BatchContext.setSqlSessionFactory((SqlSessionFactory) bean);
            // 设置运行环境为spring
            BatchContext.setInSpring();
            // 设置批量SqlSession构建器
            BatchContext.setBatchSqlSessionBuilder(new SpringBatchSqlSessionBuilder());
        }
        return bean;
    }

    @Override
    public void afterSingletonsInstantiated() {
        // 扫描注册的批量保存方法
        BatchScanner.scan();
        if (getBatchInsertInterceptor() != null) {
            // 确认一下是否注册了拦截器，有时候会有开发者自己手动装配SqlSessionFactory
            Configuration configuration = BatchContext.getSqlSessionFactory().getConfiguration();
            List<Interceptor> interceptors = configuration.getInterceptors();
            boolean b = interceptors.stream().anyMatch(e -> e instanceof BatchInsertInterceptor);
            if (!b) {
                configuration.addInterceptor(getBatchInsertInterceptor());
            }
        }
        if (getBatchInterceptor() != null) {
            // 确认一下是否注册了拦截器，有时候会有开发者自己手动装配SqlSessionFactory
            Configuration configuration = BatchContext.getSqlSessionFactory().getConfiguration();
            List<Interceptor> interceptors = configuration.getInterceptors();
            boolean b = interceptors.stream().anyMatch(e -> e instanceof BatchInterceptor);
            if (!b) {
                configuration.addInterceptor(getBatchInterceptor());
            }
        }
    }


    public BatchInsertInterceptor getBatchInsertInterceptor() {
        return batchInsertInterceptor;
    }

    public void setBatchInsertInterceptor(BatchInsertInterceptor batchInsertInterceptor) {
        this.batchInsertInterceptor = batchInsertInterceptor;
    }

    public BatchInterceptor getBatchInterceptor() {
        return batchInterceptor;
    }

    public void setBatchInterceptor(BatchInterceptor batchInterceptor) {
        this.batchInterceptor = batchInterceptor;
    }

}
