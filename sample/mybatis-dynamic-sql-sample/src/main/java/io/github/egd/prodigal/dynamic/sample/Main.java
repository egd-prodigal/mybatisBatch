package io.github.egd.prodigal.dynamic.sample;

import org.mybatis.dynamic.sql.SqlBuilder;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;
import org.mybatis.dynamic.sql.insert.render.InsertStatementProvider;
import org.mybatis.dynamic.sql.render.RenderingStrategies;

import java.io.IOException;
import java.sql.JDBCType;

public class Main {

    public static void main(String[] args) throws IOException {
        TestPO testPO = new TestPO();
        testPO.setId(1);
        testPO.setName("yeemin");
        SqlTable table = SqlTable.of("test");
        SqlColumn<Integer> id = new SqlColumn.Builder<Integer>()
                .withName("id")
                .withTable(table)
                .withJdbcType(JDBCType.INTEGER).build();
        SqlColumn<String> name = new SqlColumn.Builder<String>()
                .withName("name")
                .withTable(table)
                .withJdbcType(JDBCType.VARCHAR).build();
        InsertStatementProvider<TestPO> insertStatementProvider = SqlBuilder.insert(testPO)
                .into(table)
                .map(id)
                .toProperty("id")
                .map(name)
                .toProperty("name")
                .build()
                .render(RenderingStrategies.MYBATIS3);
        String insertStatement = insertStatementProvider.getInsertStatement();
        System.out.println(insertStatement);
    }

}