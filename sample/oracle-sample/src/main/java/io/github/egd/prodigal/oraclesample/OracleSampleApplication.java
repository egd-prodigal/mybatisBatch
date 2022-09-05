package io.github.egd.prodigal.oraclesample;

import io.github.egd.prodigal.oraclesample.mapper.ITestMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
@MapperScan("io.github.egd.prodigal.oraclesample.mapper")
public class OracleSampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(OracleSampleApplication.class, args);
    }

    @Autowired
    private ITestMapper testMapper;

    @Bean
    public ApplicationRunner runner() {
        return args -> System.out.println(testMapper.getSysdate());
    }

}
