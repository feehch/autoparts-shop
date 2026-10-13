package feehch.dev.autoparts.infrastructure.cache;

import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.LoggingCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

/**
 * Кэширование справочников и карточек товаров в Redis.
 * Ошибки Redis не ломают приложение: при сбое кэша данные читаются напрямую из PostgreSQL.
 */
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    private static final Duration REFERENCE_TTL = Duration.ofHours(1);
    private static final Duration PRODUCT_TTL = Duration.ofMinutes(10);

    @Bean
    public RedisCacheConfiguration redisCacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .prefixCacheNameWith("autoparts:cache:")
                .entryTtl(REFERENCE_TTL)
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new JdkSerializationRedisSerializer(getClass().getClassLoader())));
    }

    @Bean
    public RedisCacheManagerBuilderCustomizer cacheTtlCustomizer(RedisCacheConfiguration base) {
        return builder -> builder
                .transactionAware()
                .withCacheConfiguration(CacheNames.CATEGORIES, base)
                .withCacheConfiguration(CacheNames.BRANDS, base)
                .withCacheConfiguration(CacheNames.CAR_MAKES, base)
                .withCacheConfiguration(CacheNames.CAR_MODELS, base)
                .withCacheConfiguration(CacheNames.PRODUCTS, base.entryTtl(PRODUCT_TTL));
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new LoggingCacheErrorHandler(true);
    }
}
