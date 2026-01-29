package com.seanazlin.utils.ratelimiter;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class SlidingWindowRateLimiterTest {
    @Test
    void getEstimatedCount() {
    }

    @Test
    void tryAcquireSimple() {
        final Instant instant = Instant.parse("2023-01-01T10:00:00.999Z"); // Right up against end of current window
        final Clock clock = Clock.fixed(instant, ZoneId.of("UTC"));
        final Clock mockClock = mock(Clock.class);
        when(mockClock.instant()).thenReturn(instant);

        final long rateLimit = 5;
        final String userId = UUID.randomUUID().toString();
        final RateLimiter rateLimiter = new SlidingWindowRateLimiter(rateLimit, ChronoUnit.SECONDS, clock);
        for (int i = 0; i < rateLimit; i++) {
            assertTrue(rateLimiter.tryAcquire(userId));
        }
        assertFalse(rateLimiter.tryAcquire(userId));
    }

    @Test
    void tryAcquireHitLimitAcrossSlide() {
        final Instant instant = Instant.parse("2023-01-01T10:00:00.500Z"); // Right up against end of current window
        final Clock clock = Clock.fixed(instant, ZoneId.of("UTC"));
        final Clock mockClock = mock(Clock.class);
        when(mockClock.instant()).thenReturn(instant);
        final long rateLimit = 6;
        final String userId = UUID.randomUUID().toString();
        RateLimiter rateLimiter = new SlidingWindowRateLimiter(rateLimit, ChronoUnit.SECONDS, mockClock);

        // 50% of these 2 (read: 1) will be counted towards the estimate
        for (int i = 0; i < 2; i++) {
            assertTrue(rateLimiter.tryAcquire(userId));
        }
        // Move to next window (triggering window slide) and fill rateLimit - 1
        when(mockClock.instant()).thenReturn(instant.plus(1,  ChronoUnit.SECONDS));
        for (int i = 0; i < rateLimit - 1; i++) {
            assertTrue(rateLimiter.tryAcquire(userId));
        }

        // One more should hit the limit
        assertFalse(rateLimiter.tryAcquire(userId));
    }

    @Test
    void tryAcquireHitLimitAcrossSlidePreviousRoundsDown() {
        final Instant instant = Instant.parse("2023-01-01T10:00:00.500Z"); // Right up against end of current window
        final Clock clock = Clock.fixed(instant, ZoneId.of("UTC"));
        final Clock mockClock = mock(Clock.class);
        when(mockClock.instant()).thenReturn(instant);
        final long rateLimit = 6;
        final String userId = UUID.randomUUID().toString();
        RateLimiter rateLimiter = new SlidingWindowRateLimiter(rateLimit, ChronoUnit.SECONDS, mockClock);

        // 1 in previous window, which we expect to round down to 0 in this test
        for (int i = 0; i < 1; i++) {
            assertTrue(rateLimiter.tryAcquire(userId));
        }
        // Move to next window - just barely (triggering window slide)
        // We expect to run all the way to the limit here because the previous window rounded 1 * 0.99 to 0
        when(mockClock.instant()).thenReturn(instant.plus(501,  ChronoUnit.MILLIS));
        for (int i = 0; i < rateLimit; i++) {
            assertTrue(rateLimiter.tryAcquire(userId));
        }

        // One more should hit the limit
        assertFalse(rateLimiter.tryAcquire(userId));
    }

    @Test
    void calcPreviousWindow() {
        final ChronoUnit timeUnit = ChronoUnit.SECONDS;
        final Instant instant = Instant.parse("2023-01-01T10:00:00.500Z"); // Right up against end of current window
        final Clock mockClock = mock(Clock.class);
        when(mockClock.instant()).thenReturn(instant);
        SlidingWindowRateLimiter rateLimiter = new SlidingWindowRateLimiter(5, timeUnit, mockClock);
        assertEquals(1672567200L, rateLimiter.calcCurrentWindow());
        assertEquals(1672567199L, rateLimiter.calcPreviousWindow());
    }

    @Test
    void calcCurrentWindow() {
        final ChronoUnit timeUnit = ChronoUnit.SECONDS;
        final Instant instant = Instant.parse("2023-01-01T10:00:00.500Z");
        final Clock mockClock = mock(Clock.class);
        when(mockClock.instant()).thenReturn(instant);
        SlidingWindowRateLimiter rateLimiter = new SlidingWindowRateLimiter(5, timeUnit, mockClock);
        assertEquals(1672567200L, rateLimiter.calcCurrentWindow());
        assertEquals(1672567199L, rateLimiter.calcPreviousWindow());
        when(mockClock.instant()).thenReturn(instant.plus(501, ChronoUnit.MILLIS));
        assertEquals(1672567201L, rateLimiter.calcCurrentWindow());
        assertEquals(1672567200L, rateLimiter.calcPreviousWindow());
    }

    @Test
    void calcWindowFromInstantArg() {
        final ChronoUnit timeUnit = ChronoUnit.SECONDS;
        final Instant instant = Instant.parse("2023-01-01T10:00:00.999Z");
        final Clock clock = Clock.fixed(instant, ZoneId.of("UTC"));
        SlidingWindowRateLimiter rateLimiter = new SlidingWindowRateLimiter(5, timeUnit, Clock.systemUTC());
        assertEquals(1672567200L, rateLimiter.calcWindowFromInstant(instant));
        assertEquals(1672567201L, rateLimiter.calcWindowFromInstant(instant.plus(1, timeUnit)));
    }

}