package com.marketplace.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisLockService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String LOCK_PREFIX = "lock:";

    /**
     * Tries to acquire a lock for a specific key.
     *
     * @param key       The unique key to lock (e.g., "auction:123").
     * @param timeoutMs The expiration time for the lock in milliseconds to prevent
     *                  deadlocks.
     * @return true if the lock was acquired, false otherwise.
     */
    public boolean acquireLock(String key, long timeoutMs) {
        String lockKey = LOCK_PREFIX + key;
        Boolean success = redisTemplate.opsForValue().setIfAbsent(lockKey, "LOCKED", Duration.ofMillis(timeoutMs));
        return Boolean.TRUE.equals(success);
    }

    /**
     * Releases the lock for a specific key.
     *
     * @param key The unique key to unlock.
     */
    public void releaseLock(String key) {
        String lockKey = LOCK_PREFIX + key;
        redisTemplate.delete(lockKey);
    }

    /**
     * Attempts to acquire a lock with a retry mechanism.
     *
     * @param key            The unique key to lock.
     * @param requestTimeout The maximum time to wait for the lock in milliseconds.
     * @param lockExpiration The expiration time for the lock itself in
     *                       milliseconds.
     * @return true if the lock was acquired, false if timed out.
     */
    public boolean acquireLockWithRetry(String key, long requestTimeout, long lockExpiration) {
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < requestTimeout) {
            if (acquireLock(key, lockExpiration)) {
                return true;
            }
            try {
                Thread.sleep(50); // Wait 50ms before retrying
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }
}
