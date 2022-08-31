package cn.egd.prodigal.mybatis.batch.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 批量插入注解，与{@link org.apache.ibatis.annotations.Insert}作用类似，使用本注解的方法，将被认为是一个批量插入的方法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BatchInsert {

    /**
     * sql语句，与常规mybatis写法一致
     * @return String
     */
    String sql();

    /**
     * 参数类型，入参必须以实体类集合的方式传入，在此处标记参数类型
     * @return Class
     */
    Class<?> paramType();

    /**
     * 集合入参的名称，与{@link org.apache.ibatis.annotations.Param}注解配置的值一致，为空则默认取参数里的集合对象
     * @return String
     */
    String listParamName() default "collection";

    /**
     * 批量大小，传入的数据，按{@link #batchSize()}的值再分批提交
     * @return int
     */
    int batchSize() default 500;

}
