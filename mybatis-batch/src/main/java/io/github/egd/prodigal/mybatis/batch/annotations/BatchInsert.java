package io.github.egd.prodigal.mybatis.batch.annotations;

import org.apache.ibatis.annotations.Param;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 批量插入注解，与{@link org.apache.ibatis.annotations.Insert}作用类似，使用本注解的方法，将被认为是一个批量插入的方法，
 * 本插件将会开启mybatis的批量模式，轮询参数执行单个插入动作，并在满足插入一定数量后提交一次。<br/>
 * {@link #paramType()} 指定实体类的类型，必须要有<br/>
 * {@link #paramName()} 指定sql语句里的实体类的参数名，不写此值可以直接用对应的参数名<br/>
 * {@link #listParamName()} ()} 指定方法入参的对象名，与方法参数的{@link Param#value()}的值一致<br/>
 * {@link #batchSize()} 指定批量一次提交的数据量<br/>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BatchInsert {

    /**
     * 参数类型，入参必须以实体类集合的方式传入，在此处标记参数类型
     *
     * @return Class
     */
    Class<?> paramType();

    /**
     * sql里的参数名
     *
     * @return String
     */
    String paramName() default "";

    /**
     * 集合入参的名称，与{@link org.apache.ibatis.annotations.Param}注解配置的值一致，为空则默认取参数里的集合对象
     *
     * @return String
     */
    String listParamName() default "collection";

    /**
     * 批量大小，传入的数据，按该值再分批提交
     *
     * @return int
     */
    int batchSize() default 500;

}
