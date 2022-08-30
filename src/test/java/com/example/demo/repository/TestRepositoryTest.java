package com.example.demo.repository;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.demo.repository.dao.TestBatchDao;
import com.example.demo.repository.entity.TestPO;
import com.example.demo.repository.mapper.ITestMapper;

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
		List<TestPO> list = new ArrayList<>();
		for (int i = 0; i < 100; i++) {
			TestPO po = new TestPO();
			po.setId(i + 1);
			po.setName("yeemin");
			list.add(po);
		}
		testMapper.batchInsert(list);
		System.out.println(testMapper.queryAll());
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
