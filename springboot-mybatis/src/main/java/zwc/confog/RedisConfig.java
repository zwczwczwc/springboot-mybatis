package zwc.confog;

import com.alibaba.fastjson.support.spring.GenericFastJsonRedisSerializer;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;
import static java.util.Collections.singletonMap;

/**
 * @author zwc
 * 2022-07-13
 * 16:28
 */

@Configuration
@EnableCaching//开启缓存
public class RedisConfig extends CachingConfigurerSupport {

    /**
     * 缓存管理器,取代默认
     *
     * @param lettuceConnectionFactory redis连接
     * @return CacheManager
     */
    @Bean
    public CacheManager cacheManager(LettuceConnectionFactory lettuceConnectionFactory)
    {
        GenericFastJsonRedisSerializer jsonRedisSerializer = new GenericFastJsonRedisSerializer();

        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig();
        // 一天
        defaultCacheConfig = defaultCacheConfig.serializeValuesWith(
                RedisSerializationContext
                        .SerializationPair
                        .fromSerializer(jsonRedisSerializer)
        ).entryTtl(Duration.ofDays(1L)).disableCachingNullValues();

        // 不过期
        Map<String, RedisCacheConfiguration> configMap = singletonMap("persistent", RedisCacheConfiguration
                .defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext
                        .SerializationPair
                        .fromSerializer(jsonRedisSerializer))
                .disableCachingNullValues());

        return RedisCacheManager.builder(lettuceConnectionFactory)
                .cacheDefaults(defaultCacheConfig)
                .withInitialCacheConfigurations(configMap)
                .transactionAware()
                .build();
    }

    @Bean("myRedisTemplate")
    public RedisTemplate<String, Object> redisTemplate(LettuceConnectionFactory lettuceConnectionFactory)
    {

        RedisTemplate<String, Object> template = new RedisTemplate<>();

        template.setConnectionFactory(lettuceConnectionFactory);
        GenericFastJsonRedisSerializer fastJsonRedisSerializer = new GenericFastJsonRedisSerializer();

        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        // key采用String的序列化方式
        template.setKeySerializer(stringRedisSerializer);
        // hash的key也采用String的序列化方式
        template.setHashKeySerializer(stringRedisSerializer);
        // value序列化方式采用jackson
        template.setValueSerializer(fastJsonRedisSerializer);
        // hash的value序列化方式采用jackson
        template.setHashValueSerializer(fastJsonRedisSerializer);

        template.afterPropertiesSet();

        return template;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(LettuceConnectionFactory lettuceConnectionFactory)
    {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(lettuceConnectionFactory);
        return template;
    }
}
