package io.github.egd.prodigal.sample;

import org.apache.ibatis.session.SqlSessionFactory;

public class MssqlForeach {

    public static void main(String[] args) {
        SqlSessionFactory sqlSessionFactory = SimpleSampleUtils.getSqlSessionFactory("mssql");
        SimpleSampleUtils.stressForeach(sqlSessionFactory);
    }

}
