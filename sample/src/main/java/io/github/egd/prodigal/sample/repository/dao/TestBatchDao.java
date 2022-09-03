package io.github.egd.prodigal.sample.repository.dao;

import io.github.egd.prodigal.sample.repository.entity.TestPO;
import io.github.egd.prodigal.sample.repository.mapper.ITestMapper;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TestBatchDao {

    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    public void batchInsert(List<TestPO> list) {
        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH, false);
        ITestMapper testMapper = sqlSession.getMapper(ITestMapper.class);
        for (int i = 0; i < list.size(); i++) {
            testMapper.insert(list.get(i));
            if (i % 10 == 9) {
                sqlSession.commit();
                sqlSession.clearCache();
            }
        }
        sqlSession.commit();
        sqlSession.clearCache();
    }

}
