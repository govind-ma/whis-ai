package com.whis.app.ui.onboarding;

import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

/**
 * Builds the correct system {@link Intent} for each {@link PermissionStep},
 * including manufacturer-specific deep links for battery/autostart (step 3).
 * <p>
 * UI_PLAN.md §2.2 step 7: each screen launches the correct system Intent.
 */
public final class PermissionIntentHelper {

    private PermissionIntentHelper() {
        // Static utility
    }

    /**
     * Build the system Intent for the given permission step.
     *
     * @param context the context
     * @param step    the permission step
     * @return an Intent to launch, or a fallback to app settings if the
     *         specific intent is unavailable on this device
     */
    public static Intent buildIntent(Context context, PermissionStep step) {
        switch (step) {
            case CALLER_ID_ROLE:
                return buildCallerIdIntent(context);
            case NOTIFICATION_ACCESS:
                return buildNotificationAccessIntent(context);
            case BATTERY_AUTOSTART:
                return buildBatteryAutostartIntent(context);
            case FULL_SCREEN_ALERT:
                return buildFullScreenAlertIntent(context);
            case DND_BYPASS:
                return buildDndBypassIntent(context);
            default:
                return buildAppDetailsIntent(context);
        }
    }

    // ── Step 1: Default Caller ID & Spam app role ────────────────────────

    private static Intent buildCallerIdIntent(Context context) {
        // API 29+ RoleManager for CALL_SCREENING role
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                android.app.role.RoleManager rm =
                        (android.app.role.RoleManager) context.getSystemService(Context.ROLE_SERVICE);
                if (rm != null) {
                    Intent roleIntent = rm.createRequestRoleIntent("android.app.role.CALL_SCREENING");
                    return roleIntent;
                }
            } catch (Exception ignored) {
                // Fall through to app settings
            }
        }
        // Pre-Q or role unavailable — open app details as fallback
        return buildAppDetailsIntent(context);
    }

    // ── Step 2: Notification Access ──────────────────────────────────────

    private static Intent buildNotificationAccessIntent(Context context) {
        Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            return intent;
        }
        return buildAppDetailsIntent(context);
    }

    // ── Step 3: Battery optimization + OEM autostart ─────────────────────

    private static Intent buildBatteryAutostartIntent(Context context) {
        String manufacturer = Build.MANUFACTURER.toLowerCase();

        // Manufacturer-specific autostart settings
        Intent oemIntent = null;
        if (manufacturer.contains("xiaomi") || manufacturer.contains("redmi")) {
            oemIntent = new Intent();
            oemIntent.setComponent(new ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"));
        } else if (manufacturer.contains("samsung")) {
            oemIntent = new Intent();
            oemIntent.setComponent(new ComponentName(
                    "com.samsung.android.lool",
                    "com.samsung.android.sm.battery.ui.BatteryActivity"));
        } else if (manufacturer.contains("oppo") || manufacturer.contains("realme")) {
            oemIntent = new Intent();
            oemIntent.setComponent(new ComponentName(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.startupapp.StartupAppListActivity"));
        } else if (manufacturer.contains("vivo")) {
            oemIntent = new Intent();
            oemIntent.setComponent(new ComponentName(
                    "com.iqoo.secure",
                    "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"));
        } else if (manufacturer.contains("huawei") || manufacturer.contains("honor")) {
            oemIntent = new Intent();
            oemIntent.setComponent(new ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"));
        } else if (manufacturer.contains("oneplus")) {
            oemIntent = new Intent();
            oemIntent.setComponent(new ComponentName(
                    "com.oneplus.security",
                    "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"));
        }

        // Try OEM intent first; fall back to standard battery optimization
        if (oemIntent != null && oemIntent.resolveActivity(context.getPackageManager()) != null) {
            return oemIntent;
        }

        // Standard Android battery optimization exemption
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent batteryIntent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            batteryIntent.setData(Uri.parse("package:" + context.getPackageName()));
            if (batteryIntent.resolveActivity(context.getPackageManager()) != null) {
                return batteryIntent;
            }
        }

        return buildAppDetailsIntent(context);
    }

    // ── Step 4: Full-screen alert Special App Access ─────────────────────

    private static Intent buildFullScreenAlertIntent(Context context) {
        // API 34+ has a dedicated setting for USE_FULL_SCREEN_INTENT
        if (Build.VERSION.SDK_INT >= 34) {
            try {
                Intent fsiIntent = new Intent(
                        "android.settings.MANAGE_APP_USE_FULL_SCREEN_INTENT");
                fsiIntent.setData(Uri.parse("package:" + context.getPackageName()));
                if (fsiIntent.resolveActivity(context.getPackageManager()) != null) {
                    return fsiIntent;
                }
            } catch (Exception ignored) {
                // Fall through
            }
        }
        // Pre-34: full-screen intent is granted by default for alarm/calling categories.
        // Open notification settings as the closest relevant screen.
        Intent notifIntent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
        notifIntent.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
        if (notifIntent.resolveActivity(context.getPackageManager()) != null) {
            return notifIntent;
        }
        return buildAppDetailsIntent(context);
    }

    // ── Step 5: DND bypass ───────────────────────────────────────────────

    private static Intent buildDndBypassIntent(Context context) {
        // Open notification policy access (allows the app to modify DND)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NotificationManager nm =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null && !nm.isNotificationPolicyAccessGranted()) {
                Intent dndIntent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                if (dndIntent.resolveActivity(context.getPackageManager()) != null) {
                    return dndIntent;
                }
            }
        }
        // If already granted or intent unavailable, open app notification settings
        Intent notifIntent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
        notifIntent.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
        if (notifIntent.resolveActivity(context.getPackageManager()) != null) {
            return notifIntent;
        }
        return buildAppDetailsIntent(context);
    }

    // ── Fallback: App details ────────────────────────────────────────────

    private static Intent buildAppDetailsIntent(Context context) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        return intent;
    }
}
