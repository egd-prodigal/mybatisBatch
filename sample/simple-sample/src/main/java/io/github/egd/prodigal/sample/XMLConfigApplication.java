package io.github.egd.prodigal.sample;

import io.github.egd.prodigal.mybatis.batch.core.BatchInsertContext;
import io.github.egd.prodigal.mybatis.batch.core.BatchInsertScanner;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class XMLConfigApplication {

    private static final SqlSessionFactory sqlSessionFactory;

    static {
        try {
            // 初始化SqlSessionFactory
            String resource = "mybatis-config.xml";
            InputStream inputStream = Resources.getResourceAsStream(resource);
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        BatchInsertContext.setSqlSessionFactory(sqlSessionFactory);
        BatchInsertScanner.addClass(ITestMapper.class);
        BatchInsertScanner.scan();
    }

    public static void main(String[] args) {
        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            ITestMapper testMapper = sqlSession.getMapper(ITestMapper.class);

            testMapper.deleteAll();
            sqlSession.commit();

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
            sqlSession.commit();

            testMapper.batchInsert(list);
            System.out.println("batch: " + (System.currentTimeMillis() - start));
            System.out.println("count: " + testMapper.count());
        }
    }

}
