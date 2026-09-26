package com.example.url_short;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;

    public RateLimitService(
            StringRedisTemplate redisTemplate) {

        this.redisTemplate = redisTemplate;
    }

    public boolean isAllowed(String ipAddress) {

        String key =
                "rate_limit:" + ipAddress;

        Long count =
                redisTemplate.opsForValue()
                        .increment(key);

        if (count != null && count == 1) {

            redisTemplate.expire(
                    key,
                    1,
                    TimeUnit.MINUTES
            );
        }

        return count != null && count <= 10;
    }
}