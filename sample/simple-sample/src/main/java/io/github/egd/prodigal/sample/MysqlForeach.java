package io.github.egd.prodigal.sample;

import org.apache.ibatis.session.SqlSessionFactory;

public class MysqlForeach {

    public static void main(String[] args) {
        SqlSessionFactory sqlSessionFactory = SimpleSampleUtils.getSqlSessionFactory("mysql");
        SimpleSampleUtils.stressForeach(sqlSessionFactory);
    }

}
