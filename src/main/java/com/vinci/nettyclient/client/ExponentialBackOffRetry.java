package com.vinci.nettyclient.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;


public class ExponentialBackOffRetry implements RetryPolicy {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExponentialBackOffRetry.class);

    private static final int MAX_RETRIES_LIMIT = 29;
    private static final int DEFAULT_MAX_SLEEP_MS = Integer.MAX_VALUE;

    private final Random random = new Random();
    private final long baseSleepTimeMs;
    private final int maxRetries;
    private final int maxSleepMs;

    public ExponentialBackOffRetry(int baseSleepTimeMs, int maxRetries) {
        this(baseSleepTimeMs, maxRetries, DEFAULT_MAX_SLEEP_MS);
    }

    public ExponentialBackOffRetry(int baseSleepTimeMs, int maxRetries, int maxSleepMs) {
        this.maxRetries = maxRetries;
        this.baseSleepTimeMs = baseSleepTimeMs;
        this.maxSleepMs = maxSleepMs;
    }

    @Override
    public boolean allowRetry(int retryCount) {
        if (retryCount < maxRetries) {
            return true;
        }
        return false;
    }

    @Override
    public long getSleepTimeMs(int retryCount) {
        if (retryCount < 0) {
            throw new IllegalArgumentException("retries count must greater than 0.");
        }
        if (retryCount > MAX_RETRIES_LIMIT) {
            LOGGER.info("maxRetries too large ({}). Pinning to {}", maxRetries, MAX_RETRIES_LIMIT);
            retryCount = MAX_RETRIES_LIMIT;
        }
        long sleepMs = baseSleepTimeMs * Math.max(1, random.nextInt(1 << retryCount));
        if (sleepMs > maxSleepMs) {
            sleepMs = maxSleepMs;
        }
        return sleepMs;
    }
}
