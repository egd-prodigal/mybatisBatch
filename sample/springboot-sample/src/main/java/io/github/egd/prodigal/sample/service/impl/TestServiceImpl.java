package io.github.egd.prodigal.sample.service.impl;

import io.github.egd.prodigal.sample.repository.entity.TestPO;
import io.github.egd.prodigal.sample.repository.mapper.ITestMapper;
import io.github.egd.prodigal.sample.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class TestServiceImpl implements TestService {

    @Autowired
    private ITestMapper testMapper;

    @Override
    @Transactional
    public void test() {
        testMapper.deleteAll();
        int size = 105;
        List<TestPO> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            TestPO po = new TestPO();
            po.setId(i + 1);
            po.setName("yeemin");
            list.add(po);
        }
        testMapper.batchInsert(list);
        TestPO testPO = new TestPO();
        testPO.setId(106);
        testPO.setName("yeemin");
        testMapper.insert(testPO);
        System.out.println("count: " + testMapper.count());
    }

    @Override
    public int count() {
        return testMapper.count();
    }

}
