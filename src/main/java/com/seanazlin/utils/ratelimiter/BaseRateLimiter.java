package com.seanazlin.utils.ratelimiter;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public abstract class BaseRateLimiter implements RateLimiter {
    protected final Clock clock;
    protected final long ratePerUnit;
    protected final ChronoUnit timeUnit;

    protected BaseRateLimiter(long ratePerUnit, ChronoUnit timeUnit, Clock clock) {
        this.ratePerUnit = ratePerUnit;
        this.timeUnit = Objects.requireNonNullElse(timeUnit, ChronoUnit.SECONDS);
        this.clock = Objects.requireNonNullElse(clock, Clock.systemUTC());
    }
}
