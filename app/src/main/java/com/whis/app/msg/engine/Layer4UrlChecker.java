package com.whis.app.msg.engine;

import android.content.Context;

import com.whis.app.core.WhisConsentManager;
import com.whis.app.msg.util.UrlExpander;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Layer 4 — URL Reputation & Safe Browsing Checker (MSG_PLAN.md Section 2.6, 4.2 & Adjustment #6).
 * <p>
 * <b>Consent Gated:</b> Calls {@link WhisConsentManager#isConsentGiven(Context)} before execution.
 * If consent is not given, Layer 4 is skipped entirely to keep data processing 100% on-device.
 */
public class Layer4UrlChecker {

    private static final Pattern URL_REGEX = Pattern.compile(
            "(?i)\\b(https?|ftp)://[-a-z0-9+&@#/%?=~_|!:,.;]*[-a-z0-9+&@#/%=~_|]"
    );

    private static final Pattern SUSPICIOUS_DOMAIN_PATTERN = Pattern.compile(
            "(?i)\\b(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|.*\\.(xyz|top|site|club|online|tech|fit|work|live|app|icu|ru|tk|ml|ga|cf|gq|cn|cc|vip|biz))\\b"
    );

    public static class UrlCheckResult {
        public final boolean consentGiven;
        public final boolean containsUrl;
        public final String originalUrl;
        public final String expandedUrl;
        public final boolean isSuspiciousDomain;
        public final int riskDelta;

        public UrlCheckResult(boolean consentGiven, boolean containsUrl, String originalUrl,
                              String expandedUrl, boolean isSuspiciousDomain, int riskDelta) {
            this.consentGiven = consentGiven;
            this.containsUrl = containsUrl;
            this.originalUrl = originalUrl;
            this.expandedUrl = expandedUrl;
            this.isSuspiciousDomain = isSuspiciousDomain;
            this.riskDelta = riskDelta;
        }
    }

    private Layer4UrlChecker() {
        // Utility class
    }

    /**
     * Perform Layer 4 URL reputation check.
     */
    public static UrlCheckResult check(Context context, String body) {
        // DPDP Consent Check (Adjustment #6)
        if (context != null && !WhisConsentManager.isConsentGiven(context)) {
            return new UrlCheckResult(false, false, null, null, false, 0);
        }

        if (body == null || body.trim().isEmpty()) {
            return new UrlCheckResult(true, false, null, null, false, 0);
        }

        Matcher matcher = URL_REGEX.matcher(body);
        if (!matcher.find()) {
            return new UrlCheckResult(true, false, null, null, false, 0);
        }

        String rawUrl = matcher.group();
        String expandedUrl = UrlExpander.expand(rawUrl);

        boolean isSuspiciousDomain = false;
        int riskDelta = 0;

        if (SUSPICIOUS_DOMAIN_PATTERN.matcher(expandedUrl).find()) {
            isSuspiciousDomain = true;
            riskDelta = 35;
        } else if (!rawUrl.equals(expandedUrl)) {
            riskDelta = 20;
        }

        return new UrlCheckResult(true, true, rawUrl, expandedUrl, isSuspiciousDomain, riskDelta);
    }
}
