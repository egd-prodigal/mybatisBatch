# mybatisBatch

Batch Insert for Mybatis Mybatis 批量保存插件，提供更简化的批量insert插件

## 使用方式

### **springboot** 项目

增加如下依赖：

```xml
<dependency>
    <groupId>io.github.egd-prodigal</groupId>
    <artifactId>mybatis-batch-starter</artifactId>
    <version>1.2.2</version>
</dependency>
```

在Mapper的批量插入方法上加上注解 **@BatchInsert** ，注解参数 _collection_ 表示方法入参的集合对象，注解参数 _item_
表示sql里的参数对象，_batchSize_ 表示批量提交的数量，如下所示：

```java
@Insert({"insert into test (id, name)", "values", "(#{po.id}, #{po.name})"})
@BatchInsert(collection = "testPOS", item = "po", batchSize = 1000)
void batchInsert(@Param("testPOS") List<TestPO> po);
```

上面的代码功能与下面的一致（sql语法因数据库而异）：

```java
@Insert({"<script>",
        "insert into test(id, name) values ",
        "<foreach collection='testPOS' index='index' item='po' separator=','>",
        "(#{po.id}, #{po.name})",
        "</foreach>",
        "</script>"})
void forEachInsert(@Param("testPOS") List<TestPO> po);
```

除了基于 **@Insert** 注解的编程方式，还支持 **@InsertProvider** 和 **xml** 的方式，只需在对应的Mapper接口的方法上增加 **@BatchInsert** 注解即可。  
另外，**@BatchInsert** 还提供了一个参数 _insert_ ，用以指明单条保存的方法，这样批量保存方法可以不用写@Insert以及sql代码，使用方式如下所示：

```java
@Insert({"insert into test (id, name)", "values", "(#{po.id}, #{po.name})"})
void insertOne(@Param("po") TestPO po);

// 这个方法可以不写@Insert注解了，insertOne指上面的那个方法
// 方法入参集合的对象类型可以不用跟insertOne一致，只要拥有必要的属性就行
// item必须要跟insertOne里配置的一致
@BatchInsert(insert = "insertOne", collection = "testPOS", item = "po", batchSize = 1000)
void batchInsert(@Param("testPOS") List<TestPO> po);
```

**@BatchInsert** 注解使用时，如果指定了 _insert_ 参数的同时，方法也拥有 **@Insert** 注解，取 _insert_ 参数配置的方法。  
启动时不会检查正确性，后续可以考虑加上部分校验规则，如果编写有误，将会在执行时抛出相应异常。

> 注意：由于本项目的批量是基于Mybatis的BATCH模式，并手动批量提交已执行的部分sql，所以 **不建议在强事务性业务中使用本插件** ，如果使用了，并且遇到问题了，欢迎联系开发者修复。  
> 建议在各种 _异步批量保存_ 的场景下使用，以及批量添加后不存在查询和其他对插入的数据操作的场景。
> 关于事务问题请往下看。

### 非 **springboot** 项目

增加如下依赖

```xml
<dependency>
    <groupId>io.github.egd-prodigal</groupId>
    <artifactId>mybatis-batch</artifactId>
    <version>1.2.2</version>
</dependency>
```

在配置文件 **mybatis-config.xml** （也可以是其他文件名）里增加插件配置：

```xml
<plugins>
    <plugin interceptor="io.github.egd.prodigal.mybatis.batch.plugins.BatchInsertInterceptor"/>
</plugins>
```

如果是spring项目，基于javaConfig装配的Bean，可以在手动装配SqlSessionFaction的地方直接添加interceptor，注意手动设置batchSqlSessionBuilder。

编写mybatis初始化代码，基于xml配置生成SqlSessionFactory，然后添加如下代码：

```java
// 此处的sqlSessionFactory在之前的代码里生成，也可以是spring项目注入进来的
BatchInsertContext.setSqlSessionFactory(sqlSessionFactory);
// 添加拥有批量保存方法的Mapper接口类，可以传多个类，可以在任意位置调用
BatchInsertScanner.addClass(ITestMapper.class);
// 扫描批量保存方法，可以在任意位置任何使用调用，
// 每次调用都只会扫描上次调用scan之后调用addClass添加的新的Mapper接口类
BatchInsertScanner.scan();
```

然后就可以使用本插件编写批量保存方法了，如果是spring项目，也需要编写上面几行代码。

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

### 事务问题

上面提到的 **不建议在强事务性业务中使用本插件** 的警告，是因为在批量的模式下，当数据量足够大时，数据会一批一批的刷入数据库，
压力测试时频繁查询数据库可以实际观察到这一现象，数据会以 _batchSize_ 配置的参数递增。  
并且由于本插件是单独开启了一个 _SqlSession_ ，而其他正常的业务操作都使用了默认的 _SqlSession_ ，这两个会话不主动 **互相** 共享事务，
但是事实上，后创建的 _SqlSession_ 拥有先创建的 _SqlSession_ 已执行的数据状态，
也就是说后创建的批量模式的 _SqlSession_ 在执行时有能力读到先创建的 _SqlSession_ 已执行的sql语句的结果，
但后创建的 _SqlSession_ 执行的数据更新操作，离开这个会话后返回至先创建的 _SqlSession_ 会话时，无法感知到它已执行的语句的结果，
但是如果后创建的 _SqlSession_ 执行了 **flushStatements** 操作，那么先创建的 _SqlSession_ 的后续操作将会感知到它的执行结果。  
例如如果先创建的 _SqlSession_ 再执行保存相同主键的数据，如果后创建的 _SqlSession_ 针对这条数据没有执行 **flushStatements** ，
那么执行时不会报错，但是事务提交时会报错，并回滚两个会话的事务；如果后创建的 _SqlSession_ 针对这条数据执行了 **flushStatements** ，
那么执行时会直接报错。也就是说使用 **flushStatements** 可以让后发生的 _SqlSession_ 的结果被先发生的 _SqlSession_ 感知到。  
因此，注解 **@BatchInsert** 提供了 _flushStatements_ 参数，默认为true，表示是否预刷入数据库。  
关于事务问题示例如下:
1. 批量保存感知到之前执行的结果
```java
// 直接以主键为1插入数据库，此时是在默认的SqlSession，即先创建的SqlSession里执行
testMapper.insert(1);
List<Integer> list = new ArrayList<>();
list.add(1);
list.add(2);
// 假设这是一个批量保存的方法，会创建一个新的SqlSession，但是它能感知到之前insert的数据，所以会抛出主键冲突的异常
testMapper.batchInsert(list);
```
2. 单个保存感知到批量保存的结果
```java
List<Integer> list = new ArrayList<>();
list.add(1);
list.add(2);
// 假设这是一个批量保存的方法，会创建一个新的SqlSession
testMapper.batchInsert(list);
// 直接以主键为1插入数据库，此时是在默认的SqlSession，即先创建的SqlSession里执行，
// 由于本插件默认执行了flushStatements，所以这里将会抛出主键冲突异常
// 如果testMapper.batchInsert配置了flushStatements为false，且执行的数据小于batchSize，则不会抛出异常，而是在事务提交时抛出
testMapper.insert(1);
```

### 其他
Mybatis-Plus已经实现了本插件提供的功能，考虑到项目组开发习惯，并未引入Mybatis-Plus，故而开发此插件。  
版本：本插件开发时基于Mybatis: 3.5.9，SpringBoot： 2.7.3， Spring：5.3.22，实测可兼容版本为：Mybatis: 3.4.5，SpringBoot： 2.1.3， Spring：5.1.5，可能还能兼容更低的版本，欢迎测试。
