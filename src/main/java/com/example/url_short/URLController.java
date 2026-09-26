package com.example.url_short;

import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class URLController {

    private final URLShortener urlShortener;
    private final RateLimitService rateLimitService;
    private final URLSecurityService urlSecurityService;

    public URLController(
            URLShortener urlShortener,
            RateLimitService rateLimitService,
            URLSecurityService urlSecurityService) {

        this.urlShortener = urlShortener;
        this.rateLimitService = rateLimitService;
        this.urlSecurityService = urlSecurityService;
    }

    // Home page
    @GetMapping("/")
    public ResponseEntity<Resource> home() {

        Resource resource =
                new ClassPathResource("static/index.html");

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(resource);
    }

    // Create short URL
    @PostMapping("/shorten")
    public ResponseEntity<String> shortenURL(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {

        // Get user's IP address
        String ipAddress =
                httpRequest.getRemoteAddr();

        // Rate limiting
        if (!rateLimitService.isAllowed(ipAddress)) {

            return ResponseEntity
                    .status(429)
                    .body("Too many requests. Try again later.");
        }

        // Get original URL
        String originalURL =
                request.get("url");

        // Get custom short code
        String customCode =
                request.get("customCode");

        // Get expiration
        String expiration =
                request.get("expiration");

        // Check URL security
        if (originalURL == null
                || !urlSecurityService.isSafeURL(originalURL)) {

            return ResponseEntity
                    .badRequest()
                    .body("URL is not allowed");
        }

        // Validate custom code
        if (customCode != null
                && !customCode.isBlank()) {

            if (!customCode.matches(
                    "[a-zA-Z0-9_-]{3,30}")) {

                return ResponseEntity
                        .badRequest()
                        .body(
                                "Custom code must contain only letters, numbers, _ or -"
                        );
            }

            // Reserved paths
            if (customCode.equalsIgnoreCase("shorten")
                    || customCode.equalsIgnoreCase("analytics")) {

                return ResponseEntity
                        .badRequest()
                        .body("This custom code is reserved");
            }
        }

        // Validate expiration
        if (expiration == null
                || !(expiration.equals("1h")
                || expiration.equals("1d")
                || expiration.equals("7d")
                || expiration.equals("30d")
                || expiration.equals("never"))) {

            return ResponseEntity
                    .badRequest()
                    .body("Invalid expiration time");
        }

        // Generate short code
        String shortCode =
                urlShortener.shortenURL(
                        originalURL,
                        customCode,
                        expiration
                );

        // Custom code already exists
        if (shortCode == null) {

            return ResponseEntity
                    .status(409)
                    .body("Custom short URL already exists");
        }

        // Return short URL
        return ResponseEntity.ok(
                getBaseUrl(httpRequest)
                        + "/"
                        + shortCode
        );
    }

    // Redirect short URL
    @GetMapping("/{shortCode:[a-zA-Z0-9_-]+}")
    public ResponseEntity<Void> redirectURL(
            @PathVariable String shortCode) {

        String originalURL =
                urlShortener.getOriginalURL(shortCode);

        // Not found or expired
        if (originalURL == null) {

            return ResponseEntity
                    .notFound()
                    .build();
        }

        // Redirect
        return ResponseEntity
                .status(302)
                .header("Location", originalURL)
                .build();
    }

    // Analytics
    @GetMapping("/analytics/{shortCode}")
    public String getAnalytics(
            @PathVariable String shortCode) {

        String clicks =
                urlShortener.getClickCount(shortCode);

        return "Short code: " + shortCode
                + "\nClicks: " + clicks;
    }

    // Build URL dynamically
    private String getBaseUrl(
            HttpServletRequest request) {

        String scheme =
                request.getScheme();

        String serverName =
                request.getServerName();

        int port =
                request.getServerPort();

        if (serverName.equals("localhost")
                || serverName.equals("127.0.0.1")) {

            return scheme
                    + "://"
                    + serverName
                    + ":"
                    + port;
        }

        return scheme
                + "://"
                + serverName;
    }
}