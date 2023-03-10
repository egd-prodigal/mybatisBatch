package io.github.egd.prodigal.mybatis.batch.annotations;

import org.apache.ibatis.annotations.Param;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 批量插入注解，与{@link org.apache.ibatis.annotations.Insert}配合使用，添加本注解的插入方法，将被认为是一个批量插入的方法，
 * 本插件将会开启mybatis的批量模式，轮询参数执行单个插入动作，并在满足插入一定数量后提交一次。
 * <p>
 * {@link #collection()} ()} 指定方法入参的对象名，与方法参数的{@link Param#value()}的值一致
 * {@link #item()} 指定sql语句里的实体类的参数名，不写此值可以直接用对应的参数名
 * {@link #batchSize()} 指定批量一次提交的数据量
 * {@link #insert()} 指定单条保存的方法，必须在相同Mapper接口类里
 * {@link #flushStatements()} 是否把已执行的数据刷入数据库
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BatchInsert {

    /**
     * 集合入参的名称，与{@link org.apache.ibatis.annotations.Param}注解配置的值一致，为空则默认取参数里的集合对象
     *
     * @return 方法入参配置的集合对象名 {@link Param}
     */
    String collection() default "collection";

    /**
     * sql里的参数名
     *
     * @return String
     */
    String item() default "";

    /**
     * 批量大小，传入的数据，按该值再分批提交
     *
     * @return int
     */
    int batchSize() default 500;

    /**
     * 指定其他方法作为单条插入的方法，这个方法必须存在，且本方法入参数量符合指定的方法，否则执行报错
     *
     * @return 本接口类里的其他单条插入的方法名
     */
    String insert() default "";

    /**
     * 是否把已执行的数据刷入数据库
     *
     * @return boolean
     */
    boolean flushStatements() default true;

    /**
     * 是否直接提交事务
     *
     * @return boolean
     */
    boolean autoCommit() default false;

}
