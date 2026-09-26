package com.example.url_short;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface URLRepository extends JpaRepository<URL, Long> {

    Optional<URL> findByShortCode(String shortCode);

    Optional<URL> findByOriginalUrl(String originalUrl);
}