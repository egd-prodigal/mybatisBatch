package io.github.egd.prodigal.sample.repository.mapper;

import io.github.egd.prodigal.mybatis.batch.annotations.BatchInsert;
import io.github.egd.prodigal.sample.repository.entity.TestPO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ITestMapper {

    @Insert("insert into test (id, name) values (#{po.id}, #{po.name})")
    int insert(@Param("po") TestPO po);

    @Insert({"insert into test (id, name)", "values", "(#{po.id}, #{po.name})"})
    @BatchInsert(collection = "testPOS", item = "po", batchSize = 1000)
    void batchInsert(@Param("testPOS") List<TestPO> po);

    @Insert({"<script>",
            "insert into test(id, name) values ",
            "<foreach collection='testPOS' index='index' item='po' separator=','>",
            "(#{po.id}, #{po.name})",
            "</foreach>",
            "</script>"})
    void forEachInsert(@Param("testPOS") List<TestPO> po);

    //	不知道这玩意怎么用
    @Insert.List(@Insert("insert into test (id, name) values (#{id}, #{name})"))
    int batchInsert2(List<TestPO> po);

    @Select("select * from test")
    List<TestPO> queryAll();

    @Delete("delete from test")
    int deleteAll();

    @Select("select count(*) from test")
    int count();

}
