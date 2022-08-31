package io.github.egd.prodigal.sample;

import io.github.egd.prodigal.mybatis.batch.config.EnableMybatisBatch;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableMybatisBatch
public class MybatisBatchApplication {

	public static void main(String[] args) {
		SpringApplication.run(MybatisBatchApplication.class, args);
	}

}
