package io.github.egd.prodigal.oraclesample.service;

import java.util.Date;

public interface TestService {

    Date getSysdate();

    int count();

    int batch();

    int batch2();

    String unique1();

    String unique2();

    String unique3();

    void test();

}
