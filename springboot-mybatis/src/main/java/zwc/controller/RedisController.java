package zwc.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author zwc
 * 2022-07-13
 * 15:52
 */

@RestController
public class RedisController {

    @Autowired
    private RedisTemplate redisTemplate;

    @GetMapping("/redis/get/{key}")
    public Object greet(@PathVariable("key") String key){
        List ans =  redisTemplate.opsForList().range(key,1, -1);
        return ans;
    }

    @PostMapping("/redis/set/{key}/{value}")
    public Object post(@PathVariable("key") String key,
                       @PathVariable("value") String value){
//        redisTemplate.opsForValue().set(key,value);
        redisTemplate.opsForList().rightPush(key,value);
        return "set sucess";
    }

    @PostMapping("/redis/delete/{key}")
    public String delete(@PathVariable("key") String key){
        if(redisTemplate.hasKey(key)) {
            redisTemplate.delete(key);
            return "delete sucess";
        }
        return key;
    }
}
