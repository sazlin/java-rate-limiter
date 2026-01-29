package com.seanazlin.utils.ratelimiter;

import java.io.IOException;
import java.time.Clock;
import java.time.temporal.ChronoUnit;

public class Main {
    static void main() throws InterruptedException, IOException {
        final long testPeriodSeconds = 10L;
        final long bobRate = 10L;
        final long janeRate = 33L;
        final String bobId = "bob";
        final String janeId = "jane";
        RateLimiter bobFixedWindowRateLimiter = new FixedWindowRateLimiter(bobRate, ChronoUnit.SECONDS, Clock.systemDefaultZone());
        RateLimiter janeFixedWindowRateLimiter = new FixedWindowRateLimiter(janeRate, ChronoUnit.SECONDS, Clock.systemDefaultZone());
        RateLimiter bobSlidingWindowRateLimiter = new SlidingWindowRateLimiter(bobRate, ChronoUnit.SECONDS, Clock.systemDefaultZone());
        RateLimiter janeSlidingWindowRateLimiter = new SlidingWindowRateLimiter(janeRate, ChronoUnit.SECONDS, Clock.systemDefaultZone());

        int bobAllowedFixed = 0;
        int janeAllowedFixed = 0;
        int bobAllowedSliding = 0;
        int janeAllowedSliding = 0;

        final long startTime = Clock.systemDefaultZone().millis();
        IO.print("Testing for " + testPeriodSeconds + " seconds...");
        while(Clock.systemDefaultZone().millis() < (startTime + 1000 * testPeriodSeconds)) {
            bobAllowedFixed += bobFixedWindowRateLimiter.tryAcquire(bobId) ? 1 : 0;
            janeAllowedFixed += janeFixedWindowRateLimiter.tryAcquire(janeId) ? 1 : 0;
            bobAllowedSliding += bobSlidingWindowRateLimiter.tryAcquire(bobId) ? 1 : 0;
            janeAllowedSliding += janeSlidingWindowRateLimiter.tryAcquire(janeId) ? 1 : 0;
            Thread.sleep(1);
        }
        IO.print("Done\n");

        IO.println("Results:");
        IO.println("  [FIXED WINDOW]");
        IO.println("    [Bob] : Actual (Per Test Period): " + bobAllowedFixed + ". Target: " + (bobRate * testPeriodSeconds));
        IO.println("    [Jane]: Actual (Per Test Period): " + janeAllowedFixed + ". Target: " + (janeRate * testPeriodSeconds));
        IO.println("  [SLIDING WINDOW]");
        IO.println("    [Bob] : Actual (Per Test Period): " + bobAllowedSliding + ". Target: " + (bobRate * testPeriodSeconds));
        IO.println("    [Jane]: Actual (Per Test Period): " + janeAllowedSliding + ". Target: " + (janeRate * testPeriodSeconds));
    }
}
