package io.github.egd.prodigal.sample.test;

import io.github.egd.prodigal.sample.repository.dao.TestBatchDao;
import io.github.egd.prodigal.sample.repository.entity.TestPO;
import io.github.egd.prodigal.sample.repository.mapper.ITestMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
public class TestRepositoryTest {

    @Autowired
    private ITestMapper testMapper;

    @Autowired
    private TestBatchDao testBatchDao;

    @Test
    public void insert() {
        deleteAll();
        TestPO po = new TestPO();
        po.setId(1);
        po.setName("yeemin");
        testMapper.insert(po);
        System.out.println(testMapper.queryAll());
    }

    @Test
    public void queryAll() {
        List<TestPO> list = testMapper.queryAll();
        System.out.println(list);
    }

    @Test
    public void deleteAll() {
        testMapper.deleteAll();
    }

    @Test
    public void batchInsert() {
        deleteAll();
        List<TestPO> list = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            TestPO po = new TestPO();
            po.setId(i + 1);
            po.setName("yeemin");
            list.add(po);
        }
        testBatchDao.batchInsert(list);
        list = testMapper.queryAll();
        System.out.println(list);
    }

    @Test
    public void batchInsertMapper() {
        deleteAll();
        int size = 100;
        List<TestPO> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            TestPO po = new TestPO();
            po.setId(i + 1);
            po.setName("yeemin");
            list.add(po);
        }
        long start = System.currentTimeMillis();
        testMapper.batchInsert("test", list);
        System.out.println("batch: " + (System.currentTimeMillis() - start));
		System.out.println("count: " + testMapper.count());
    }


    @Test
    public void batchInsertMapper3() {
        deleteAll();
        List<TestPO> list = new ArrayList<>();
        int size = 10000000;
        for (int i = 0; i < size; i++) {
            TestPO po = new TestPO();
            po.setId(i + 1);
            po.setName("yeemin");
            list.add(po);
        }
        long start = System.currentTimeMillis();
        int count = 0;
        while (count + 500 < size) {
            testMapper.forEachInsert(list.subList(count, count + 500));
            count += 500;
        }
        if (count < size) {
            testMapper.forEachInsert(list.subList(count, size));
        }
        System.out.println("foreach: " + (System.currentTimeMillis() - start));
//		System.out.println(testMapper.queryAll());
    }

    @Test
    public void batchInsertMapper2() {
        deleteAll();
        List<TestPO> list = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            TestPO po = new TestPO();
            po.setId(i + 1);
            po.setName("yeemin");
            list.add(po);
        }
        try {
            testMapper.batchInsert2(list);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(testMapper.queryAll());
    }


}
