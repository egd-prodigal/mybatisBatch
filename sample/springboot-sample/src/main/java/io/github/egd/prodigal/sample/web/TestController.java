package io.github.egd.prodigal.sample.web;

import io.github.egd.prodigal.sample.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    private TestService testService;

    @RequestMapping("/")
    public String test() {
        testService.test();
        return testService.count() + "";
    }

}
