package com.whis.app.agent.emergency;

import android.content.Context;
import android.telephony.SmsManager;

import com.whis.app.agent.model.UserProfile;

/**
 * Sends emergency SMS alert to user's designated emergency contact (AI_AGENT_PLAN.md Section 4.2 & 4.5).
 */
public class EmergencyContactNotifier {

    private EmergencyContactNotifier() {
        // Utility class
    }

    public static boolean sendEmergencyAlert(Context context, UserProfile profile) {
        String phone1 = com.whis.app.core.EmergencyContactStore.getC1Phone(context);
        String phone2 = com.whis.app.core.EmergencyContactStore.getC2Phone(context);

        String message = "[WHIS EMERGENCY ALERT] " + (profile != null && profile.name != null ? profile.name : "User")
                + " may be experiencing an active cyber fraud / digital arrest incident. Please check on them or call 1930 immediately.";

        boolean sentAny = false;
        SmsManager smsManager = SmsManager.getDefault();

        if (phone1 != null && !phone1.trim().isEmpty()) {
            try {
                smsManager.sendTextMessage(phone1.trim(), null, message, null, null);
                sentAny = true;
            } catch (Exception ignored) {}
        }

        if (phone2 != null && !phone2.trim().isEmpty()) {
            try {
                smsManager.sendTextMessage(phone2.trim(), null, message, null, null);
                sentAny = true;
            } catch (Exception ignored) {}
        }

        // Fallback to legacy profile contact if set
        if (!sentAny && profile != null && profile.emergencyContact != null && !profile.emergencyContact.trim().isEmpty()) {
            try {
                smsManager.sendTextMessage(profile.emergencyContact.trim(), null, message, null, null);
                sentAny = true;
            } catch (Exception ignored) {}
        }

        return sentAny;
    }
}
