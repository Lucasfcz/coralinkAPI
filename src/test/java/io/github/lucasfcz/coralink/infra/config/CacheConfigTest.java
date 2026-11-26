package io.github.lucasfcz.coralink.infra.config;

import io.github.lucasfcz.coralink.modules.opportunity.dto.OpportunityResponse;
import io.github.lucasfcz.coralink.modules.opportunity.enums.Modality;
import io.github.lucasfcz.coralink.modules.opportunity.enums.OpportunityType;
import io.github.lucasfcz.coralink.modules.opportunity.enums.TargetCourseAudience;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class CacheConfigTest {

    private final CacheConfig cacheConfig = new CacheConfig();
    private final CacheManager cacheManager = cacheConfig.cacheManager();

    private OpportunityResponse createSampleResponse(Long id) {
        return new OpportunityResponse(
                id,
                "Workshop de Inteligência Artificial",
                "Descrição completa do evento",
                OpportunityType.WORKSHOP,
                "Inteligência Artificial",
                Set.of(TargetCourseAudience.COMPUTER_SCIENCE, TargetCourseAudience.ADS),
                Modality.ONLINE,
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 2),
                LocalDate.of(2026, 9, 28),
                "Online",
                "https://example.com/ai-workshop",
                "CIN_UFPE",
                "https://example.com/banner.png",
                true,
                true,
                LocalDate.of(2026, 10, 1)
        );
    }

    @Test
    @DisplayName("Should register all expected named caches as CaffeineCache with configured TTL and maxSize")
    void shouldRegisterNamedCachesWithExpectedPolicies() {
        assertTrue(cacheManager.getCacheNames().containsAll(List.of(
                CacheConfig.OPPORTUNITIES_CACHE,
                CacheConfig.OPPORTUNITIES_COUNT_CACHE,
                CacheConfig.OPPORTUNITY_DETAIL_CACHE
        )));

        assertCachePolicy(CacheConfig.OPPORTUNITIES_CACHE, CacheConfig.OPPORTUNITIES_TTL, CacheConfig.OPPORTUNITIES_MAX_SIZE);
        assertCachePolicy(CacheConfig.OPPORTUNITIES_COUNT_CACHE, CacheConfig.OPPORTUNITIES_COUNT_TTL, CacheConfig.OPPORTUNITIES_COUNT_MAX_SIZE);
        assertCachePolicy(CacheConfig.OPPORTUNITY_DETAIL_CACHE, CacheConfig.OPPORTUNITY_DETAIL_TTL, CacheConfig.OPPORTUNITY_DETAIL_MAX_SIZE);
    }

    @Test
    @DisplayName("Should apply default fallback policy with TTL and maxSize for dynamic caches")
    void shouldApplyDefaultFallbackPolicyForDynamicCaches() {
        assertCachePolicy("dynamic_test_cache", CacheConfig.OPPORTUNITIES_TTL, CacheConfig.DEFAULT_MAX_SIZE);
    }

    @Test
    @DisplayName("Should store and retrieve OpportunityResponse, PageImpl and Integer values cleanly")
    void shouldStoreAndRetrieveDomainValues() {
        Cache detailCache = cacheManager.getCache(CacheConfig.OPPORTUNITY_DETAIL_CACHE);
        Cache pageCache = cacheManager.getCache(CacheConfig.OPPORTUNITIES_CACHE);
        Cache countCache = cacheManager.getCache(CacheConfig.OPPORTUNITIES_COUNT_CACHE);

        assertNotNull(detailCache);
        assertNotNull(pageCache);
        assertNotNull(countCache);

        OpportunityResponse sample = createSampleResponse(100L);
        detailCache.put(100L, sample);

        OpportunityResponse cachedDetail = detailCache.get(100L, OpportunityResponse.class);
        assertNotNull(cachedDetail);
        assertEquals(100L, cachedDetail.id());
        assertEquals("Workshop de Inteligência Artificial", cachedDetail.title());
        assertEquals(LocalDate.of(2026, 10, 1), cachedDetail.expiresAt());

        Page<OpportunityResponse> page = new PageImpl<>(
                List.of(createSampleResponse(1L), createSampleResponse(2L)),
                PageRequest.of(0, 10),
                2
        );
        pageCache.put("page-key", page);

        Page<?> cachedPage = pageCache.get("page-key", Page.class);
        assertNotNull(cachedPage);
        assertEquals(2, cachedPage.getTotalElements());
        assertEquals(2, cachedPage.getContent().size());

        countCache.put("count", 42);
        assertEquals(42, countCache.get("count", Integer.class));
    }

    @Test
    @DisplayName("Should reject null values when allowNullValues is disabled")
    void shouldRejectNullValues() {
        Cache detailCache = cacheManager.getCache(CacheConfig.OPPORTUNITY_DETAIL_CACHE);
        assertNotNull(detailCache);

        assertThrows(IllegalArgumentException.class, () -> detailCache.put(999L, null));
    }

    @Test
    @DisplayName("Should evict single entry and clear all entries from cache")
    void shouldEvictAndClearCacheEntries() {
        Cache detailCache = cacheManager.getCache(CacheConfig.OPPORTUNITY_DETAIL_CACHE);
        assertNotNull(detailCache);

        detailCache.put(1L, createSampleResponse(1L));
        detailCache.put(2L, createSampleResponse(2L));

        detailCache.evict(1L);
        assertNull(detailCache.get(1L));
        assertNotNull(detailCache.get(2L));

        detailCache.clear();
        assertNull(detailCache.get(2L));
    }

    private void assertCachePolicy(String cacheName, Duration expectedTtl, long expectedMaxSize) {
        Cache cache = cacheManager.getCache(cacheName);
        assertNotNull(cache, "Cache should exist: " + cacheName);
        assertInstanceOf(CaffeineCache.class, cache);

        com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache =
                ((CaffeineCache) cache).getNativeCache();

        var expiration = nativeCache.policy().expireAfterWrite();
        assertTrue(expiration.isPresent(), "expireAfterWrite should be configured for: " + cacheName);
        assertEquals(expectedTtl.toNanos(), expiration.get().getExpiresAfter(TimeUnit.NANOSECONDS));

        var eviction = nativeCache.policy().eviction();
        assertTrue(eviction.isPresent(), "maximumSize eviction should be configured for: " + cacheName);
        assertEquals(expectedMaxSize, eviction.get().getMaximum());
    }
}
