# mybatisBatch

Batch Insert for Mybatis，提供更简化的基于 **mybatis** 的数据库批量保存插件。

## 功能说明

本项目是一个 **mybatis** 插件，目的是为了简化编写批量保存的代码。  
通常情况下需要基于mybatis的动态sql机制，使用 _\<foreach\>_ 标签拼接sql，这明显带来了不小的开发工作量，并且伴随着代码量的增多还增加了错误发生的概率，开发人员甚至还需要适配学习不同数据库的批量保存的sql语法，使用本插件，可以避免编写复杂的动态拼接批量保存sql代码，极大的降低开发难度并且一定程度上能够提高开发效率。  

## 使用指导

### 编码方式
在Mapper里用做批量保存的方法上添加注解 **@BatchInsert** ，并且这个方法映射的sql语句是单条数据保存。  
注解参数简单说明：
字段|格式|用途说明
----|----|----
collection|String|表示方法入参的集合对象，与方法入参里表示实体类集合参数的@Param注解的值一致
item|String|sql语句里的对象名
batchSize|int|分页提交的数量，默认500
insert|String|指定的单条插入方法名，可以为空
flushStatements|boolean|是否预执行sql，默认为true

示例代码：

```java
@Insert({"insert into test (id, name)", "values", "(#{po.id}, #{po.name})"})
@BatchInsert(collection = "testPOS", item = "po", batchSize = 1000)
void batchInsert(@Param("testPOS") List<TestPO> po);
```
> 如果只有一个集合入参，可以不配置 **@Param**，并且不指定 _collection_，如果sql语句里的参数没有明确指定参数名， 可以不指定 _item_，
> 批量提交的数量也可以不指定值，下面的代码与上面代码的将会达到相同的效果：  
> ```java
> @Insert("insert into test (id, name) values (#{id}, #{name})")  
> @BatchInsert  
> void batchInsert(List<TestPO> po);  
> ```

上面的两段代码功能与下面的一致（sql语法因数据库而异），但是性能更优（见[性能测试](#性能测试)）：

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
参数 _insert_ ，指定的单条插入方法名，这个方法一定要在当前Mapper里存在，并且单个保存方法的参数是批量保存参数的子集，
这样批量保存方法可以不用写@Insert以及sql代码，使用方式如下所示：

```java
@Insert({"insert into ${tableName} (id, name)", "values", "(#{po.id}, #{po.name})"})
void insertOne(@Param("tableName") String tableName, @Param("po") TestPO po);

// item必须要跟insertOne的sql语句里的对象名一致
@BatchInsert(insert = "insertOne", collection = "testPOS", item = "po", batchSize = 1000)
void batchInsert(@Param("tableName") String tableName, @Param("unused") String unused, @Param("testPOS") List<TestPO> po);
```

**@BatchInsert** 注解使用时，如果指定了 _insert_ 参数的同时，方法也拥有 **@Insert** 注解，取 _insert_ 参数配置的方法。  
启动时不会检查正确性，如果编写有误，将会在执行时抛出相应异常。

> 注意：  
> 1. 由于本项目的批量是基于Mybatis的BATCH模式，并手动批量提交已执行的部分sql， **不大建议在强事务性业务中使用本插件** ，如果使用中遇到了问题，欢迎联系开发者共同学习。  
> 2. 建议在各种 _异步批量保存_ 的场景下使用，以及事务里批量保存后不再访问数据库的场景。  
> 3. 本插件已尽最大可能解决事务的问题，确保spring环境下正常使用，关于事务问题请看：[事务问题](#事务问题)。


### **springboot** 项目

增加如下依赖：

```xml
<dependency>
    <groupId>io.github.egd-prodigal</groupId>
    <artifactId>mybatis-batch-starter</artifactId>
    <version>2.0.3</version>
</dependency>
```
示例见项目：sample -> springboot-sample

> 注：如果是自己装配SqlSessionFactoryBean的，不需要额外编写添加插件的代码，会自动添加，纯spring环境下也一样。

### 纯 **spring** 项目

增加如下依赖

```xml
<dependency>
    <groupId>io.github.egd-prodigal</groupId>
    <artifactId>mybatis-batch-spring</artifactId>
    <version>2.0.3</version>
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
    <version>2.0.3</version>
</dependency>
```
在配置文件 **mybatis-config.xml** （也可以是其他文件名）里增加插件配置：

```xml
<plugins>
    <plugin interceptor="io.github.egd.prodigal.mybatis.batch.plugins.BatchInsertInterceptor"/>
</plugins>
```

编写mybatis初始化代码，基于xml配置生成SqlSessionFactory，如果是不基于xml配置生成的，请自行添加插件，然后在执行数据访问代码前编写如下代码：

```java
// 此处的sqlSessionFactory在之前的代码里生成
BatchInsertContext.setSqlSessionFactory(sqlSessionFactory);
// 添加拥有批量保存方法的Mapper接口类，可以传多个类，可以在任意位置调用
BatchInsertScanner.addClass(ITestMapper.class);
// 扫描批量保存方法，可以在任意位置任何使用调用，
// 每次调用都只会扫描上次调用scan之后调用addClass添加的新的Mapper接口类
BatchInsertScanner.scan();
```
示例见项目：sample -> simple-sample （这个项目也承载了性能测试的功能）

### spring-batch 
**这一段与本插件基本没有关系** 。spring批处理组件，使用这个组件的用户，大概率不需要使用本插件，这里只是提供另外一种mybatis批量保存的方式。  
编写代码手动装配要进行批量保存的 **MyBatisBatchItemWriter** ，示例代码：
```java
@Bean
public MyBatisBatchItemWriter<TestPO> itemWriter() {
   MyBatisBatchItemWriterBuilder<TestPO> itemWriterBuilder = new MyBatisBatchItemWriterBuilder<>();
   itemWriterBuilder.sqlSessionTemplate(new SqlSessionTemplate(sqlSessionFactory, ExecutorType.BATCH));
   // 这里的statementId，填完整的路径：package.class.method，指定单个保存方法
   itemWriterBuilder.statementId("io.github.egd.prodigal.sample.repository.mapper.ITestMapper.insert");
   // 执行时校验结果的，事实上结果由各个数据库驱动提供，设置成true的话部分数据库执行报错，详情自测
   itemWriterBuilder.assertUpdates(false);
   return itemWriterBuilder.build();
}
```
如果单个保存的方法，方法入参没有指定 **@Param** 注解，上面的代码就可以了，但是如果指定了 **@Param** 注解，那么还需要额外
设置 _itemWriterBuilder_ 的 _itemToParameterConverter_ ，如下所示：
```java
itemWriterBuilder.itemToParameterConverter(testPO -> {
   Map<String, TestPO> paramMap = new HashMap();
   // 此处的key与单条保存的方法注解Param里配置的一致
   paramMap.put("po", testPO);
   return paramMap;
});
```
这样就可以在业务中使用 **MyBatisBatchItemWriter** 执行批量保存的逻辑，但是关于它的事务问题，请自行研究，下面关于事务的内容仅供参考。
## 事务问题

上面提到的 **不大建议在强事务性业务中使用本插件** ，注意 **'强事务性业务'**，是为了避免大量数据保存的情况下，事务一次提交过多数据导致数据库压力过大，
事务提交缓慢并长期占用数据库连接资源，应用服务等待时间过长导致整体业务服务不稳定的现象。  
实际上，本插件支持事务的特性，由mybatis自身的特性提供，并且在 _mybatis-batch-spring_ 模块里我们把事务交给 **spring** 事务管理框架，由 **spring** 统一管理事务。  
本插件的核心是使用一个批量模式的 **SqlSession** 执行单条保存的 **MappedStatement** ，但是在实际业务过程中，其他常规数据库访问使用默认的 **SqlSession** 实现。
**SqlSession**顾名思义就是sql会话，正常思维下，多个会话不能共享数据，事实上也是如此，多个**SqlSession** 之间不能直接互相感知对方的操作，
但是mybatis对**SqlSession**提供了 _flushStatements()_ 方法，这是个神奇的方法，在无事务的情况下执行该方法，数据将会直接写入数据库，
在有事务管理的情况下执行该方法，它将会把自己会话里的数据库操作 _“共享”_ 给当前事务，而每个会话都能从当前事务里感知到数据库操作，即“_会话分享事务_”，
这个方法最终是调用 **java.sql.Statement** 的 _executeBatch()_ 方法，由各个数据库驱动实现方法逻辑，因此本插件对事务控制的实际表现也因数据库而异，
但不管使用什么数据库，常规的数据库读写操作跟批量模式下的读写操作都被一个事务管理着，要么一起成功要么一起失败。    
因此通过 _flushStatements()_ 方法可以实现多个会话间互相感知对方对数据库的操作，并且这些会话也被相同的事务管理器控制。 
 
综上，本插件的注解 **@BatchInsert** 提供了 _flushStatements_ 参数，默认为true，表示是否执行sql，当然但哪怕设置成false了，
当一次保存的数据大于配置的 _batchSize_ 时，还是会有一部分数据已经被执行，同时在执行批量保存方法前会获取当前事务里的默认 **SqlSession**，
并执行它的 _flushStatements()_ 方法，以批量保存的会话能够正常同步到事务里已经执行的更新操作，并且使批量保存的执行结果能够即时共享给事务，
并且让外部默认的会话也能同感知到批量执行的结果，一个事务里的多个批量保存方法使用相同的会话。    
关于事务问题示例如下，假定下面代码都是在spring事务里执行:
1. 批量保存感知到之前执行的结果
```java
// 直接以主键为1保存数据库，此时是在默认的SqlSession
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
3. 默认事务无法感知到批量保存的结果
```java
// 假设数据清空，直接以主键为1保存数据库，此时是在默认的SqlSession
testMapper.insert(1);
// 构造从1开始到5的集合
List<Integer> list = generateList(5);
// 假设这是一个批量保存的方法，并且flushStatements为false，batchSize>5，它不会抛出主键冲突的异常
testMapper.batchInsert(list);
// 查询数据库数据数量，此时是在默认的SqlSession
int count = testMapper.count();
// 由于flushStatements为false，并且执行数量小于batchSize，所以事务无法感知到
assert count = 1;
// 事务提交时将会抛出主键冲突的异常
```
4. 批量保存部分提交
```java
// 假设数据清空
// 构造从1开始到105的集合
List<Integer> list = generateList(105);
// 假设这是一个批量保存的方法，并且flushStatements为false，batchSize为10
testMapper.batchInsert(list);
// 查询数据库数据数量，此时是在默认的SqlSession
int count = testMapper.count();
// 由于flushStatements为false，但数量大于batchSize，每执行10条都会flushStatements()一次，
// 造成会话部分共享的情况，执行到第100条时flushStatements()，但后续数量不足10条，没有flushStatements()
assert count = 100;
// 事务提交后，数据库有105条数据
```
关于事务的功能测试见sample -> oracle-sample项目、sample -> mysql-sample项目、sample -> mssql-sample项目、sample -> postgre-sample项目，
数据库创建表test，修改连接配置后启动，请求web包下的url可以观察事务的工作情况，对比可以发现不同数据库对批量保存事务管理的不同表现:
> oracle在会话里共享到的数据更新数时执行到具体的那一条上一条的数量；  
> mssql在会话里共享到的数据更新数时执行到具体的那一条上一条的数量；  
> mysql在会话里共享到上一批批处理成功的数量；  
> postgres直接把当前事务设置成aborted，后续无法再继续访问数据库，但如果捕获异常不回滚， 并且正常提交事务，可以发现数据库的数据是执行到报错的那一条的上一条。


## 性能测试

我们基于mysql、oracle、postgre、mssql测试batch方法和foreach方法的性能，实测batch方式性能方面以微弱的优势胜出。  
测试方法：一次性保存1000_000条数据，1000条一批，batch方式配置 _batchSize_ 参数，foreach手动分页，两种方式均以无事务的方式运行，连续测试5次并取平均值，  
> 注意：mysql数据库连接字符串一定要加上参数:  **rewriteBatchedStatements=true**，否则批量保存无效.    

不同数据库分别使用batch与foreach批量保存测试的耗时数据如下，单位：毫秒：  
<table>
<thead>
    <tr>
        <th rowspan='2'></th>
        <th colspan='2'>mysql</th>
        <th colspan='2'>oracle</th>
        <th colspan='2'>postgre</th>
        <th colspan='2'>mssql</th>
    </tr>
    <tr>
        <th>batch</th>
        <th>foreach</th>
        <th>batch</th>
        <th>foreach</th>
        <th>batch</th>
        <th>foreach</th>
        <th>batch</th>
        <th>foreach</th>
    </tr>
</thead>
<tbody>
    <tr>
        <td>1</td>
        <td>14399</td>
        <td>18810</td>
        <td>8236</td>
        <td>9002</td>
        <td>15971</td>
        <td>18763</td>
        <td>20293</td>
        <td>26365</td>
    <tr>
    <tr>
        <td>2</td>
        <td>13797</td>
        <td>18365</td>
        <td>8457</td>
        <td>8556</td>
        <td>18874</td>
        <td>17755</td>
        <td>21673</td>
        <td>25676</td>
    <tr>
    <tr>
        <td>3</td>
        <td>13649</td>
        <td>18356</td>
        <td>6840</td>
        <td>10093</td>
        <td>16185</td>
        <td>17713</td>
        <td>20834</td>
        <td>26257</td>
    <tr>
    <tr>
        <td>4</td>
        <td>13710</td>
        <td>18836</td>
        <td>6795</td>
        <td>10516</td>
        <td>16777</td>
        <td>17253</td>
        <td>21094</td>
        <td>25441</td>
    <tr>
    <tr>
        <td>5</td>
        <td>13152</td>
        <td>19292</td>
        <td>8213</td>
        <td>8307</td>
        <td>16450</td>
        <td>16434</td>
        <td>20511</td>
        <td>25775</td>
    <tr>
    <tr>
        <td>平均</td>
        <td><strong>13741</strong></td>
        <td>18732</td>
        <td><strong>7708</strong></td>
        <td>9295</td>
        <td><strong>16851</strong></td>
        <td>17584</td>
        <td><strong>20881</strong></td>
        <td>25903</td>
    <tr>
</tbody>
</table>

> 性能测试详情见sample -> simple-sample里的代码 

## 更新日志

- v2.0.3 发布第一个release版本

### 后续计划

- 批量保存将支持java8 Stream入参

## 参与者

- 生一鸣 <yeeminshon@outlook.com>
- 宋祥富 <784413317@qq.com>
- 曾广霏 <guangfeizeng163@163.com>

## 其他
Mybatis-Plus已经实现了本插件提供的功能，考虑到项目组开发习惯，并未引入Mybatis-Plus，故而开发此插件。  
版本：本插件开发时基于Mybatis: 3.5.9，SpringBoot： 2.7.3， Spring：5.3.22，实测可兼容版本为：Mybatis: 3.4.5，SpringBoot： 2.1.3， Spring：5.1.5，可能还能兼容更低的版本，欢迎测试。
