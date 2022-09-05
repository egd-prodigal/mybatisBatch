# mybatisBatch

Batch Insert for Mybatis Mybatis 批量保存插件，提供更简化的批量insert插件

## 使用方式

### **springboot** 项目

增加如下依赖：

```xml

<dependency>
    <groupId>io.github.egd-prodigal</groupId>
    <artifactId>mybatis-batch-starter</artifactId>
    <version>1.1.1</version>
</dependency>
```

在Mapper的批量插入方法上加上注解 **@BatchInsert** ，注解参数 _collection_ 表示方法入参的集合对象，注解参数 _item_
表示sql里的参数对象，_batchSize_ 表示批量提交的数量，如下所示：

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

除了基于 **@Insert** 注解的编程方式，还支持 **@InsertProvider** 和 **xml** 的方式，只需在对应的Mapper接口的方法上增加 **
@BatchInsert** 注解即可。  
另外，**@BatchInsert** 还提供了一个参数 _insert_ ，用以指明单条保存的方法，这样批量保存方法可以不用写@Insert以及sql代码，使用方式如下所示：

```java
...
@Insert({"insert into test (id, name)", "values", "(#{po.id}, #{po.name})"})
void insertOne(@Param("po") TestPO po);

// 这个方法可以不写@Insert注解了，insertOne指上面的那个方法
// 方法入参集合的对象类型可以不用跟insertOne一致，只要拥有必要的属性就行
// item必须要跟insertOne里配置的一致
@BatchInsert(insert = "insertOne", collection = "testPOS", item = "po", batchSize = 1000)
void batchInsert(@Param("testPOS") List<TestPO> po);
        ...
```

**@BatchInsert** 注解使用时，如果指定了 _insert_ 参数的同时，方法也拥有 **@Insert** 注解，取 _insert_ 参数配置的方法。  
启动时不会检查正确性，如果编写有误，将会在执行时抛出相应异常。

> 注意：由于本项目的批量是基于Mybatis的BATCH模式，并自行提交，所以 **不要在强事务性业务使用本插件** ，遇到问题后果自负。  
> 建议在各种 _异步批量保存_ 的场景下使用。

### 非 **springboot** 项目

增加如下依赖

```xml

<dependency>
    <groupId>io.github.egd-prodigal</groupId>
    <artifactId>mybatis-batch</artifactId>
    <version>1.1.1</version>
</dependency>
```

在配置文件 **mybatis-config.xml** （也可以是其他文件名）里增加插件配置：

```xml
...

<plugins>
    <plugin interceptor="io.github.egd.prodigal.mybatis.batch.plugins.BatchInsertInterceptor"/>
</plugins>

        ...
```

如果是spring项目，基于javaConfig装配的Bean，可以在手动装配SqlSessionFaction的地方直接添加intercepter，注意手动设置batchSqlSessionBuilder。

编写mybatis初始化代码，基于xml配置生成SqlSessionFactory，然后添加如下代码：

```java
// 此处的sqlSessionFactory在之前的代码里生成，也可以是spring项目注入进来的
BatchInsertContext.setSqlSessionFactory(sqlSessionFactory);
// 添加拥有批量保存方法的Mapper接口类，相信此类场景不会太多，可以在任意位置调用
        BatchInsertScanner.addClass(ITestMapper.class);
// 扫描批量保存方法，可以在任意位置任何使用调用，
// 每次调用都只会扫描上次调用scan之后调用addClass添加的新的Mapper接口类
        BatchInsertScanner.scan();
```

然后就可以使用本插件编写批量保存方法了，如果是spring项目，也需要编写上面几行代码。这类场景较少，暂不提供更便利的方式。

### 性能测试

实测batch方式性能方面以微弱的优势胜出，但从编码难度来看，batch方式显然更友好。
> 性能测试数据  
> 一次性插入1000_000条数据，1000条一批（batch方式由插件自行轮询，foreach手动分页），测试3次，batch与foreach方式耗时如下：  
> mysql数据库（注意mysql数据库连接字符串一定要加上参数:  **rewriteBatchedStatements=true**，否则批量插入无效）：  
> 次数|batch|耗时（毫秒）|foreach|耗时（毫秒）
> ----|----|----|----|----
> 1|batch|39509|foreach|46933
> 2|batch|42421|foreach|42349
> 3|batch|41450|foreach|51032
>
> oracle数据库（oracle性能果然高）：  
> 次数|batch|耗时（毫秒）|foreach|耗时（毫秒）
> ----|----|----|----|----
> 1|batch|10537|foreach|13239
> 2|batch|8927|foreach|12498
> 3|batch|10699|foreach|14566

### 开发者

> 生一鸣，曾广霏，宋祥富
