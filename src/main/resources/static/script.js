/* ==========================================================================
   URL Shortener - JavaScript Controller
   ========================================================================== */

document.addEventListener("DOMContentLoaded", function () {
    // DOM Elements
    const form = document.getElementById("shortenForm");
    const result = document.getElementById("result");
    const shortUrl = document.getElementById("shortUrl");
    const analytics = document.getElementById("analytics");
    const analyticsCode = document.getElementById("analyticsCode");
    const clickCount = document.getElementById("clickCount");
    const message = document.getElementById("message");
    const errorMessage = document.getElementById("errorMessage");
    const errorText = document.getElementById("errorText");

    const copyButton = document.getElementById("copyButton");
    const shareButton = document.getElementById("shareButton");
    const trackButton = document.getElementById("trackButton");

    const trackFeature = document.getElementById("trackFeature");
    const shareFeature = document.getElementById("shareFeature");
    const refreshAnalyticsBtn = document.getElementById("refreshAnalyticsBtn");
    const submitBtn = document.getElementById("submitBtn");

    let currentShortCode = localStorage.getItem("currentShortCode") || "";
    let savedShortUrl = localStorage.getItem("currentShortUrl") || "";

    /* =========================================
       1. RESTORE SAVED URL ON LOAD
    ========================================= */

    if (savedShortUrl) {
        if (shortUrl) shortUrl.value = savedShortUrl;
        if (result) result.classList.remove("hidden");

        if (!currentShortCode && savedShortUrl) {
            currentShortCode = savedShortUrl.substring(
                savedShortUrl.lastIndexOf("/") + 1
            );
        }

        if (currentShortCode && analytics) {
            analytics.classList.remove("hidden");
            if (analyticsCode) analyticsCode.textContent = currentShortCode;
            loadAnalytics(currentShortCode);
        }
    }

    /* =========================================
       2. CREATE SHORT URL (POST /shorten)
    ========================================= */

    if (form) {
        form.addEventListener("submit", async function (event) {
            event.preventDefault();
            hideError();

            const urlInput = document.getElementById("url");
            const customCodeInput = document.getElementById("customCode");
            const expirationSelect = document.getElementById("expiration");

            let url = urlInput ? urlInput.value.trim() : "";
            const customCode = customCodeInput ? customCodeInput.value.trim() : "";
            const expiration = expirationSelect ? expirationSelect.value : "7d";

            if (!url) {
                showError("Please enter a valid long URL.");
                return;
            }

            // Automatically format URL if scheme missing
            if (!/^https?:\/\//i.test(url)) {
                url = "https://" + url;
            }

            setLoading(true);

            try {
                const response = await fetch("/shorten", {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({
                        url: url,
                        customCode: customCode,
                        expiration: expiration
                    })
                });

                const data = await response.text();

                if (!response.ok) {
                    let friendlyMessage = data;
                    if (response.status === 400) {
                        friendlyMessage = data || "Invalid URL or parameter provided.";
                    } else if (response.status === 409) {
                        friendlyMessage = data || "Custom short URL already exists.";
                    } else if (response.status === 429) {
                        friendlyMessage = data || "Too many requests. Please try again later.";
                    }

                    showError(friendlyMessage);
                    showToast(friendlyMessage, true);
                    return;
                }

                // Success response: plain text short URL
                if (shortUrl) shortUrl.value = data;
                if (result) result.classList.remove("hidden");

                if (message) {
                    message.textContent = "Your short link is ready and active!";
                }

                currentShortCode = data.substring(data.lastIndexOf("/") + 1);

                // Save to localStorage
                localStorage.setItem("currentShortCode", currentShortCode);
                localStorage.setItem("currentShortUrl", data);

                if (analyticsCode) analyticsCode.textContent = currentShortCode;
                if (analytics) analytics.classList.remove("hidden");

                await loadAnalytics(currentShortCode);
                showToast("Short link created successfully!");

                if (result) {
                    result.scrollIntoView({
                        behavior: "smooth",
                        block: "center"
                    });
                }

            } catch (error) {
                console.error("Shorten Error:", error);
                const connErr = "Unable to connect to the server. Please try again.";
                showError(connErr);
                showToast(connErr, true);
            } finally {
                setLoading(false);
            }
        });
    }

    /* =========================================
       3. COPY SHORT URL
    ========================================= */

    if (copyButton) {
        copyButton.addEventListener("click", async function () {
            const urlToCopy = shortUrl ? shortUrl.value : localStorage.getItem("currentShortUrl");
            if (!urlToCopy) {
                showToast("Create a short URL first.", true);
                return;
            }

            const success = await copyToClipboard(urlToCopy);
            if (success) {
                const span = copyButton.querySelector("span");
                const origText = span ? span.textContent : "Copy";
                if (span) span.textContent = "Copied!";

                showToast("Short URL copied to clipboard!");

                setTimeout(() => {
                    if (span) span.textContent = origText;
                }, 1500);
            }
        });
    }

    /* =========================================
       4. TRACK ACTION
    ========================================= */

    const handleTrackAction = async function () {
        if (!currentShortCode) {
            currentShortCode = localStorage.getItem("currentShortCode");
        }

        if (!currentShortCode) {
            showToast("Create a short URL first to view analytics.", true);
            return;
        }

        if (analytics) analytics.classList.remove("hidden");
        if (analyticsCode) analyticsCode.textContent = currentShortCode;

        await loadAnalytics(currentShortCode);

        if (analytics) {
            analytics.scrollIntoView({
                behavior: "smooth",
                block: "center"
            });
        }
    };

    if (trackFeature) trackFeature.addEventListener("click", handleTrackAction);
    if (trackButton) trackButton.addEventListener("click", handleTrackAction);

    /* =========================================
       5. SHARE ACTION
    ========================================= */

    const handleShareAction = async function () {
        let urlToShare = shortUrl ? shortUrl.value : "";
        if (!urlToShare) {
            urlToShare = localStorage.getItem("currentShortUrl") || "";
        }

        if (!currentShortCode) {
            currentShortCode = localStorage.getItem("currentShortCode");
        }

        if (!currentShortCode || !urlToShare) {
            showToast("Create a short URL first to share it.", true);
            return;
        }

        if (navigator.share) {
            try {
                await navigator.share({
                    title: "Shortened Link",
                    text: "Check out this short link:",
                    url: urlToShare
                });
            } catch (err) {
                if (err.name !== "AbortError") {
                    await fallbackCopy(urlToShare);
                }
            }
        } else {
            await fallbackCopy(urlToShare);
        }
    };

    async function fallbackCopy(url) {
        const copied = await copyToClipboard(url);
        if (copied) {
            showToast("Short URL copied to clipboard for sharing!");
        }
    }

    if (shareFeature) shareFeature.addEventListener("click", handleShareAction);
    if (shareButton) shareButton.addEventListener("click", handleShareAction);

    /* =========================================
       6. REFRESH ANALYTICS
    ========================================= */

    if (refreshAnalyticsBtn) {
        refreshAnalyticsBtn.addEventListener("click", async function () {
            if (currentShortCode) {
                await loadAnalytics(currentShortCode);
                showToast("Analytics updated!");
            } else {
                showToast("No active link to track.", true);
            }
        });
    }

    /* =========================================
       7. ANALYTICS FETCH (GET /analytics/{code})
    ========================================= */

    async function loadAnalytics(code) {
        if (!code) return;

        try {
            const response = await fetch("/analytics/" + encodeURIComponent(code));
            if (!response.ok) {
                console.error("Analytics request failed with status:", response.status);
                return;
            }

            const data = await response.text();
            const match = data.match(/Clicks:\s*(\d+)/i);

            if (match && clickCount) {
                clickCount.textContent = match[1];
            }
        } catch (error) {
            console.error("Analytics fetch error:", error);
        }
    }

    /* =========================================
       8. UTILITY HELPERS
    ========================================= */

    async function copyToClipboard(text) {
        try {
            if (navigator.clipboard && window.isSecureContext) {
                await navigator.clipboard.writeText(text);
                return true;
            } else if (shortUrl) {
                shortUrl.select();
                document.execCommand("copy");
                return true;
            }
            return false;
        } catch (err) {
            console.error("Copy failed:", err);
            return false;
        }
    }

    function showError(msg) {
        if (errorMessage && errorText) {
            errorText.textContent = msg;
            errorMessage.classList.remove("hidden");
        }
    }

    function hideError() {
        if (errorMessage) {
            errorMessage.classList.add("hidden");
        }
    }

    function setLoading(isLoading) {
        if (!submitBtn) return;
        const btnText = submitBtn.querySelector(".btn-text");
        const btnIcon = submitBtn.querySelector(".btn-icon");
        const btnLoader = submitBtn.querySelector(".btn-loader");

        if (isLoading) {
            submitBtn.disabled = true;
            if (btnText) btnText.textContent = "Shortening...";
            if (btnIcon) btnIcon.classList.add("hidden");
            if (btnLoader) btnLoader.classList.remove("hidden");
        } else {
            submitBtn.disabled = false;
            if (btnText) btnText.textContent = "Shorten URL";
            if (btnIcon) btnIcon.classList.remove("hidden");
            if (btnLoader) btnLoader.classList.add("hidden");
        }
    }

    function showToast(msg, isError = false) {
        const container = document.getElementById("toastContainer");
        if (!container) return;

        const toast = document.createElement("div");
        toast.className = `toast ${isError ? "toast-error" : ""}`;
        toast.innerHTML = `<span>${isError ? "⚠️" : "✨"}</span><span>${escapeHtml(msg)}</span>`;

        container.appendChild(toast);

        setTimeout(() => {
            toast.style.opacity = "0";
            toast.style.transform = "translateX(40px)";
            toast.style.transition = "all 0.3s ease";
            setTimeout(() => toast.remove(), 300);
        }, 3200);
    }

    function escapeHtml(str) {
        return String(str)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;");
    }
});