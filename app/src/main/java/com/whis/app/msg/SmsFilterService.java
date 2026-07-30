package com.whis.app.msg;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;

import com.whis.app.core.WhisVerdict;
import com.whis.app.msg.engine.WeightedScoreEngine;
import com.whis.app.msg.model.MsgDetectionResult;
import com.whis.app.msg.storage.LocalMsgDatabase;
import com.whis.app.msg.storage.MsgHistoryEntry;

import java.security.MessageDigest;

/**
 * Foreground / Job service executing the SMS detection pipeline (MSG_PLAN.md Section 4.1 & 4.2).
 */
public class SmsFilterService extends JobIntentService {

    private static final int JOB_ID = 1001;
    private static final String CHANNEL_ID = "whis_msg_warnings";
    private static final int NOTIFICATION_ID = 3001;

    public static void enqueueWork(Context context, Intent intent) {
        enqueueWork(context, SmsFilterService.class, JOB_ID, intent);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        String sender = intent.getStringExtra("sender");
        String body = intent.getStringExtra("body");
        long timestamp = intent.getLongExtra("timestamp", System.currentTimeMillis());

        if (body == null || body.isEmpty()) return;

        // Run full 5-layer detection pipeline (Layer 5 now makes a real Gemini API call)
        MsgDetectionResult result = WeightedScoreEngine.analyze(this, sender, body);

        // ── Map threat level to user-facing badge ────────────────────────────
        // SAFE       → "Likely Safe"    (green badge)
        // SUSPICIOUS → "Suspicious"     (orange badge)
        // SCAM       → "Scam Detected"  (red badge)
        String badge;
        if ("SCAM".equalsIgnoreCase(result.threatLevel)) {
            badge = "Scam Detected";
        } else if ("SUSPICIOUS".equalsIgnoreCase(result.threatLevel)) {
            badge = "Suspicious";
        } else {
            badge = "Likely Safe";
        }
        result.reasonText = "[" + badge + "] " + (result.reasonText != null ? result.reasonText : "");

        // Persist SHA-256 hash of body for DPDP compliance
        saveHistoryEntry(sender, body, result, timestamp);

        // Show notification warning if SUSPICIOUS or HIGH_RISK
        if (result.verdict == WhisVerdict.SUSPICIOUS || result.verdict == WhisVerdict.HIGH_RISK) {
            showWarningNotification(result, badge);
        }
    }

    private void saveHistoryEntry(String sender, String body, MsgDetectionResult result, long timestamp) {
        try {
            LocalMsgDatabase db = LocalMsgDatabase.getInstance(this);
            String bodyHash = sha256(body);
            MsgHistoryEntry entry = new MsgHistoryEntry(
                    timestamp,
                    sender != null ? sender : "",
                    bodyHash,
                    result.category.name(),
                    result.threatLevel,
                    result.riskScore / 100.0f,
                    result.reasonText,
                    result.layersUsed,
                    null
            );
            db.msgHistoryDao().insert(entry);
        } catch (Exception e) {
            // Defensive
        }
    }

    private void showWarningNotification(MsgDetectionResult result, String badge) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Whis SMS Scam Warnings",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("High-priority warnings for scam and phishing SMS messages");
            nm.createNotificationChannel(channel);
        }

        String title = "⚠ " + badge + ": " + (result.sender != null ? result.sender : "Unknown");

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentTitle(title)
                .setContentText(result.reasonText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        nm.notify(NOTIFICATION_ID, builder.build());
    }

    private static String sha256(String base) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(base.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception ex) {
            return String.valueOf(base.hashCode());
        }
    }
}
