package com.whis.app.call;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;

import com.whis.app.agent.RedAlertActivity;

/**
 * Background job service for AI-powered incoming call classification.
 * <p>
 * Pipeline:
 * <ol>
 *   <li>Receives phoneNumber + contactName from {@link PhoneStateReceiver}</li>
 *   <li>Calls {@link CallGeminiAnalyzer#analyze(Context, String, String)} (synchronous, 3 s timeout)</li>
 *   <li>Derives user-facing badge from risk_level</li>
 *   <li>Saves result to {@link CallHistoryStore}</li>
 *   <li>If SCAM / DIGITAL_ARREST / IMPERSONATION → launches {@link RedAlertActivity}</li>
 *   <li>Otherwise shows an appropriate notification</li>
 *   <li>On ANY failure → falls back silently; never crashes</li>
 * </ol>
 */
public class CallFilterService extends JobIntentService {

    private static final String TAG             = "CallFilterService";
    private static final int    JOB_ID          = 1002;
    private static final String CHANNEL_ID      = "whis_call_warnings";
    private static final int    NOTIFICATION_ID = 4001;

    public static final String EXTRA_PHONE_NUMBER  = "phone_number";
    public static final String EXTRA_CONTACT_NAME  = "contact_name";

    // ── Enqueue helper ────────────────────────────────────────────────────────

    public static void enqueueWork(Context context, String phoneNumber, String contactName) {
        Intent intent = new Intent(context, CallFilterService.class);
        intent.putExtra(EXTRA_PHONE_NUMBER, phoneNumber);
        intent.putExtra(EXTRA_CONTACT_NAME, contactName);
        enqueueWork(context, CallFilterService.class, JOB_ID, intent);
    }

    // ── Work ──────────────────────────────────────────────────────────────────

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        String phoneNumber  = intent.getStringExtra(EXTRA_PHONE_NUMBER);
        String contactName  = intent.getStringExtra(EXTRA_CONTACT_NAME);
        if (contactName == null) contactName = "Unknown";

        Log.d(TAG, "Analysing call from: " + phoneNumber + " (" + contactName + ")");

        // Known Contact Safeguard: If caller is in address book (not "Unknown"), mark as Trusted instantly
        if (contactName != null && !contactName.equalsIgnoreCase("Unknown") && !contactName.trim().isEmpty()) {
            Log.d(TAG, "Known contact [" + contactName + "] calling — marking as Trusted.");
            CallHistoryStore.CallEntry entry = new CallHistoryStore.CallEntry(
                    phoneNumber,
                    contactName,
                    "SAFE",
                    "SAVED_CONTACT",
                    "Saved contact in phone address book",
                    "Trusted",
                    System.currentTimeMillis()
            );
            CallHistoryStore.save(this, entry);
            return;
        }

        // ── Step 1: AI analysis (synchronous) ────────────
        CallGeminiAnalyzer.CallGeminiResult aiResult =
                CallGeminiAnalyzer.analyze(this, phoneNumber, contactName);

        // ── Step 2: Derive user-facing badge ─────────────────────────────────
        // SAFE       → "Trusted"        (green)
        // SUSPICIOUS → "Suspicious"     (orange)
        // SCAM       → "Scam Detected"  (red)
        String badge;
        switch (aiResult.riskLevel) {
            case "SCAM":
                badge = "Scam Detected";
                break;
            case "SAFE":
                badge = "Trusted";
                break;
            default:
                badge = "Suspicious";
                break;
        }

        // ── Step 3: Persist to CallHistoryStore ───────────────────────────────
        CallHistoryStore.CallEntry entry = new CallHistoryStore.CallEntry(
                phoneNumber,
                contactName,
                aiResult.riskLevel,
                aiResult.category,
                aiResult.reason,
                badge,
                System.currentTimeMillis()
        );
        CallHistoryStore.save(this, entry);

        // ── Step 4: Act on verdict ────────────────────────────────────────────
        boolean isScam          = "SCAM".equals(aiResult.riskLevel);
        boolean isDigitalArrest = "DIGITAL_ARREST".equals(aiResult.category);
        boolean isImpersonation = "IMPERSONATION".equals(aiResult.category);

        if (isScam || isDigitalArrest || isImpersonation) {
            launchRedAlert(phoneNumber, aiResult.category);
        } else if ("SUSPICIOUS".equals(aiResult.riskLevel)) {
            showWarningNotification(phoneNumber, contactName, badge, aiResult.reason);
        }
        // SAFE: no action needed
    }

    // ── RedAlert launcher ─────────────────────────────────────────────────────

    private void launchRedAlert(String phoneNumber, String category) {
        try {
            Intent redAlertIntent = new Intent(this, RedAlertActivity.class);
            redAlertIntent.putExtra("threat_type", category);
            redAlertIntent.putExtra("phone_number", phoneNumber);
            redAlertIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    redAlertIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    NotificationChannel channel = new NotificationChannel(
                            CHANNEL_ID,
                            "Whis Emergency Scam Alerts",
                            NotificationManager.IMPORTANCE_HIGH
                    );
                    channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
                    nm.createNotificationChannel(channel);
                }

                NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.stat_sys_warning)
                        .setContentTitle("🚨 CRITICAL SCAM ALERT")
                        .setContentText("Emergency threat detected: " + phoneNumber)
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setCategory(NotificationCompat.CATEGORY_CALL)
                        .setFullScreenIntent(pendingIntent, true)
                        .setAutoCancel(true);

                nm.notify(NOTIFICATION_ID, builder.build());
            }

            // Direct start activity fallback
            startActivity(redAlertIntent);
            Log.d(TAG, "RedAlertActivity launched for: " + phoneNumber + " cat=" + category);
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch RedAlertActivity", e);
            showWarningNotification(phoneNumber, "Unknown", "Scam Detected",
                    "Scam call detected. Do not answer.");
        }
    }

    // ── Notification ──────────────────────────────────────────────────────────

    private void showWarningNotification(String phoneNumber, String contactName,
                                         String badge, String reason) {
        try {
            // Do not show warning notification for known address book contacts
            if (contactName != null && !contactName.equalsIgnoreCase("Unknown") && !contactName.trim().isEmpty()) {
                return;
            }

            NotificationManager nm =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "Whis Call Warnings",
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("AI-powered scam call warnings");
                nm.createNotificationChannel(channel);
            }

            String displayNum = (phoneNumber != null && !phoneNumber.isEmpty()) ? phoneNumber : "Unknown";
            String title = "\u26a0\ufe0f Suspicious Call — सावधान!";
            String text = displayNum + "\nअनजान caller है। कोई पैसा या OTP न दें।";

            if ("Scam Detected".equalsIgnoreCase(badge)) {
                title = "\uD83D\uDEA8 SCAM CALL — तुरंत फोन काटें!";
                text = displayNum + "\nयह Digital Arrest या Impersonation scam हो सकता है।\nपैसे मत भेजें — 1930 पर call करें।";
            }

            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(this, CHANNEL_ID)
                            .setSmallIcon(android.R.drawable.stat_sys_warning)
                            .setContentTitle(title)
                            .setContentText(text)
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setAutoCancel(true);

            nm.notify(NOTIFICATION_ID, builder.build());
        } catch (Exception e) {
            Log.e(TAG, "Failed to show call warning notification", e);
        }
    }
}
