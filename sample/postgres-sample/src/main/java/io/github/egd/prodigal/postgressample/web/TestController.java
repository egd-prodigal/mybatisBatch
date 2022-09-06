package io.github.egd.prodigal.postgressample.web;

import io.github.egd.prodigal.postgressample.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @Autowired
    private TestService testService;

    @RequestMapping("/monitor")
    public String monitor() {
        return "status ok at: " + testService.getSysdate();
    }

    @RequestMapping("/test")
    public String test() {
        int batch = testService.batch();
        return batch + " -> " + testService.count();
    }

    @RequestMapping("/test2")
    public String test2() {
        int batch = testService.batch2();
        return batch + " -> " + testService.count();
    }

    @RequestMapping("/unique1")
    public String unique1() {
        try {
            return testService.unique1() + "，数据库现存数据数：" + testService.count();
        } catch (Exception e) {
            return "在事务提交时才发现异常" + "，数据库现存数据数：" + testService.count();
        }
    }

    @RequestMapping("/unique2")
    public String unique2() {
        try {
            return testService.unique2() + "，数据库现存数据数：" + testService.count();
        } catch (Exception e) {
            return "在事务提交时才发现异常" + "，数据库现存数据数：" + testService.count();
        }
    }


    @RequestMapping("/unique3")
    public String unique3() {
        try {
            return testService.unique3() + "，数据库现存数据数：" + testService.count();
        } catch (Exception e) {
            return "在事务提交时才发现异常" + "，数据库现存数据数：" + testService.count();
        }
    }

}
