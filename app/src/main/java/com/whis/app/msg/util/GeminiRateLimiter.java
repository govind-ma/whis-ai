package com.whis.app.msg.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Calendar;

/**
 * Token-bucket rate limiter for Gemini API calls (MSG_PLAN.md Section 4.3).
 * <p>
 * Max 10 requests/minute, 200 requests/day cap stored in SharedPreferences.
 */
public class GeminiRateLimiter {

    private static final String PREFS_NAME = "whis_gemini_rate_limiter";
    private static final String KEY_DAILY_COUNT = "daily_count";
    private static final String KEY_LAST_DAY = "last_day";

    private static final int MAX_PER_MINUTE = 10;
    private static final int MAX_PER_DAY = 200;

    private static long lastMinuteResetTime = 0;
    private static int minuteTokens = MAX_PER_MINUTE;

    private GeminiRateLimiter() {
        // Utility class
    }

    public static synchronized boolean tryAcquire(Context context) {
        if (context == null) return false;

        long now = System.currentTimeMillis();

        // Minute window refill
        if (now - lastMinuteResetTime >= 60 * 1000L) {
            lastMinuteResetTime = now;
            minuteTokens = MAX_PER_MINUTE;
        }

        if (minuteTokens <= 0) {
            return false;
        }

        // Daily cap check
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int currentDay = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        int storedDay = prefs.getInt(KEY_LAST_DAY, -1);
        int dailyCount = prefs.getInt(KEY_DAILY_COUNT, 0);

        if (storedDay != currentDay) {
            storedDay = currentDay;
            dailyCount = 0;
        }

        if (dailyCount >= MAX_PER_DAY) {
            return false;
        }

        // Consume tokens
        minuteTokens--;
        dailyCount++;

        prefs.edit()
                .putInt(KEY_LAST_DAY, storedDay)
                .putInt(KEY_DAILY_COUNT, dailyCount)
                .apply();

        return true;
    }
}
