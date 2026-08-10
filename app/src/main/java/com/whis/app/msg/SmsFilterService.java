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

        // ── Map threat level & category to user-facing badge ──────────────────
        String badge;
        if (result.category == com.whis.app.msg.model.MsgCategory.EMERGENCY) {
            badge = "Emergency Alert";
        } else if ("SCAM".equalsIgnoreCase(result.threatLevel)) {
            badge = "Scam Detected";
        } else if ("SUSPICIOUS".equalsIgnoreCase(result.threatLevel)) {
            badge = "Suspicious";
        } else {
            badge = "Likely Safe";
        }

        // Clean reason text (without messy debug tags)
        if (result.reasonText == null || result.reasonText.isEmpty()) {
            result.reasonText = "Verified message check completed.";
        }

        // Persist SHA-256 hash of body for DPDP compliance
        saveHistoryEntry(sender, body, result, timestamp);

        // Show notification if EMERGENCY, SUSPICIOUS or HIGH_RISK
        if (result.category == com.whis.app.msg.model.MsgCategory.EMERGENCY) {
            showEmergencyNotification(sender, body);
        } else if (result.verdict == WhisVerdict.SUSPICIOUS || result.verdict == WhisVerdict.HIGH_RISK) {
            showWarningNotification(sender, result, badge);
        }

        // UPI SMS Pattern check — offline, instant, no network needed
        UpiSmsPatternEngine.UpiScanResult upiScan = UpiSmsPatternEngine.scan(sender, body);
        if (upiScan.isScamSuspected) {
            showUpiWarningNotification(sender, upiScan);
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

    private void showEmergencyNotification(String sender, String body) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "whis_emergency",
                    "Whis Urgent Emergency Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Urgent family emergency and hospital message alerts");
            nm.createNotificationChannel(channel);
        }

        String displaySender = (sender != null && !sender.isEmpty()) ? sender : "Unknown";
        String title = "🚑 URGENT: Possible Family Emergency!";
        String text = "Message from " + displaySender + " mentions emergency or hospital. Tap to view.";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "whis_emergency")
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setAutoCancel(true);

        nm.notify(NOTIFICATION_ID + 100, builder.build());
    }

    private void showWarningNotification(String sender, MsgDetectionResult result, String badge) {
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

        String displaySender = (sender != null && !sender.isEmpty()) ? sender : "Unknown";
        String title = "🚨 Scam SMS Alert: " + displaySender;
        String text = "Phishing or fake link detected. Do not share OTP or click unverified links.";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        nm.notify(NOTIFICATION_ID, builder.build());
    }

    private void showUpiWarningNotification(String sender, UpiSmsPatternEngine.UpiScanResult upiScan) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        String channelId = "whis_upi_warnings";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Whis UPI Scam Warnings",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Instant alerts for suspicious UPI and banking SMS messages");
            nm.createNotificationChannel(channel);
        }

        String displaySender = (sender != null && !sender.isEmpty()) ? sender : "Unknown";
        androidx.core.app.NotificationCompat.Builder builder =
                new androidx.core.app.NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(android.R.drawable.stat_sys_warning)
                        .setContentTitle("\u26a0\ufe0f UPI Alert: " + displaySender)
                        .setContentText(upiScan.warningHindi)
                        .setStyle(new androidx.core.app.NotificationCompat.BigTextStyle()
                                .bigText(upiScan.warningHindi + "\n\n" + upiScan.warningEnglish
                                        + "\n\nWhis AI Confidence: " + upiScan.confidenceScore + "%"))
                        .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MAX)
                        .setAutoCancel(true);

        nm.notify(NOTIFICATION_ID + 200, builder.build());
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
