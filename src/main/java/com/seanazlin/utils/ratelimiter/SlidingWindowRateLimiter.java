package com.seanazlin.utils.ratelimiter;

import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SlidingWindowRateLimiter extends BaseRateLimiter {
    private static final Logger log = LoggerFactory.getLogger(SlidingWindowRateLimiter.class);

    protected volatile Map<String, Long> currentWindowCounts;
    protected volatile Map<String, Long> previousWindowCounts;
    protected volatile long currentTimeWindow;
    protected volatile long previousTimeWindow;
    protected final float millisPerUnit;

    public SlidingWindowRateLimiter(long rate, ChronoUnit timeUnit, Clock clock) {
        super(rate, timeUnit, clock);
        currentWindowCounts = new ConcurrentHashMap<>();
        previousWindowCounts = new ConcurrentHashMap<>();
        previousTimeWindow = calcPreviousWindow();
        currentTimeWindow = calcCurrentWindow();
        millisPerUnit = Duration.of(1, timeUnit).toMillis();
    }

    @VisibleForTesting
    protected long getEstimatedCount(final String userId) {
        final long currentCount = currentWindowCounts.getOrDefault(userId, 0L);
        final long previousCount = previousWindowCounts.getOrDefault(userId, 0L);
        final long nowTime = calcCurrentTimeMillis();
        final long diff = nowTime - (this.currentTimeWindow * Math.round(millisPerUnit));
        final float previousWindowFactor = 1.0f - diff / millisPerUnit;
        float intermediate = currentCount + (previousCount * previousWindowFactor);
        double floored = Math.floor(intermediate);
        return Math.round(floored);
    }

    @Override
    public boolean tryAcquire(String userId) {
        final long nowTimeWindow = calcCurrentWindow();
        if (nowTimeWindow > currentTimeWindow) {
            shiftWindows(nowTimeWindow);
        }
        final long estimatedCount = getEstimatedCount(userId);

        if (estimatedCount >= ratePerUnit) {
            log.debug("Rate limiting {} (rate limit: {})",  userId, ratePerUnit);
            return false;
        }

        // ConcurrentHashMap's writes via merge are atomic per-userId, removing
        // the need to synchronize these incrementing writes
        currentWindowCounts.merge(userId, 1L, Long::sum);

        log.debug("Acquired token for user {} (new estimatedCount: {})", userId, estimatedCount + 1);
        return true;
    }

    protected synchronized void shiftWindows(final long newCurrentWindowTime) {
        if (newCurrentWindowTime > currentTimeWindow) {
            log.debug("Shifting windows (old: {}, new: {})", currentTimeWindow, newCurrentWindowTime);
            final Map<String, Long> newWindowCounts = previousWindowCounts;
            previousWindowCounts = currentWindowCounts;
            previousTimeWindow = currentTimeWindow;
            currentWindowCounts = newWindowCounts;
            currentWindowCounts.clear();
            currentTimeWindow = newCurrentWindowTime;
            if(newCurrentWindowTime > currentTimeWindow + millisPerUnit) {
                // TODO: Add a test for this case
                // We've skipped over some time windows, so clear the previous window as well
                previousWindowCounts.clear();
            }
        }
    }

    @VisibleForTesting
    long calcCurrentTimeMillis() {
        return ChronoUnit.MILLIS.between(Instant.EPOCH, clock.instant());
    }

    @VisibleForTesting
    public long calcCurrentWindow() {
        return calcWindowFromInstant(clock.instant());
    }

    @VisibleForTesting
    public long calcWindowFromInstant(final Instant instant) {
        return timeUnit.between(Instant.EPOCH, instant);
    }
    @VisibleForTesting
    public long calcPreviousWindow() {
        return calcWindowFromInstant(clock.instant().minus(1, timeUnit));
    }
}
