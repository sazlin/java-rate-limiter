package com.seanazlin.utils.ratelimiter;

public interface RateLimiter {
    boolean tryAcquire(String userId);
}
