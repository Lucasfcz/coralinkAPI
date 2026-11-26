package io.github.lucasfcz.coralink.infra.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuração centralizada do cache em memória com Caffeine.
 * Define políticas individuais de TTL e limite máximo de entradas por cache para proteger a heap da JVM.
 */
@Configuration
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "caffeine", matchIfMissing = true)
public class CacheConfig {

    public static final String OPPORTUNITIES_CACHE = "opportunities";
    public static final String OPPORTUNITIES_COUNT_CACHE = "opportunities_count";
    public static final String OPPORTUNITY_DETAIL_CACHE = "opportunity_detail";

    public static final Duration OPPORTUNITIES_TTL = Duration.ofMinutes(15);
    public static final Duration OPPORTUNITIES_COUNT_TTL = Duration.ofMinutes(30);
    public static final Duration OPPORTUNITY_DETAIL_TTL = Duration.ofHours(2);

    public static final long OPPORTUNITIES_MAX_SIZE = 500;
    public static final long OPPORTUNITIES_COUNT_MAX_SIZE = 10;
    public static final long OPPORTUNITY_DETAIL_MAX_SIZE = 500;
    public static final long DEFAULT_MAX_SIZE = 200;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setAllowNullValues(false);
        cacheManager.setCaffeine(buildSpec(OPPORTUNITIES_TTL, DEFAULT_MAX_SIZE));

        cacheManager.registerCustomCache(
                OPPORTUNITIES_CACHE,
                buildSpec(OPPORTUNITIES_TTL, OPPORTUNITIES_MAX_SIZE).build()
        );
        cacheManager.registerCustomCache(
                OPPORTUNITIES_COUNT_CACHE,
                buildSpec(OPPORTUNITIES_COUNT_TTL, OPPORTUNITIES_COUNT_MAX_SIZE).build()
        );
        cacheManager.registerCustomCache(
                OPPORTUNITY_DETAIL_CACHE,
                buildSpec(OPPORTUNITY_DETAIL_TTL, OPPORTUNITY_DETAIL_MAX_SIZE).build()
        );

        return cacheManager;
    }

    private Caffeine<Object, Object> buildSpec(Duration ttl, long maxSize) {
        return Caffeine.newBuilder()
                .expireAfterWrite(ttl)
                .maximumSize(maxSize)
                .recordStats();
    }
}
