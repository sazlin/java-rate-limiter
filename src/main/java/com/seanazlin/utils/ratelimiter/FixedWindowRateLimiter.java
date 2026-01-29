package com.seanazlin.utils.ratelimiter;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FixedWindowRateLimiter extends BaseRateLimiter{
    private static final Logger log = LoggerFactory.getLogger(FixedWindowRateLimiter.class);

    protected volatile Map<String, Long> fixedWindowCounts;
    protected volatile long fixedWindowTime;

    public FixedWindowRateLimiter(long rate, ChronoUnit timeUnit, Clock clock) {
        super(rate, timeUnit, clock);
        fixedWindowCounts = new ConcurrentHashMap<>();
        fixedWindowTime = getCurrentWindow();
    }

    @Override
    public boolean tryAcquire(String userId) {
        long currentWindowTime = getCurrentWindow();
        log.debug("TryAcquire userId:{}, currentWindow:{}, tokenBucketTime:{}", userId, currentWindowTime, fixedWindowTime);

        // Double-checked locking
        if (fixedWindowTime <  currentWindowTime) {
            synchronized (this) {
                if (fixedWindowTime <  currentWindowTime) {
                    fixedWindowTime = currentWindowTime;
                    fixedWindowCounts.clear();
                    log.debug("Fixed window counts reset (New Time: {})", fixedWindowTime);
                }
            }
        }

        long currentUserRate = fixedWindowCounts.getOrDefault(userId, 0L);
        if  (currentUserRate >= ratePerUnit) {
            log.debug("Rate Limited (limit: {})",  ratePerUnit);
            return false;
        }

        // Increment value if present
        final long newRate = fixedWindowCounts.compute(userId, (k, v) -> (v == null) ? 1 : v + 1);

        log.debug("Acquired token for user {} (newRate: {})", userId, newRate);
        return true;
    }

    /*
    Get timeUnit-aligned epoch
     */
    @VisibleForTesting
    long getCurrentWindow() {
        return timeUnit.between(Instant.EPOCH, clock.instant());
    }
}
