package io.github.egd.prodigal.mybatis.batch.context;

import io.github.egd.prodigal.mybatis.batch.core.BatchSqlSessionBuilder;
import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.MyBatisSystemException;
import org.mybatis.spring.SqlSessionUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.transaction.TransactionException;

import java.sql.SQLException;

public class SpringBatchSqlSessionBuilder implements BatchSqlSessionBuilder {

    private final SqlSessionFactory sqlSessionFactory;

//    private final MyBatisExceptionTranslator exceptionTranslator = new MyBatisExceptionTranslator(SQLErrorCodeSQLExceptionTranslator::new, false);

    private final PersistenceExceptionTranslator exceptionTranslator = new PersistenceExceptionTranslator() {

        final SQLErrorCodeSQLExceptionTranslator translator = new SQLErrorCodeSQLExceptionTranslator();

        @Override
        public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
            if (ex instanceof PersistenceException) {
                if (ex.getCause() instanceof PersistenceException) {
                    ex = (PersistenceException) ex.getCause();
                }
                if (ex.getCause() instanceof SQLException) {
                    String task = ex.getMessage() + "\n";
                    SQLException se = (SQLException) ex.getCause();
                    DataAccessException dae = this.translator.translate(task, null, se);
                    return dae != null ? dae : new UncategorizedSQLException(task, null, se);
                } else if (ex.getCause() instanceof TransactionException) {
                    throw (TransactionException) ex.getCause();
                }
                return new MyBatisSystemException(ex);
            }
            return null;
        }
    };

    public SpringBatchSqlSessionBuilder(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    @Override
    public SqlSession build() {
        return SqlSessionUtils.getSqlSession(sqlSessionFactory, ExecutorType.BATCH, exceptionTranslator);
    }
}
