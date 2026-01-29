package com.seanazlin.utils.ratelimiter;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FixedWindowRateLimiterTest {
    @Test
    void tryAcquireSimple() {
        final Clock clock = Clock.fixed(Instant.parse("2023-01-01T10:00:00Z"), ZoneId.of("UTC"));
        final long rateLimit = 5;
        final String userId = UUID.randomUUID().toString();
        RateLimiter rateLimiter = new FixedWindowRateLimiter(rateLimit, ChronoUnit.SECONDS, clock);
        for (int i = 0; i < rateLimit; i++) {
            assertTrue(rateLimiter.tryAcquire(userId));
        }
        assertFalse(rateLimiter.tryAcquire(userId));
    }

    @Test
    void tryAcquireBucketResets() {
        final Instant instant = Instant.parse("2023-01-01T10:00:00Z");
        final Clock clock = Clock.fixed(instant, ZoneId.of("UTC"));
        final Clock mockClock = mock(Clock.class);

        when(mockClock.instant()).thenReturn(clock.instant());
        final long rateLimit = 2;
        final String userId = UUID.randomUUID().toString();
        RateLimiter rateLimiter = new FixedWindowRateLimiter(rateLimit, ChronoUnit.SECONDS, mockClock);
        for (int i = 0; i < rateLimit; i++) {
            assertTrue(rateLimiter.tryAcquire(userId));
        }
        assertFalse(rateLimiter.tryAcquire(userId));

        when(mockClock.instant()).thenReturn(instant.plus(1, ChronoUnit.SECONDS));
        for (int i = 0; i < rateLimit; i++) {
            assertTrue(rateLimiter.tryAcquire(userId));
        }
        assertFalse(rateLimiter.tryAcquire(userId));
    }

    @Test
    void tryAcquireMultipleThreads() throws ExecutionException, InterruptedException {
        final Instant instant = Instant.parse("2023-01-01T10:00:00Z");
        final Clock clock = Clock.fixed(instant, ZoneId.of("UTC"));
        final Clock mockClock = mock(Clock.class);

        when(mockClock.instant()).thenReturn(clock.instant());
        final long rateLimit = 100;
        final String userIdA = UUID.randomUUID().toString();
        final String userIdB = UUID.randomUUID().toString();
        final String userIdC = UUID.randomUUID().toString();
        final String userIdD = UUID.randomUUID().toString();

        RateLimiter rateLimiter = new FixedWindowRateLimiter(rateLimit, ChronoUnit.SECONDS, mockClock);

        Function<String, Boolean> doStuff = (final String userId) -> {
            for (int i = 0; i < rateLimit; i++) {
                assertTrue(rateLimiter.tryAcquire(userId));
            }
            assertFalse(rateLimiter.tryAcquire(userId));
            return true;
        };

        CompletableFuture.allOf(
                CompletableFuture.supplyAsync(() -> doStuff.apply(userIdA)),
                CompletableFuture.supplyAsync(() -> doStuff.apply(userIdB)),
                CompletableFuture.supplyAsync(() -> doStuff.apply(userIdC)),
                CompletableFuture.supplyAsync(() -> doStuff.apply(userIdD))
        ).get();

        when(mockClock.instant()).thenReturn(instant.plus(1, ChronoUnit.SECONDS));

        CompletableFuture.allOf(
                CompletableFuture.supplyAsync(() -> doStuff.apply(userIdA)),
                CompletableFuture.supplyAsync(() -> doStuff.apply(userIdB)),
                CompletableFuture.supplyAsync(() -> doStuff.apply(userIdC)),
                CompletableFuture.supplyAsync(() -> doStuff.apply(userIdD))
        ).get();
    }
}