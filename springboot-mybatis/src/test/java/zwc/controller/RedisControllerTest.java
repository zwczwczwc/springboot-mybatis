package zwc.controller;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author zwc
 * 2022-07-13
 * 16:22
 */

@SpringBootTest
class RedisControllerTest {
    @Test
    public void redisTest01() {
        //连接本地的 Redis 服务
        RedisProperties.Jedis jedis = new RedisProperties.Jedis();
        // 如果 Redis 服务设置了密码，需要用下面这行代码输入密码
        // jedis.auth("123456");
        System.out.println("连接成功");
        //查看服务是否运行
        System.out.println("服务正在运行: "+jedis.toString());
    }
}