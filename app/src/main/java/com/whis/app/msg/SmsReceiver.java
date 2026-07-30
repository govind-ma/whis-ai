package com.whis.app.msg;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;

/**
 * BroadcastReceiver entry point for incoming SMS (MSG_PLAN.md Section 4.1 Step 9).
 * <p>
 * Listens to {@link Telephony.Sms#SMS_RECEIVED_ACTION}.
 */
public class SmsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
            SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
            if (messages == null || messages.length == 0) return;

            StringBuilder fullBody = new StringBuilder();
            String sender = messages[0].getDisplayOriginatingAddress();
            long timestamp = messages[0].getTimestampMillis();

            for (SmsMessage msg : messages) {
                if (msg.getMessageBody() != null) {
                    fullBody.append(msg.getMessageBody());
                }
            }

            Intent serviceIntent = new Intent(context, SmsFilterService.class);
            serviceIntent.putExtra("sender", sender);
            serviceIntent.putExtra("body", fullBody.toString());
            serviceIntent.putExtra("timestamp", timestamp);

            SmsFilterService.enqueueWork(context, serviceIntent);
        }
    }
}
