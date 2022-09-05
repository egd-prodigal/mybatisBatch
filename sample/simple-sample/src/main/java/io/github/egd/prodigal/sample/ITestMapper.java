package io.github.egd.prodigal.sample;

import io.github.egd.prodigal.mybatis.batch.annotations.BatchInsert;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ITestMapper {

    @Insert("insert into test (id, name) values (#{po.id}, #{po.name})")
    int insert(@Param("po") TestPO po);

    @Insert({"insert into test (id, name)", "values", "(#{po.id}, #{po.name})"})
    @BatchInsert(collection = "testPOS", item = "po", batchSize = 10, flushStatements = true)
    void batchInsert(@Param("testPOS") List<TestPO> po);

    @BatchInsert(insert = "insert", collection = "testPOS", item = "po", batchSize = 10)
    int batchInsert2(@Param("testPOS") List<TestPO> po);

    @BatchInsert(collection = "testPOS", item = "po", batchSize = 10)
    @InsertProvider(type = Provider.class, method = "providerBatch")
    void providerBatchInsert(@Param("testPOS") List<TestPO> po);

    @BatchInsert(collection = "testPOS", item = "po", batchSize = 10)
    void xmlBatchInsert(@Param("testPOS") List<TestPO> po);

    @Select("select * from test")
    List<TestPO> queryAll();

    @Delete("delete from test")
    void deleteAll();

    @Select("select count(*) from test")
    int count();


    class Provider {

        public String providerBatch(@Param("testPOS") List<TestPO> po) {
            return "insert into test (id, name) values (#{po.id}, #{po.name})";
        }

    }

}
