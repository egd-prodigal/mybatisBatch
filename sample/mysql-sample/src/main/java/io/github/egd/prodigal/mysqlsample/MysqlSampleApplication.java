package io.github.egd.prodigal.mysqlsample;

import io.github.egd.prodigal.mysqlsample.mapper.ITestMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@MapperScan("io.github.egd.prodigal.mysqlsample.mapper")
public class MysqlSampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(MysqlSampleApplication.class, args);
    }

    @Autowired
    private ITestMapper testMapper;

    @Bean
    public ApplicationRunner runner() {
        return args -> System.out.println(testMapper.getSysdate());
    }

}
