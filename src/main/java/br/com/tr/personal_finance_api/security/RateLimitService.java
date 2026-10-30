package br.com.tr.personal_finance_api.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private static final int LOGIN_REQUESTS_PER_MINUTE = 10;
    private static final int GENERAL_REQUESTS_PER_MINUTE = 100;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public Bucket resolveBucket(String clientIp, boolean isLoginEndpoint) {
        String bucketKey = clientIp + (isLoginEndpoint ? ":login" : ":general");

        return buckets.computeIfAbsent(
                bucketKey,
                key -> createBucket(isLoginEndpoint)
        );
    }

    private Bucket createBucket(boolean isLoginEndpoint) {
        int capacity = isLoginEndpoint
                ? LOGIN_REQUESTS_PER_MINUTE
                : GENERAL_REQUESTS_PER_MINUTE;

        return Bucket.builder()
                .addLimit(
                        Bandwidth.classic(
                                capacity,
                                Refill.intervally(capacity, Duration.ofMinutes(1))
                        )
                )
                .build();
    }
}