package cn.egd.prodigal.sample.repository.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import cn.egd.prodigal.sample.mybaits.annotation.BatchInsert;
import cn.egd.prodigal.sample.repository.entity.TestPO;

@Mapper
public interface ITestMapper {

	@Insert("insert into test (id, name) values (#{po.id}, #{po.name})")
	int insert(@Param("po") TestPO po);
	
	@Insert("")
	@BatchInsert(insert = "insert", batchSize = 100)
	int batchInsert(List<TestPO> po);
	
	
	
	
	
	
	@Insert.List(@Insert("insert into test (id, name) values (#{id}, #{name})"))
	int batchInsert2(List<TestPO> po);
	
	@Select("select * from test")
	List<TestPO> queryAll();
	
	@Delete("delete from test")
	int deleteAll();
	
}
