package io.github.lucasfcz.coralink.infra.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimiterService {

    @org.springframework.beans.factory.annotation.Value("${coralink.ratelimit.capacity:120}")
    private long capacity;

    @org.springframework.beans.factory.annotation.Value("${coralink.ratelimit.refill-tokens:120}")
    private long refillTokens;

    @org.springframework.beans.factory.annotation.Value("${coralink.ratelimit.refill-duration-minutes:1}")
    private long refillDurationMinutes;

    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();

    public Bucket resolveBucket(String key) {
        return buckets.get(key, k -> newBucket());
    }

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(refillTokens, Duration.ofMinutes(refillDurationMinutes))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }
}