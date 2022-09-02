# mybatisBatch
Batch Insert for Mybatis
Mybatis 批量插入插件，提供更简化的批量insert插件

## 使用方式
### **springboot** 项目 
增加如下依赖：
```xml
<dependency>
  <groupId>io.github.egd-prodigal</groupId>
  <artifactId>mybatis-batch-starter</artifactId>
  <version>1.0</version>
</dependency>
```

在Mapper的批量插入方法上加上注解 **@BatchInsert** ，注解参数 _collection_ 表示方法入参的集合对象，注解参数 _item_ 表示sql里的参数对象，_batchSize_ 表示批量提交的数量，如下所示：
```java
@Insert({"insert into test (id, name)", "values", "(#{po.id}, #{po.name})"})
@BatchInsert(collection = "testPOS", item = "po", batchSize = 1000)
void batchInsert(@Param("testPOS") List<TestPO> po);
```
上面的代码功能与下面的一致：
```java
@Insert({"<script>",
        "insert into test(id, name) values ",
        "<foreach collection='testPOS' index='index' item='po' separator=','>",
        "(#{po.id}, #{po.name})",
        "</foreach>",
        "</script>"})
void forEachInsert(@Param("testPOS") List<TestPO> po);
```
实测batch方式性能更佳

### 非 **springboot** 项目 
 配置插件，增加 _BatchInsertInterceptor_ 插件，注意一定要配置插件的 _batchSqlSessionBuilder_ 为 _DefaultBatchSqlSessionBuilder_
