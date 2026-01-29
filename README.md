# rate-limiter

A small, configurable, thread-safe Java rate-limiter library for multi-user services.

[![Build Status](https://img.shields.io/badge/build-__placeholder__-blue)](https://github.com/<owner>/<repo>/actions)
[![Javadocs](https://img.shields.io/badge/api-javadoc-lightgrey)](https://github.com/<owner>/<repo>/docs)
[![License: MIT](https://img.shields.io/badge/license-MIT-yellow.svg)](./LICENSE)

## Key features
- Fixed-window and Sliding-window algorithms
- Thread-safe implementations using ConcurrentHashMap and light synchronization
- Time abstraction via injected `Clock` for deterministic testing
- Minimal dependencies
- Simple API: `boolean tryAcquire(String clientId)`

## Usage
```java
import com.seanazlin.utils.ratelimiter.FixedWindowRateLimiter;
import com.seanazlin.utils.ratelimiter.SlidingWindowRateLimiter;
import java.time.Clock;
import java.time.temporal.ChronoUnit;

// Fixed window: 100 requests per minute
FixedWindowRateLimiter fixedLimiter = new FixedWindowRateLimiter(100, ChronoUnit.MINUTES, Clock.systemUTC());
boolean allowed = fixedLimiter.tryAcquire("user-123");

// Sliding window: 60 requests per minute
SlidingWindowRateLimiter slidingLimiter = new SlidingWindowRateLimiter(60, ChronoUnit.MINUTES, Clock.systemUTC());
boolean allowed2 = slidingLimiter.tryAcquire("user-123");
```

## Configuration options
- rate (long) — maximum requests per unit
- time unit (`java.time.temporal.ChronoUnit`) — e.g., SECONDS, MINUTES, HOURS
- clock — injection of `java.time.Clock` (use `Clock.systemUTC()` in production, `Clock.fixed(...)` or test clocks in unit tests)

## TODO
- Add async methods
- Enable pluggable and extensible storage (w/ a Redis/Valkey storage option built-in)
- Expand unit tests to cover more cases
- Add code coverage

## Tests & CI
- Run tests locally:

```bash
./gradlew test
```

- Manually test / demonstrate different rate limiters:
```bash
./gradlew run
```

License
- This repository is licensed under the MIT License. See `LICENSE` for details.

Acknowledgements
- Built by the author. Uses SLF4J for logging.

