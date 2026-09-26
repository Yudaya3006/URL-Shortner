package com.example.url_short;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class URLShortener {

    private static final String BASE62 =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private final URLRepository urlRepository;
    private final StringRedisTemplate redisTemplate;

    public URLShortener(
            URLRepository urlRepository,
            StringRedisTemplate redisTemplate) {

        this.urlRepository = urlRepository;
        this.redisTemplate = redisTemplate;
    }

    // Convert database ID into Base62
    public String encodeBase62(long number) {

        StringBuilder result =
                new StringBuilder();

        while (number > 0) {

            int remainder =
                    (int) (number % 62);

            result.append(
                    BASE62.charAt(remainder)
            );

            number =
                    number / 62;
        }

        return result.reverse().toString();
    }

    // Calculate expiration
    private LocalDateTime calculateExpiration(
            String expiration) {

        if (expiration == null
                || expiration.equals("never")) {

            return null;
        }

        switch (expiration) {

            case "1h":
                return LocalDateTime.now()
                        .plusHours(1);

            case "1d":
                return LocalDateTime.now()
                        .plusDays(1);

            case "7d":
                return LocalDateTime.now()
                        .plusDays(7);

            case "30d":
                return LocalDateTime.now()
                        .plusDays(30);

            default:
                return LocalDateTime.now()
                        .plusDays(7);
        }
    }

    // Create short URL
    public String shortenURL(
            String originalURL,
            String customCode,
            String expiration) {

        LocalDateTime expiresAt =
                calculateExpiration(expiration);

        // Automatic short code
        if (customCode == null
                || customCode.isBlank()) {

            var existingURL =
                    urlRepository.findByOriginalUrl(
                            originalURL
                    );

            // Return existing short code
            if (existingURL.isPresent()) {

                return existingURL
                        .get()
                        .getShortCode();
            }

            // Temporary code
            String temporaryCode =
                    UUID.randomUUID().toString();

            URL url =
                    new URL(
                            temporaryCode,
                            originalURL
                    );

            // Set expiration
            url.setExpiresAt(expiresAt);

            // Save first to generate ID
            URL savedURL =
                    urlRepository.save(url);

            // Generate Base62 code
            String shortCode =
                    encodeBase62(
                            savedURL.getId()
                    );

            // Update short code
            savedURL.setShortCode(shortCode);

            urlRepository.save(savedURL);

            return shortCode;
        }

        // Custom short code
        var existingCode =
                urlRepository.findByShortCode(
                        customCode
                );

        // Custom code already exists
        if (existingCode.isPresent()) {

            return null;
        }

        URL url =
                new URL(
                        customCode,
                        originalURL
                );

        // Set selected expiration
        url.setExpiresAt(expiresAt);

        urlRepository.save(url);

        return customCode;
    }

    // Redirect short URL
    public String getOriginalURL(
            String shortCode) {

        // Check Redis first
        String originalURL =
                redisTemplate.opsForValue()
                        .get(shortCode);

        if (originalURL != null) {

            redisTemplate.opsForValue()
                    .increment(
                            "clicks:" + shortCode
                    );

            System.out.println("Redis HIT");

            return originalURL;
        }

        System.out.println("Redis MISS");

        // Redis miss
        var url =
                urlRepository.findByShortCode(
                        shortCode
                );

        if (url.isEmpty()) {

            return null;
        }

        URL savedURL =
                url.get();

        // Check expiration
        if (savedURL.getExpiresAt() != null
                && savedURL
                        .getExpiresAt()
                        .isBefore(
                                LocalDateTime.now()
                        )) {

            return null;
        }

        originalURL =
                savedURL.getOriginalUrl();

        // Increase clicks
        redisTemplate.opsForValue()
                .increment(
                        "clicks:" + shortCode
                );

        // Cache URL
        if (savedURL.getExpiresAt() == null) {

            redisTemplate.opsForValue()
                    .set(
                            shortCode,
                            originalURL,
                            10,
                            TimeUnit.MINUTES
                    );

        } else {

            long remainingSeconds =
                    Duration.between(
                            LocalDateTime.now(),
                            savedURL.getExpiresAt()
                    ).getSeconds();

            if (remainingSeconds > 0) {

                long cacheSeconds =
                        Math.min(
                                remainingSeconds,
                                600
                        );

                redisTemplate.opsForValue()
                        .set(
                                shortCode,
                                originalURL,
                                cacheSeconds,
                                TimeUnit.SECONDS
                        );
            }
        }

        return originalURL;
    }

    // Get click count
    public String getClickCount(
            String shortCode) {

        String count =
                redisTemplate.opsForValue()
                        .get(
                                "clicks:" + shortCode
                        );

        return count == null
                ? "0"
                : count;
    }
}