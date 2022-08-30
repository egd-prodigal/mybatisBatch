package cn.egd.prodigal.mybatis.batch.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BatchInsert {
	
	String insert();
	
	int batchSize() default 500;

	String listParamName() default "collection";

}
