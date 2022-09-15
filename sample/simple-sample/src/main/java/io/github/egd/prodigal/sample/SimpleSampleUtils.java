package io.github.egd.prodigal.sample;

import io.github.egd.prodigal.mybatis.batch.core.BatchContext;
import io.github.egd.prodigal.mybatis.batch.core.BatchScanner;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class SimpleSampleUtils {

    private static SqlSessionFactory sqlSessionFactory;


    public synchronized static SqlSessionFactory getSqlSessionFactory(String environment) {
        if (sqlSessionFactory == null) {
            try {
                // 初始化SqlSessionFactory
                String resource = "mybatis-config.xml";
                InputStream inputStream = Resources.getResourceAsStream(resource);
                sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream, environment);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            BatchContext.setSqlSessionFactory(sqlSessionFactory);
            BatchScanner.addClass(ITestMapper.class);
            BatchScanner.scan();
        }
        return sqlSessionFactory;
    }

    public static void stressBatch(SqlSessionFactory sqlSessionFactory) {
        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            ITestMapper testMapper = sqlSession.getMapper(ITestMapper.class);
            testMapper.deleteAll();
            sqlSession.commit();

            List<TestPO> list = createList(100_0000);
            long start = System.currentTimeMillis();
            testMapper.batchInsert(list);
            System.out.println("batch: " + (System.currentTimeMillis() - start));
            System.out.println("count: " + testMapper.count());
            testMapper.deleteAll();
            sqlSession.commit();
        }
    }

    public static void stressForeach(SqlSessionFactory sqlSessionFactory) {
        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            ITestMapper testMapper = sqlSession.getMapper(ITestMapper.class);
            testMapper.deleteAll();
            sqlSession.commit();

            int size = 100_0000;
            List<TestPO> list = createList(size);
            long start = System.currentTimeMillis();
            int pageSize = 1000;
            for (int i = 0; i < size; ) {
                int toIndex = i + pageSize;
                testMapper.batchInsert(list.subList(i, Math.min(toIndex, size)));
                i += pageSize;
            }
            System.out.println("batch: " + (System.currentTimeMillis() - start));
            System.out.println("count: " + testMapper.count());
            testMapper.deleteAll();
            sqlSession.commit();
        }
    }

    private static List<TestPO> createList(int size) {
        List<TestPO> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            TestPO po = new TestPO();
            po.setId(i + 1);
            po.setName("yeemin-" + po.getId());
            list.add(po);
        }
        return list;
    }

}
