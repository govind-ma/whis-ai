package com.whis.app.agent.context;

import android.content.Context;
import android.content.SharedPreferences;

import com.whis.app.agent.model.UserProfile;

import org.json.JSONObject;

/**
 * Reads user profile from SharedPreferences with graceful default fallback (AI_AGENT_PLAN.md Section 4.2 & Adjustment #2).
 */
public class UserProfileContext {

    private static final String PREFS_NAME = "whis_prefs";
    private static final String KEY_USER_PROFILE = "whis_user_profile";

    private UserProfileContext() {
        // Utility class
    }

    public static UserProfile getProfile(Context context) {
        if (context == null) return new UserProfile();

        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String jsonStr = prefs.getString(KEY_USER_PROFILE, null);
            if (jsonStr != null && !jsonStr.trim().isEmpty()) {
                JSONObject json = new JSONObject(jsonStr);
                return new UserProfile(
                        json.optString("name", "User"),
                        json.optString("ageGroup", "26-40"),
                        json.optString("occupation", "Professional"),
                        json.optString("language", "Hindi"),
                        json.optString("techLevel", "Basic"),
                        json.optString("primaryUpi", "Google Pay"),
                        json.optString("bankName", "SBI"),
                        json.optString("emergencyContact", null)
                );
            }
        } catch (Exception e) {
            // Fallback to default profile
        }

        return new UserProfile();
    }
}
