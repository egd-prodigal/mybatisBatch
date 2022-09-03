package io.github.egd.prodigal.sample.test;

import io.github.egd.prodigal.mybatis.batch.core.BatchInsertContext;
import io.github.egd.prodigal.mybatis.batch.core.BatchInsertScanner;
import io.github.egd.prodigal.mybatis.batch.core.DefaultBatchSqlSessionBuilder;
import io.github.egd.prodigal.mybatis.batch.plugins.BatchInsertInterceptor;
import io.github.egd.prodigal.sample.repository.entity.TestPO;
import io.github.egd.prodigal.sample.repository.mapper.ITestMapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class TestWithoutSpring {

    @Test
    public void test() throws IOException {
        String resource = "nospring/mybatis-config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);

        BatchInsertContext.setSqlSessionFactory(sqlSessionFactory);
        BatchInsertScanner.addClass(ITestMapper.class);
        BatchInsertScanner.scan();

        SqlSession sqlSession = sqlSessionFactory.openSession();
        ITestMapper testMapper = sqlSession.getMapper(ITestMapper.class);

        testMapper.deleteAll();
        sqlSession.commit();

        int size = 100;
        List<TestPO> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            TestPO po = new TestPO();
            po.setId(i + 1);
            po.setName("yeemin");
            list.add(po);
        }
        long start = System.currentTimeMillis();
        testMapper.batchInsert2(list);

        testMapper.deleteAll();
        sqlSession.commit();

        testMapper.batchInsert(list);
        System.out.println("batch: " + (System.currentTimeMillis() - start));
        System.out.println("count: " + testMapper.count());

    }

}
