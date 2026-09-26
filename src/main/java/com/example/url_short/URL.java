package com.example.url_short;

import jakarta.persistence.*;

@Entity
@Table(name = "urls")
public class URL {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "short_code", unique = true)
    private String shortCode;

    @Column(name = "original_url", nullable = false)
    private String originalUrl;

    @Column(name = "expires_at")
    private java.time.LocalDateTime expiresAt;


    public java.time.LocalDateTime getExpiresAt() {
    return expiresAt;
    }

    public void setExpiresAt(java.time.LocalDateTime expiresAt) {
    this.expiresAt = expiresAt;
       }

    public URL() {
    }

    public URL(String shortCode, String originalUrl) {
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
    }

    public Long getId() {
        return id;
    }

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }
}