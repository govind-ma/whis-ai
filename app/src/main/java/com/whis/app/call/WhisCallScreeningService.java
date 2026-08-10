package com.whis.app.call;

import android.os.Build;
import android.telecom.Call;
import android.telecom.CallScreeningService;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

/**
 * WhisCallScreeningService — silently rejects incoming calls from blocked numbers.
 * <p>
 * On Android 10+ (API 29), an app with the CALL_SCREENING role can reject calls without
 * being the default dialer. This service is called by the system for every incoming call.
 * <p>
 * How to enable:
 *   The user must grant Whis the "Call Screening" role via
 *   {@code RoleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)} prompt,
 *   which is shown once in {@link com.whis.app.ui.onboarding.OnboardingActivity}.
 * <p>
 * Fallback (pre-Android 10): We rely on {@link android.provider.BlockedNumberContract}
 * which is synced in {@link BlockedNumberStore#block} so the system dialer rejects the call.
 */
@RequiresApi(api = Build.VERSION_CODES.Q) // Android 10+
public class WhisCallScreeningService extends CallScreeningService {

    private static final String TAG = "WhisCallScreening";

    @Override
    public void onScreenCall(@NonNull Call.Details callDetails) {
        String number = null;
        if (callDetails.getHandle() != null) {
            // tel:+919000000000 → extract number part
            number = callDetails.getHandle().getSchemeSpecificPart();
        }

        Log.d(TAG, "Screening incoming call from: " + number);

        CallResponse.Builder response = new CallResponse.Builder();

        if (number != null && !number.isEmpty() && BlockedNumberStore.isBlocked(this, number)) {
            // Reject the call silently — no ringing, no notification to the caller
            Log.d(TAG, "Blocked number detected — rejecting call silently: " + number);
            response.setRejectCall(true)
                    .setDisallowCall(true)
                    .setSkipCallLog(false)  // still appear in call log as blocked
                    .setSkipNotification(true); // no missed-call notification
        } else {
            // Allow call through normally
            response.setRejectCall(false)
                    .setDisallowCall(false)
                    .setSkipCallLog(false)
                    .setSkipNotification(false);
        }

        respondToCall(callDetails, response.build());
    }
}
