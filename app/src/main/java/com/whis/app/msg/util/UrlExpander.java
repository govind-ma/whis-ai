package com.whis.app.msg.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * URL Expansion Utility (MSG_PLAN.md Section 2.6 & 4.1).
 * <p>
 * Follows HTTP redirect chains (up to 5 hops) without fetching full page HTML
 * to resolve shortened links (bit.ly, tinyurl, etc.).
 */
public class UrlExpander {

    private static final int MAX_HOPS = 5;
    private static final int TIMEOUT_MS = 3000;

    private UrlExpander() {
        // Utility class
    }

    /**
     * Expand a shortened URL to its final destination URL.
     *
     * @param originalUrl raw URL extracted from SMS body
     * @return final destination URL, or original URL if unexpanded/error
     */
    public static String expand(String originalUrl) {
        if (originalUrl == null || !originalUrl.startsWith("http")) {
            return originalUrl;
        }

        String currentUrl = originalUrl;
        int hops = 0;

        while (hops < MAX_HOPS) {
            try {
                URL url = new URL(currentUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setInstanceFollowRedirects(false);
                conn.setConnectTimeout(TIMEOUT_MS);
                conn.setReadTimeout(TIMEOUT_MS);
                conn.setRequestMethod("HEAD");
                conn.connect();

                int responseCode = conn.getResponseCode();
                if (responseCode >= 300 && responseCode < 400) {
                    String location = conn.getHeaderField("Location");
                    if (location != null && !location.isEmpty()) {
                        if (location.startsWith("/")) {
                            currentUrl = url.getProtocol() + "://" + url.getHost() + location;
                        } else {
                            currentUrl = location;
                        }
                        hops++;
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            } catch (Exception e) {
                // Timeout or network unreachable — return last known URL
                break;
            }
        }

        return currentUrl;
    }
}
