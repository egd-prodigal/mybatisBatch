package io.github.egd.prodigal.sample;

import org.apache.ibatis.session.SqlSessionFactory;

public class PostgreForeach {

    public static void main(String[] args) {
        SqlSessionFactory sqlSessionFactory = SimpleSampleUtils.getSqlSessionFactory("postgre");
        SimpleSampleUtils.stressForeach(sqlSessionFactory);
    }

}
