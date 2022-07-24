package com.xn2001.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author zwc
 * 2022-07-22
 * 11:28
 */

@Data
@Component
//约定大于配置，只要前缀名和变量名拼接和配置文件一致就能完成属性的注入
@ConfigurationProperties(prefix = "pattern")
public class PatternProperties {

    private String dataformat;

}
