package io.github.egd.prodigal.sample;

import io.github.egd.prodigal.mybatis.batch.config.MybatisBatchConfiguration;
import org.h2.jdbcx.JdbcConnectionPool;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class SpringSampleApplication {

    public static void main(String[] args) {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext("io.github.egd.prodigal.sample",
                "io.github.egd.prodigal.mybatis.batch.config");
//        applicationContext.register(MybatisBatchConfiguration.class);

        ITestMapper testMapper = applicationContext.getBean(ITestMapper.class);

        testMapper.deleteAll();
        int size = 100;
        List<TestPO> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            TestPO po = new TestPO();
            po.setId(i + 1);
            po.setName("yeemin-" + po.getId());
            list.add(po);
        }
        long start = System.currentTimeMillis();
        int i = testMapper.batchInsert2(list);
        System.out.println("batchCount: " + i);

        testMapper.deleteAll();

        testMapper.batchInsert(list);
        System.out.println("batch: " + (System.currentTimeMillis() - start));
        System.out.println("count: " + testMapper.count());
    }

    @Bean
    public DataSource dataSource() {
        return JdbcConnectionPool.create("jdbc:h2:~/h2/mybatisbatch", "sa", "sa");
    }

    @Bean
    public SqlSessionFactoryBean sqlSessionFactoryBean(@Autowired DataSource dataSource) {
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(dataSource);
        sqlSessionFactoryBean.setMapperLocations(new ClassPathResource("mapper/ITestMapper.xml"));
        return sqlSessionFactoryBean;
    }

    @Bean
    public MapperScannerConfigurer mapperScannerConfigurer() {
        MapperScannerConfigurer mapperScannerConfigurer = new MapperScannerConfigurer();
        mapperScannerConfigurer.setBasePackage("io.github.egd.prodigal.sample");
        return mapperScannerConfigurer;
    }


}
