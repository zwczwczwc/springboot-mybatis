package com.xn2001.user.web;

import com.xn2001.user.config.PatternProperties;
import com.xn2001.user.pojo.User;
import com.xn2001.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@RestController
@RequestMapping("/user")
//@RefreshScope
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private PatternProperties patternProperties;
//    @Value("${pattern.dataformat}")
//    private String dataformat;

    /**
     * 路径： /user/110
     *
     * @param id 用户id
     * @return 用户
     */
    @GetMapping("/{id}")
    public User queryById(@PathVariable("id") Long id) {
        return userService.queryById(id);
    }

    @GetMapping("/now")
    public String dataformat(){
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(patternProperties.getDataformat()));
    }
}
