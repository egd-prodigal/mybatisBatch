package cn.egd.prodigal.sample.repository.mapper;

import java.util.List;

import cn.egd.prodigal.mybatis.batch.annotations.BatchInsert;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import cn.egd.prodigal.sample.repository.entity.TestPO;

@Mapper
public interface ITestMapper {

	@Insert("insert into test (id, name) values (#{po.id}, #{po.name})")
	int insert(@Param("po") TestPO po);
	
	@Insert("")
	@BatchInsert(insert = "insert", batchSize = 500, listParamName = "testPOS")
	void batchInsert(@Param("testPOS") List<TestPO> po);

	@Insert({"<script>",
			"insert into test(id, name) values ",
			"<foreach collection='testPOS' index='index' item='po' separator=','>",
			"(#{po.id}, #{po.name})",
			"</foreach>",
			"</script>"})
	int forEachInsert(@Param("testPOS") List<TestPO> po);

//	不知道这玩意怎么用
	@Insert.List(@Insert("insert into test (id, name) values (#{id}, #{name})"))
	int batchInsert2(List<TestPO> po);
	
	@Select("select * from test")
	List<TestPO> queryAll();
	
	@Delete("delete from test")
	int deleteAll();
	
}
