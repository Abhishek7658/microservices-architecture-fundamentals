package product_service.config;

import java.time.Duration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
public class RedisCacheConfig {

    @Bean
    public RedisCacheConfiguration redisCacheConfiguration() {

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new GenericJackson2JsonRedisSerializer())
                );
    }

    @Bean
    public CacheManager cacheManager(
            RedisConnectionFactory redisConnectionFactory) {

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(redisCacheConfiguration())
                .build();
    }

    @Bean
    public CacheErrorHandler cacheErrorHandler() {
        return new RedisCacheErrorHandler();
    }

    private static class RedisCacheErrorHandler
            implements CacheErrorHandler {

        @Override
        public void handleCacheGetError(
                RuntimeException exception,
                org.springframework.cache.Cache cache,
                Object key) {

            System.out.println(
                    "Redis cache GET failed: " + exception.getMessage()
            );
        }

        @Override
        public void handleCachePutError(
                RuntimeException exception,
                org.springframework.cache.Cache cache,
                Object key,
                Object value) {

            System.out.println(
                    "Redis cache PUT failed: " + exception.getMessage()
            );
        }

        @Override
        public void handleCacheEvictError(
                RuntimeException exception,
                org.springframework.cache.Cache cache,
                Object key) {

            System.out.println(
                    "Redis cache EVICT failed: " + exception.getMessage()
            );
        }

        @Override
        public void handleCacheClearError(
                RuntimeException exception,
                org.springframework.cache.Cache cache) {

            System.out.println(
                    "Redis cache CLEAR failed: " + exception.getMessage()
            );
        }
    }
}