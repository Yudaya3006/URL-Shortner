package com.example.url_short;

import java.net.InetAddress;
import java.net.URI;
import java.util.Locale;

import org.springframework.stereotype.Service;

@Service
public class URLSecurityService {

    public boolean isSafeURL(String url) {

        try {

            if (url == null || url.isBlank()) {
                return false;
            }

            URI uri = URI.create(url);

            String scheme = uri.getScheme();

            if (scheme == null ||
                    (!scheme.equalsIgnoreCase("http")
                    && !scheme.equalsIgnoreCase("https"))) {
                return false;
            }

            String host = uri.getHost();

            if (host == null || host.isBlank()) {
                return false;
            }

            host = host.toLowerCase(Locale.ROOT);

            // Block localhost
            if (host.equals("localhost")) {
                return false;
            }

            // Block direct IP addresses
            if (isIPAddress(host)) {
                return false;
            }

            // Resolve hostname
            InetAddress address =
                    InetAddress.getByName(host);

            // Block local/private addresses
            if (address.isAnyLocalAddress()
                    || address.isLoopbackAddress()
                    || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress()) {
                return false;
            }

            // Suspicious URL checks
            if (isSuspiciousURL(uri)) {
                return false;
            }

            return true;

        } catch (Exception e) {
            return false;
        }
    }

    private boolean isSuspiciousURL(URI uri) {

        String url =
                uri.toString().toLowerCase(Locale.ROOT);

        String host =
                uri.getHost().toLowerCase(Locale.ROOT);

        String[] suspiciousKeywords = {
                "phishing",
                "malware",
                "ransomware",
                "keylogger",
                "credential-steal",
                "credentialsteal",
                "account-verify",
                "accountverify",
                "verify-account",
                "verifyaccount",
                "login-verify",
                "loginverify",
                "secure-login",
                "securelogin"
        };

        for (String keyword : suspiciousKeywords) {

            if (url.contains(keyword)) {
                return true;
            }
        }

        if (url.contains("%00")
                || url.contains("%0d")
                || url.contains("%0a")) {
            return true;
        }

        int dotCount =
                host.length() - host.replace(".", "").length();

        if (dotCount > 5) {
            return true;
        }

        if (host.contains("@")) {
            return true;
        }

        return false;
    }

    private boolean isIPAddress(String host) {

        // IPv4
        if (host.matches(
                "\\d{1,3}(\\.\\d{1,3}){3}")) {
            return true;
        }

        // IPv6
        return host.contains(":");
    }
}