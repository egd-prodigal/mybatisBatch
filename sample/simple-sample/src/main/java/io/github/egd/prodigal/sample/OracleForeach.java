package io.github.egd.prodigal.sample;

import org.apache.ibatis.session.SqlSessionFactory;

public class OracleForeach {

    public static void main(String[] args) {
        SqlSessionFactory sqlSessionFactory = SimpleSampleUtils.getSqlSessionFactory("oracle");
        SimpleSampleUtils.stressForeach(sqlSessionFactory);
    }

}
