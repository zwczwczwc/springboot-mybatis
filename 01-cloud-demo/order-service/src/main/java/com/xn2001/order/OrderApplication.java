package com.xn2001.order;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@MapperScan("com.xn2001.order.mapper")
@SpringBootApplication
public class OrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }

    /*
    * 创建Resttemplate并注入Spring
    * resttemplate是专门用来处理HTTP请求的
    * */
    @Bean
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }
}