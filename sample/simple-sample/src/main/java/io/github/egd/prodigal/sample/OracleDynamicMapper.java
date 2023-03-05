package io.github.egd.prodigal.sample;

import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.scripting.defaults.RawSqlSource;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import java.util.HashMap;
import java.util.Map;

public class OracleDynamicMapper {

    private static final String INSERT_MAPPED_STATEMENT = "TAHM.INSERT_";

    public static void main(String[] args) {
        SqlSessionFactory sqlSessionFactory = SimpleSampleUtils.getSqlSessionFactory("oracle");
        Configuration cfg = sqlSessionFactory.getConfiguration();
        String statementName = INSERT_MAPPED_STATEMENT + "TEST";
        boolean hasInsert = cfg.hasStatement(statementName);
        if (hasInsert) {
            return;
        }
        String sql = "INSERT INTO TEST (ID, NAME) VALUES (#{id, jdbcType=INTEGER}, #{name, jdbcType=VARCHAR})";
        RawSqlSource rawSqlSource = new RawSqlSource(cfg, sql, Map.class);
        MappedStatement.Builder builder = new MappedStatement.Builder(cfg, statementName, rawSqlSource, SqlCommandType.INSERT);
        MappedStatement mappedStatement = builder.build();
        cfg.addMappedStatement(mappedStatement);

        try (SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH)) {
            Map<String, Object> parameter = new HashMap<>();
            parameter.put("id", 1);
            parameter.put("name", "yeemin");
            int insert = sqlSession.insert(statementName, parameter);
            System.out.println(insert);
            sqlSession.flushStatements();
        }
    }

}
