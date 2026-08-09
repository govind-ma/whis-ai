package com.whis.app.msg;

import android.content.Intent;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

/**
 * Whis Notification Listener Service.
 * Listens to incoming push notifications (e.g. SMS apps, Banking apps, Messaging)
 * to screen for phishing links, fake UPI collect requests, and scam alerts.
 */
public class WhisNotificationListenerService extends NotificationListenerService {

    private static final String TAG = "WhisNotifService";

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        Log.d(TAG, "Whis Notification Listener Connected!");
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || sbn.getNotification() == null) return;

        // Skip Whis's own notifications to prevent loops
        String packageName = sbn.getPackageName();
        if (getPackageName().equals(packageName)) return;

        // Skip System Utilities (smartcapture, screenshot, systemui, dialer, settings, etc.)
        String pkgLower = packageName.toLowerCase();
        if (pkgLower.startsWith("android")
                || pkgLower.contains("systemui")
                || pkgLower.contains("smartcapture")
                || pkgLower.contains("screenshot")
                || pkgLower.contains("settings")
                || pkgLower.contains("dialer")
                || pkgLower.contains("telecom")
                || pkgLower.contains("incallui")
                || pkgLower.contains("phone")
                || (pkgLower.startsWith("com.samsung.android.") && !pkgLower.contains("messaging"))) {
            return;
        }

        Bundle extras = sbn.getNotification().extras;
        if (extras == null) return;

        CharSequence titleCS = extras.getCharSequence("android.title");
        CharSequence textCS = extras.getCharSequence("android.text");

        StringBuilder bodyBuilder = new StringBuilder();
        if (textCS != null && !textCS.toString().trim().isEmpty()) {
            bodyBuilder.append(textCS.toString().trim());
        }

        // Parse multi-line notifications (WhatsApp group chats, aggregated lines)
        CharSequence[] textLines = extras.getCharSequenceArray("android.textLines");
        if (textLines != null && textLines.length > 0) {
            for (CharSequence line : textLines) {
                if (line != null && !line.toString().trim().isEmpty()) {
                    if (bodyBuilder.length() > 0) bodyBuilder.append(" ");
                    bodyBuilder.append(line.toString().trim());
                }
            }
        }

        String title = titleCS != null ? titleCS.toString() : "";
        String text = bodyBuilder.toString();

        if (text.trim().isEmpty()) return;

        Log.d(TAG, "Notification received from [" + packageName + "]: " + title + " - " + text);

        // Process message through SmsFilterService pipeline
        Intent intent = new Intent();
        intent.putExtra("sender", packageName);
        intent.putExtra("body", title + " " + text);
        intent.putExtra("timestamp", sbn.getPostTime());

        SmsFilterService.enqueueWork(this, intent);
    }
}
