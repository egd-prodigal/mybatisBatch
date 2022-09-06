# mybatisBatch

Batch Insert for Mybatis Mybatis 批量保存插件，提供更简化的批量insert插件

## 使用指导

### 编码方式
在Mapper里用做批量保存的方法上添加注解 **@BatchInsert** ，并且这个方法映射的sql语句使用单条数据保存的，
注解参数 _collection_ 表示方法入参的集合对象，注解参数 _item_
表示sql里的参数对象，_batchSize_ 表示批量提交的数量，默认500，如下所示：

```java
@Insert({"insert into test (id, name)", "values", "(#{po.id}, #{po.name})"})
@BatchInsert(collection = "testPOS", item = "po", batchSize = 1000)
void batchInsert(@Param("testPOS") List<TestPO> po);
```
> 如果只有一个集合入参，可以不配置 **@Param**，并且不指定 _collection_，如果sql语句里的参数没有明确指定参数名， 可以不指定 _item_，
> 批量提交的数量也可以不指定值，如下所示：  
> @Insert("insert into test (id, name) values (#{id}, #{name})")  
> @BatchInsert  
> void batchInsert(List<TestPO> po);  

上面的代码功能与下面的一致（sql语法因数据库而异），但是性能更优（见[性能测试(#性能测试)]）：

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

> 注意：由于本项目的批量是基于Mybatis的BATCH模式，并手动批量提交已执行的部分sql， **不大建议在强事务性业务中使用本插件** ，如果使用中遇到了问题，欢迎联系开发者共同学习。  
> 建议在各种 _异步批量保存_ 的场景下使用，以及批量添加后不再数据操作的场景。  
> 本插件已尽最大可能性处理事务的问题，关于事务问题请跳到：[事务问题](#事务问题)。


### **springboot** 项目

增加如下依赖：

```xml
<dependency>
    <groupId>io.github.egd-prodigal</groupId>
    <artifactId>mybatis-batch-starter</artifactId>
    <version>2.0.2</version>
</dependency>
```
示例见项目：sample -> springboot-sample

### 纯 **spring** 项目

增加如下依赖

```xml
<dependency>
    <groupId>io.github.egd-prodigal</groupId>
    <artifactId>mybatis-batch-spring</artifactId>
    <version>2.0.2</version>
</dependency>
```
前提是已经在项目里整合好了Mybatis，然后在项目中使用下列任意一种配置：
1. 指定扫描包路径
    ```xml
    <context:component-scan  base-package="io.github.egd.prodigal.mybatis.batch.config"/>
    ```
2. 手动注册Bean
   1. 基于XML配置
      ```xml
      <bean class="io.github.egd.prodigal.mybatis.batch.config.MybatisBatchConfiguration"/>
      ```
   2. 基于JavaConfig
      ```java
      @Bean
      public MybatisBatchConfiguration mybatisBatchConfiguration() {
        return new MybatisBatchConfiguration();
      }
      ```
示例见项目：sample -> springboot-sample
### 非 **spring** 项目
增加如下依赖

```xml
<dependency>
    <groupId>io.github.egd-prodigal</groupId>
    <artifactId>mybatis-batch</artifactId>
    <version>2.0.2</version>
</dependency>
```
在配置文件 **mybatis-config.xml** （也可以是其他文件名）里增加插件配置：

```xml
<plugins>
    <plugin interceptor="io.github.egd.prodigal.mybatis.batch.plugins.BatchInsertInterceptor"/>
</plugins>
```

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
示例见项目：sample -> simple-sample

### 事务问题

上面提到的 **不大建议在强事务性业务中使用本插件** ，注意有个 **'强'**，主要是为了避免大量数据保存的情况下，事务一次提交过多数据导致数据库压力过大，
应用服务等待时间过长导致的业务接口不稳定的现象。  
实际上，本插件支持事务的特性，由mybatis自身的特性提供，但是通常我们把事务交给 **spring** 事务管理框架，由 **spring** 统一管理。  
本插件的核心是使用一个批量模式的 **SqlSession** 执行单条保存的 **MappedStatement** ，但是在实际业务过程中，其他的数据库访问代码
是有一个默认的 **SqlSession** 实现。  
**SqlSession**顾名思义就是sql会话，正常思维下，多个会话不能共享数据，事实上也是如此，多个**SqlSession** 之间不能直接互相感知对方的操作，
但是mybatis对**SqlSession**提供了 _flushStatements()_ 方法，这是个神奇的方法， 在无事务的情况下执行该方法，数据将会直接写入数据库，
在有事务管理的情况下执行该方法，它将会把自己会话里的数据库操作 _‘共享’_ 给当前会话， 底层是调用 **java.sql.Statement** 的
_executeBatch()_ 方法，由各个数据库驱动实现方法逻辑，因此本插件对事务控制的实际表现 也因数据库而异，但不管使用什么数据库，
普通数据库访问方法跟批处理操作都被一个事务管理着，要么一起成功要么一起失败。    
因此通过 _flushStatements()_ 方法可以实现多个会话间互相感知对方对数据库的操作，并且这些会话也被相同的事务管理器控制。  
综上，本插件的注解 **@BatchInsert** 提供了 _flushStatements_ 参数，默认为true，表示是否预执行sql，当然但哪怕设置成false了，
当一次保存的数据大于配置的 _batchSize_ 时，还是会有一部分数据已经预执行，并且在执行批量保存方法前也会获取当前事务里的其他 **SqlSession**，
并执行其 _flushStatements()_ 方法，以便在批量保存的会话里感知到事务里的其他sql执行操作。  
关于事务问题示例如下，假定下面代码都是在spring事务里操作:
1. 批量保存感知到之前执行的结果
```java
// 直接以主键为1保存数据库，此时是在默认的SqlSession，即先创建的SqlSession里执行
testMapper.insert(1);
List<Integer> list = new ArrayList<>();
list.add(1);
list.add(2);
// 假设这是一个批量保存的方法，会创建一个新的SqlSession
// 如果设置flushStatements为false，它将不会抛出异常，但是在事务提交时抛出异常
testMapper.batchInsert(list);
```
2. 单个保存感知到批量保存的结果
```java
List<Integer> list = new ArrayList<>();
list.add(1);
list.add(2);
// 假设这是一个批量保存的方法，并且flushStatements为true
testMapper.batchInsert(list);
// 直接以主键为1保存数据库，此时是在默认的SqlSession里
// 由于本插件默认执行了flushStatements，所以这里将会抛出主键冲突异常
testMapper.insert(1);
```
关于事务的功能测试见sample -> oracle-sample项目、sample -> mysql-sample项目和sample -> postgre-sample项目，
数据库创建表test，修改连接配置后启动，请求web包下的url可以观察事务的工作情况，对比可以发现不同数据库对批量保存的不同表现。
> 区别：在实际测试中，批处理执行时如果有如主键冲突的报错，oracle在会话里共享到的数据更新数时执行到具体的那一条上一条的数量，
> mysql在会话里共享到上一批批处理成功的数量，postgres直接把当前事务设置成aborted，后续无法再继续访问数据库，但如果捕获异常不回滚，
> 并且正常提交事务，可以发现数据库的数据是执行到报错的那一条的上一条。


### 性能测试

实测batch方式性能方面以明显的优势胜出，并且从编码难度来看，batch方式显然更友好。
> 性能测试数据，详情见sample -> simple-sample里的代码  
> 一次性保存1000_000条数据，1000条一批（batch方式由插件自行轮询，foreach手动分页），测试3次，batch与foreach方式耗时如下：  
> mysql数据库（注意mysql数据库连接字符串一定要加上参数:  **rewriteBatchedStatements=true**，否则批量保存无效）：  
> 次数|batch耗时（毫秒）|foreach耗时（毫秒）
> ----|----|----
> 1|14399|18810
> 2|13797|18365
> 3|13649|18356
> 4|13710|18836
> 5|13152|19292
> 平均|13741|18732
>
> oracle数据库：  
> 次数|batch耗时（毫秒）|foreach耗时（毫秒）
> ----|----|----
> 1|8236|9002
> 2|8457|8556
> 3|6840|10093
> 4|6795|10516
> 5|8213|8307
> 平均|7708|9295


### 其他
Mybatis-Plus已经实现了本插件提供的功能，考虑到项目组开发习惯，并未引入Mybatis-Plus，故而开发此插件。  
版本：本插件开发时基于Mybatis: 3.5.9，SpringBoot： 2.7.3， Spring：5.3.22，实测可兼容版本为：Mybatis: 3.4.5，SpringBoot： 2.1.3， Spring：5.1.5，可能还能兼容更低的版本，欢迎测试。
