package com.whis.app.call;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * BroadcastReceiver for incoming call detection via {@code READ_PHONE_STATE}.
 * <p>
 * On {@link TelephonyManager#CALL_STATE_RINGING}, extracts the incoming number,
 * resolves a contact display name (if saved), and delegates analysis to
 * {@link CallFilterService} on a background thread.
 * <p>
 * Registered in AndroidManifest.xml with action
 * {@code android.intent.action.PHONE_STATE}.
 */
public class PhoneStateReceiver extends BroadcastReceiver {

    private static final String TAG = "PhoneStateReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        if (!TelephonyManager.EXTRA_STATE_RINGING.equals(state)) return;

        // Extract incoming number — may be null if permission not yet granted
        String phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            Log.w(TAG, "Incoming call with null/empty number — skipping AI analysis");
            return;
        }

        // ── BLOCK CHECK: Skip AI analysis for blocked numbers ─────────────────
        // Actual call rejection is handled by:
        //   • WhisCallScreeningService (Android 10+, requires CALL_SCREENING role)
        //   • BlockedNumberContract sync in BlockedNumberStore (system dialer rejects pre-Android 10)
        // We still return early here to avoid unnecessary Gemini API calls for blocked numbers.
        if (BlockedNumberStore.isBlocked(context, phoneNumber)) {
            Log.d(TAG, "Blocked number calling: " + phoneNumber + " — skipping AI analysis (blocking handled by CallScreeningService/system).");
            return;
        }

        // Resolve contact name (requires READ_CONTACTS permission)
        String contactName = resolveContactName(context, phoneNumber);

        Log.d(TAG, "Incoming call: " + phoneNumber + " → " + contactName);

        // Delegate analysis to background JobIntentService
        CallFilterService.enqueueWork(context, phoneNumber, contactName);
    }

    // ── Contact resolution ────────────────────────────────────────────────────

    /**
     * Look up the display name for a phone number in the device contacts.
     *
     * @return display name, or "Unknown" if not found / permission denied
     */
    private static String resolveContactName(Context context, String phoneNumber) {
        try {
            android.net.Uri uri = android.net.Uri.withAppendedPath(
                    ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                    android.net.Uri.encode(phoneNumber));

            Cursor cursor = context.getContentResolver().query(
                    uri,
                    new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME},
                    null, null, null);

            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        int idx = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);
                        if (idx >= 0) {
                            String name = cursor.getString(idx);
                            if (name != null && !name.isEmpty()) return name;
                        }
                    }
                } finally {
                    cursor.close();
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Contact lookup failed: " + e.getMessage());
        }
        return "Unknown";
    }
}
