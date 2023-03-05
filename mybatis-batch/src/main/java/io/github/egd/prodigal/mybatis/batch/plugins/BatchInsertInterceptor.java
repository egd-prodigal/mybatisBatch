package io.github.egd.prodigal.mybatis.batch.plugins;

import io.github.egd.prodigal.mybatis.batch.annotations.BatchInsert;
import io.github.egd.prodigal.mybatis.batch.core.BatchContext;
import org.apache.ibatis.binding.MapperMethod.ParamMap;
import org.apache.ibatis.executor.BatchResult;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.defaults.DefaultSqlSession;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 批量保存拦截器，拦截 {@link Executor#update(MappedStatement, Object)}方法，
 * 发现是批量保存方法，则开启Batch模式并执行单条插入方法，当执行的数量达到预期的值时，
 * 执行提交后继续保存剩下的
 */
@Intercepts(@Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}))
public class BatchInsertInterceptor implements Interceptor {

    /**
     * 非spring模式下缓存Mapper的类
     */
    private final Map<String, Class<?>> mapperClassMap = new HashMap<>();

    /**
     * 非spring模式下缓存mapper的类和方法<key1:mapper类<key2：methodName>>
     */
    private final Map<Class<?>, Map<String, Method>> mapperClassMethodMap = new HashMap<>();

    /**
     * <p>拦截方法，一切的入口，由此发生</p>
     *
     * @param invocation 拦截的调用器
     * @return 访问数据库的返回结果
     * @throws Throwable 运行时异常
     */
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 先获取参数
        Object[] argsObjects = invocation.getArgs();
        // 根据配置，拦截的是 org.apache.ibatis.executor.Executor#update方法，依次获取参数
        // 第一额参数是MappedStatement
        MappedStatement mappedStatement = (MappedStatement) argsObjects[0];
        // 根据sql指令类型判断，不是插入的直接执行sql
        if (mappedStatement.getSqlCommandType() != SqlCommandType.INSERT) {
            return invocation.proceed();
        }
        // 获取mappedStatementId，如果是约定的.egd-singleInsert结尾的，表明已经是单条保存方法了，直接执行sql
        String id = mappedStatement.getId();
        if (id.endsWith(BatchContext.EGD_SINGLE_INSERT)) {
            return invocation.proceed();
        }
        // 其他保存方法，先声明一个BatchInsert，后面赋值，并根据这个batchInsert执行sql
        BatchInsert batchInsert;
        if (BatchContext.isInSpring()) {
            // spring环境下，直接调用上下文的方法判断是否是批量保存方法
            if (BatchContext.isBatchInsertMappedStatement(id)) {
                // 是批量保存方法，获取它的BatchInsert注解
                batchInsert = BatchContext.getBatchInsertByMappedStatementId(id);
            } else {
                // 不是批量保存方法，直接执行sql
                return invocation.proceed();
            }
        } else {
            // 非spring环境，需要反射获取类、方法、方法注解来判断
            Class<?> mapperClass = findMapperClass(id);
            // 如果存在一些框架自动注册mappedStatement，没有具体实现类，那么可能会没有类
            if (mapperClass == null) {
                return invocation.proceed();
            }
            Method mapperMethod = findMapperMethod(id, mapperClass);
            // 获取的方法为空，基本不可能出现，防止其他插件用了类似的机制，所以此处直接执行sql，让其他插件来解决
            if (mapperMethod == null) {
                return invocation.proceed();
            }
            // 获取方法的BatchInsert注解
            batchInsert = findBatchInsert(id, mapperMethod);
            if (batchInsert == null) {
                // 这个方法不具有BatchInsert注解，不是批量保存的方法，直接执行sql
                return invocation.proceed();
            }
        }
        // 这是方法入参，由开发者编写Mapper接口里的方法生成，正常情况下的调用都会是一个ParamMap类型的参数
        Object object = argsObjects[1];
        // 非HashMap的，直接往后执行
        if (!(object instanceof HashMap)) {
            return invocation.proceed();
        }
        // 3.5.5之前的版本大都是DefaultSqlSession#StrictMap，后面的版本大都是MapperMethod#ParamMap，兼容两者
        // ParamMap从3.2.0版本开始引入,instanceof 判断效率更高
        if (object instanceof ParamMap || object instanceof DefaultSqlSession.StrictMap) {
            // 获取集合参数，
            Collection<?> itemList = getItemList((HashMap<?, ?>) object, batchInsert);
            // 执行批量保存
            return invokeBatchInsert(mappedStatement, batchInsert, itemList, (HashMap<?, ?>) object);
        } else {
            return invocation.proceed();
        }
    }

    /**
     * 执行批量保存，核心方法，在这里开启批量模式，并执行保存方法
     *
     * @param mappedStatement MappedStatement
     * @param batchInsert     batchInsert
     * @param itemList        集合入参
     * @param paramMap        其他参数
     * @return Object，一般都是void
     */
    private Object invokeBatchInsert(MappedStatement mappedStatement, BatchInsert batchInsert, Collection<?> itemList, HashMap<?, ?> paramMap) {
        int updateCounts = 0;
        // 先获取SqlSession，必须是Batch模式的
        SqlSession sqlSession = openSession(batchInsert.flushStatements());
        try {
            // 批量提交数量
            int batchSize = batchInsert.batchSize();
            // 索引器
            int index = 1;
            // 可能的单条保存方法入参
            ParamMap<Object> objectParamMap = new ParamMap<>();
            // 判断是否指定了单个对象的名字
            String item = batchInsert.item();
            boolean hasItemName = !"".equals(item);
            if (hasItemName) {
                // 指定了单个对象名字，则入参一定是objectParamMap
                paramMap.forEach((k, v) -> objectParamMap.put(((String) k), v));
            }
            // 设置单条保存MappedStatementId，允许开发者指定一个当前接口类里的其他方法
            String statement;
            String insert = batchInsert.insert();
            String mappedStatementId = mappedStatement.getId();
            if (!"".equals(insert)) {
                // 截去方法名，替换成指定的单条保存方法名
                statement = mappedStatementId.substring(0, mappedStatementId.lastIndexOf(".") + 1) + insert;
            } else {
                statement = mappedStatementId + BatchContext.EGD_SINGLE_INSERT;
            }
            try {
                for (Object argument : itemList) {
                    if (hasItemName) {
                        // 指定了单个对象名字，入参使用objectParamMap，并赋值
                        objectParamMap.put(item, argument);
                        argument = objectParamMap;
                    }
                    // 调用单个保存方法
                    sqlSession.insert(statement, argument);
                    if (index % batchSize == 0) {
                        // 执行之前的保存sql
                        List<BatchResult> batchResults = sqlSession.flushStatements();
                        for (BatchResult batchResult : batchResults) {
                            int[] batchResultUpdateCounts = batchResult.getUpdateCounts();
                            for (int result : batchResultUpdateCounts) {
                                updateCounts += result;
                            }
                        }
                    }
                    index++;
                }
                // 最后提交一次
                BatchContext.getBatchSqlSessionBuilder().commit(sqlSession, batchInsert.flushStatements());
            } catch (Throwable throwable) {
                // 异常回滚
                sqlSession.rollback();
                throw throwable;
            }
        } finally {
            BatchContext.getBatchSqlSessionBuilder().close(sqlSession);
        }
        return updateCounts;
    }

    /**
     * 获取集合参数
     *
     * @param paramMap    实际入参
     * @param batchInsert 批量保存注解
     * @return List<?>
     */
    private Collection<?> getItemList(HashMap<?, ?> paramMap, BatchInsert batchInsert) {
        Collection<?> itemList;
        // 先从注解配置的collection获取参数
        String collection = batchInsert.collection();
        if (paramMap.containsKey(collection)) {
            Object o = paramMap.get(collection);
            try {
                // 参数必须是一个集合
                itemList = (Collection<?>) o;
            } catch (ClassCastException e) {
                throw new PluginException(e);
            }
        } else {
            // 没找到注解配置的，随便找一个集合类型的入参
            Stream<? extends Collection<?>> stream = paramMap.values().stream().filter(v -> (v instanceof Collection)).map(v -> ((Collection<?>) v));
            itemList = stream.findAny().orElseThrow(() -> new PluginException("cannot find argument instance of List"));
        }
        return itemList;
    }

    /**
     * 获取Mapper的类
     *
     * @param id MappedStatementId
     * @return Class<?>
     */
    private Class<?> findMapperClass(String id) {
        Class<?> aClass = mapperClassMap.get(id);
        if (aClass == null) {
            String mapperClassName = id.substring(0, id.lastIndexOf("."));
            try {
                aClass = Class.forName(mapperClassName);
            } catch (ClassNotFoundException e) {
                return null;
            }
            mapperClassMap.put(id, aClass);
        }
        return aClass;
    }

    /**
     * 获取Mapper的方法
     *
     * @param id          MappedStatementId
     * @param mapperClass Mapper的类
     * @return Method
     */
    private Method findMapperMethod(String id, Class<?> mapperClass) {
        //如果是null，说明没获取过
        Map<String, Method> methodMap = mapperClassMethodMap.get(mapperClass);
        if (methodMap == null) {
            Configuration configuration = BatchContext.getSqlSessionFactory().getConfiguration();
            Method[] methods = mapperClass.getDeclaredMethods();
            methodMap = Arrays.stream(methods).filter(method -> {
                // 仅判断insert的
                String mappedStatementId = method.getDeclaringClass().getName() + "." + method.getName();
                if (configuration.hasStatement(mappedStatementId)) {
                    MappedStatement mappedStatement = configuration.getMappedStatement(mappedStatementId);
                    return SqlCommandType.INSERT.equals(mappedStatement.getSqlCommandType());
                }
                return false;
            }).collect(Collectors.toMap(Method::getName, method -> method));
            mapperClassMethodMap.put(mapperClass, methodMap);
        }

        //获取过
        String methodName = id.substring(id.lastIndexOf(".") + 1);
        return methodMap.get(methodName);
    }

    /**
     * 获取执行方法的BatchInsert注解
     *
     * @param id           mappedStatementId
     * @param mapperMethod 执行方法
     * @return BatchInsert 批量保存注解
     */
    private BatchInsert findBatchInsert(String id, Method mapperMethod) {
        // 先判断上下文是否已经标识这是个批量保存方法
        if (BatchContext.isBatchInsertMappedStatement(id)) {
            // 直接获取方法注解并返回
            return BatchContext.getBatchInsertByMappedStatementId(id);
        }
        // 手动尝试获取BatchInsert注解
        BatchInsert batchInsert = mapperMethod.getAnnotation(BatchInsert.class);
        if (batchInsert != null) {
            // 本方法拥有BatchInsert注解，注册它成为一个批量保存方法
            BatchContext.addBatchInsertMappedStatement(id, batchInsert);
            if (!BatchContext.isInSpring()) {
                // 不在spring环境里的，还需要额外注册单条保存的MappedStatement
                BatchContext.registerSingleInsertMappedStatement(id, batchInsert);
            }
        }
        return batchInsert;
    }

    /**
     * 开启一个批量模式的SqlSession
     *
     * @return SqlSession
     */
    private SqlSession openSession(boolean flushStatements) {
        return BatchContext.getBatchSqlSessionBuilder().build(flushStatements);
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {

    }

}
