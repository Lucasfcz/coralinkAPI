package io.github.lucasfcz.coralink.infra.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableCaching
@Slf4j
public class RedisCacheConfig implements CachingConfigurer {

    public static final String OPPORTUNITIES_CACHE = "opportunities";
    public static final String OPPORTUNITIES_COUNT_CACHE = "opportunities_count";
    public static final String OPPORTUNITY_DETAIL_CACHE = "opportunity_detail";

    /**
     * Fábrica de conexão com o Redis com resolução inteligente de URL do Upstash ou Docker local.
     * Suporta nativamente:
     * 1. UPSTASH_REDIS_REST_URL + UPSTASH_REDIS_REST_TOKEN (extrai host, porta 6379, SSL=true)
     * 2. REDIS_URL / spring.data.redis.url (rediss://... ou redis://...)
     * 3. Host, Porta, Senha e SSL convencionais
     */
    @Bean
    public LettuceConnectionFactory redisConnectionFactory(
            @Value("${spring.data.redis.url:${REDIS_URL:}}") String redisUrl,
            @Value("${UPSTASH_REDIS_REST_URL:}") String upstashRestUrl,
            @Value("${UPSTASH_REDIS_REST_TOKEN:${REDIS_PASSWORD:}}") String upstashRestToken,
            @Value("${spring.data.redis.host:localhost}") String host,
            @Value("${spring.data.redis.port:6379}") int port,
            @Value("${spring.data.redis.password:}") String password,
            @Value("${spring.data.redis.ssl.enabled:false}") boolean sslEnabled
    ) {
        // 1. Prioridade para variáveis do Upstash
        if (upstashRestUrl != null && !upstashRestUrl.isBlank()) {
            String cleanHost = extractHost(upstashRestUrl);
            log.info("Configurando Redis via UPSTASH_REDIS_REST_URL. Host: {}, Porta: 6379, SSL: true", cleanHost);

            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(cleanHost, 6379);
            if (upstashRestToken != null && !upstashRestToken.isBlank()) {
                config.setPassword(RedisPassword.of(upstashRestToken));
            }
            LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                    .useSsl()
                    .build();
            return new LettuceConnectionFactory(config, clientConfig);
        }

        // 2. Prioridade para string de conexão completa (rediss:// ou redis://)
        if (redisUrl != null && !redisUrl.isBlank()) {
            log.info("Configurando Redis via REDIS_URL");
            LettuceClientConfiguration.LettuceClientConfigurationBuilder clientBuilder = LettuceClientConfiguration.builder();
            if (redisUrl.startsWith("rediss://")) {
                clientBuilder.useSsl();
            }
            RedisStandaloneConfiguration config = parseRedisUrl(redisUrl);
            return new LettuceConnectionFactory(config, clientBuilder.build());
        }

        // 3. Fallback tradicional por host, porta e senha
        log.info("Configurando Redis via Host: {}:{}, SSL: {}", host, port, sslEnabled);
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);
        if (password != null && !password.isBlank()) {
            config.setPassword(RedisPassword.of(password));
        }

        LettuceClientConfiguration.LettuceClientConfigurationBuilder clientBuilder = LettuceClientConfiguration.builder();
        if (sslEnabled) {
            clientBuilder.useSsl();
        }
        return new LettuceConnectionFactory(config, clientBuilder.build());
    }

    private String extractHost(String rawUrl) {
        try {
            URI uri = URI.create(rawUrl.trim());
            if (uri.getHost() != null) {
                return uri.getHost();
            }
        } catch (Exception ignored) {
        }
        return rawUrl.trim().replaceFirst("^https?://", "").split("[:/]", 2)[0];
    }

    private RedisStandaloneConfiguration parseRedisUrl(String redisUrl) {
        try {
            URI uri = URI.create(redisUrl.trim());
            String host = uri.getHost() != null ? uri.getHost() : "localhost";
            int port = uri.getPort() != -1 ? uri.getPort() : 6379;
            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);
            if (uri.getUserInfo() != null) {
                String[] parts = uri.getUserInfo().split(":");
                String pass = parts[parts.length - 1];
                config.setPassword(RedisPassword.of(pass));
            }
            return config;
        } catch (Exception e) {
            log.warn("Falha ao analisar REDIS_URL, usando configuração padrão", e);
            return new RedisStandaloneConfiguration("localhost", 6379);
        }
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisSerializer<Object> jsonSerializer = createJsonSerializer();

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .prefixCacheNameWith("coralink:cache:v2:")
                .entryTtl(Duration.ofMinutes(15))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.string()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer));

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put(OPPORTUNITIES_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put(OPPORTUNITIES_COUNT_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put(OPPORTUNITY_DETAIL_CACHE, defaultConfig.entryTtl(Duration.ofHours(2)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Falha ao recuperar chave '{}' do cache '{}'. Prosseguindo com consulta no banco: {}",
                        key, cache.getName(), exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("Falha ao persistir chave '{}' no cache '{}': {}",
                        key, cache.getName(), exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Falha ao invalidar chave '{}' do cache '{}': {}",
                        key, cache.getName(), exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("Falha ao limpar cache '{}': {}",
                        cache.getName(), exception.getMessage());
            }
        };
    }

    public static RedisSerializer<Object> createJsonSerializer() {
        return GenericJacksonJsonRedisSerializer.builder()
                .enableSpringCacheNullValueSupport()
                .enableUnsafeDefaultTyping()
                .customize(builder -> {
                    tools.jackson.databind.module.SimpleModule pageModule = new tools.jackson.databind.module.SimpleModule("PageImplModule");
                    pageModule.addDeserializer(org.springframework.data.domain.PageImpl.class, new PageImplDeserializer());
                    builder.addModule(pageModule);
                })
                .build();
    }

    public static class PageImplDeserializer extends tools.jackson.databind.ValueDeserializer<org.springframework.data.domain.PageImpl<?>> {
        @Override
        public org.springframework.data.domain.PageImpl<?> deserialize(tools.jackson.core.JsonParser p, tools.jackson.databind.DeserializationContext ctxt) throws tools.jackson.core.JacksonException {
            tools.jackson.databind.JsonNode node = ctxt.readTree(p);

            tools.jackson.databind.JsonNode contentNode = node.get("content");
            List<?> content = List.of();
            if (contentNode != null && contentNode.isArray()) {
                content = ctxt.readTreeAsValue(contentNode, List.class);
            }

            long totalElements = node.has("totalElements") ? node.get("totalElements").asLong() : (content != null ? content.size() : 0);
            int number = node.has("number") ? node.get("number").asInt() : 0;
            int size = node.has("size") && node.get("size").asInt() > 0 ? node.get("size").asInt() : Math.max(1, content != null ? content.size() : 1);

            return new org.springframework.data.domain.PageImpl<>(content != null ? content : List.of(), org.springframework.data.domain.PageRequest.of(number, size), totalElements);
        }

        @Override
        public Object deserializeWithType(tools.jackson.core.JsonParser p, tools.jackson.databind.DeserializationContext ctxt, tools.jackson.databind.jsontype.TypeDeserializer typeDeserializer) throws tools.jackson.core.JacksonException {
            return deserialize(p, ctxt);
        }
    }
}
