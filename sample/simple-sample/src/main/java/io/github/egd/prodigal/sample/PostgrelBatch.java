package io.github.egd.prodigal.sample;

import org.apache.ibatis.session.SqlSessionFactory;

public class PostgrelBatch {

    public static void main(String[] args) {
        SqlSessionFactory sqlSessionFactory = SimpleSampleUtils.getSqlSessionFactory("postgre");
        SimpleSampleUtils.stressBatch(sqlSessionFactory);
    }

}
