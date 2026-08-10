package com.whis.app.ui.settings;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.whis.app.BuildConfig;
import com.whis.app.R;
import com.whis.app.agent.context.UserProfileContext;
import com.whis.app.agent.model.UserProfile;

/**
 * SettingsFragment — UI_PLAN.md §3.1.
 * <p>
 * Controls:
 * - Permission status for SMS and Calls
 * - User Profile display
 * - Notifications toggle
 * - App Version number
 * - "Report False Positive" option
 * - Dark theme switch
 * - Accessibility / System display settings link
 */
public class SettingsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        view.setAlpha(0f);
        view.animate().alpha(1f).setDuration(200).start();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvName = view.findViewById(R.id.tv_settings_user_name);
        TextView tvEmergency = view.findViewById(R.id.tv_settings_emergency_contact);
        TextView tvPermSms = view.findViewById(R.id.tv_perm_sms_status);
        TextView tvPermCalls = view.findViewById(R.id.tv_perm_calls_status);
        TextView tvAppVersion = view.findViewById(R.id.tv_app_version);
        Switch switchVoice = view.findViewById(R.id.switch_voice_alerts);
        Switch switchDarkMode = view.findViewById(R.id.switch_dark_mode);
        Button btnReportFalsePositive = view.findViewById(R.id.btn_report_false_positive);

        // 1. Account section display with real user profile
        UserProfile profile = UserProfileContext.getProfile(requireContext());
        tvName.setText("User Profile: " + (profile.name != null ? profile.name : "User"));
        if (tvAppVersion != null) tvAppVersion.setText("Whis v2.2 (Build 3)");
        tvEmergency.setText("Emergency Contact: " + (profile.emergencyContact != null ? profile.emergencyContact : "Not set"));

        // 2. Dynamic permission status checks
        boolean smsGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECEIVE_SMS)
                == PackageManager.PERMISSION_GRANTED;
        boolean callsGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ANSWER_PHONE_CALLS)
                == PackageManager.PERMISSION_GRANTED;

        tvPermSms.setText("SMS Protection: " + (smsGranted ? "Active (Granted)" : "Action Needed (Denied)"));
        tvPermCalls.setText("Call Screening & Blocking: " + (callsGranted ? "Active (Granted)" : "Action Needed (Denied)"));

        Button btnGrantCall = view.findViewById(R.id.btn_grant_call_perm);
        if (btnGrantCall != null) {
            if (!callsGranted) {
                btnGrantCall.setVisibility(View.VISIBLE);
                btnGrantCall.setOnClickListener(v -> requestPermissions(new String[]{
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.ANSWER_PHONE_CALLS,
                        Manifest.permission.READ_CALL_LOG
                }, 2001));
            } else {
                btnGrantCall.setVisibility(View.GONE);
            }
        }

        // WhatsApp & Push Screening Toggle Switch
        Switch switchWhatsApp = view.findViewById(R.id.switch_whatsapp_screening);
        if (switchWhatsApp != null) {
            boolean notifAccessGranted = isNotificationAccessGranted();
            switchWhatsApp.setChecked(notifAccessGranted);

            switchWhatsApp.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked && !isNotificationAccessGranted()) {
                    Toast.makeText(requireContext(), "Enable Notification Access for Whis", Toast.LENGTH_LONG).show();
                    try {
                        startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
                    } catch (Exception e) {
                        startActivity(new Intent(Settings.ACTION_SETTINGS));
                    }
                } else {
                    String status = isChecked ? "Enabled" : "Disabled";
                    Toast.makeText(requireContext(), "WhatsApp Screening " + status, Toast.LENGTH_SHORT).show();
                }
            });
        }

        // 5. Dark theme switch — default is Light Mode
        if (switchDarkMode != null) {
            boolean isDarkMode = requireContext().getSharedPreferences("whis_prefs", android.content.Context.MODE_PRIVATE)
                    .getBoolean("dark_mode", false);
            switchDarkMode.setChecked(isDarkMode);

            switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
                requireContext().getSharedPreferences("whis_prefs", android.content.Context.MODE_PRIVATE)
                        .edit().putBoolean("dark_mode", isChecked).apply();
                int newMode = isChecked ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                                        : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(newMode);
            });
        }

        // 6. Report False Positive action — opens email with pre-filled report
        if (btnReportFalsePositive != null) {
            btnReportFalsePositive.setOnClickListener(v -> {
                try {
                    Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                    emailIntent.setData(android.net.Uri.parse("mailto:"));
                    emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"whisai.support@gmail.com"});
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Whis AI — False Positive Report");
                    emailIntent.putExtra(Intent.EXTRA_TEXT,
                            "Hi Whis AI Team,\n\n"
                            + "I want to report a false positive detection in the app.\n\n"
                            + "--- Details ---\n"
                            + "App Version: Whis v2.2 (Build 3)\n"
                            + "Device: " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL + "\n"
                            + "Android: " + android.os.Build.VERSION.RELEASE + "\n\n"
                            + "Describe what was incorrectly flagged:\n"
                            + "[Please describe the number/message that was wrongly detected]\n\n"
                            + "Thank you.");
                    startActivity(Intent.createChooser(emailIntent, "Send Report via Email"));
                } catch (Exception e) {
                    Toast.makeText(requireContext(),
                            "No email app found. Please email: whisai.support@gmail.com",
                            Toast.LENGTH_LONG).show();
                }
            });
        }

        // 7. System display & font settings button
        view.findViewById(R.id.btn_open_display_settings).setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Settings.ACTION_DISPLAY_SETTINGS);
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(requireContext(),
                        "Please open your phone's Display settings to adjust text size.",
                        Toast.LENGTH_LONG).show();
            }
        });

        // 8. Notification Access settings button
        View btnNotifAccess = view.findViewById(R.id.btn_open_notification_access);
        if (btnNotifAccess != null) {
            btnNotifAccess.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Opening Settings...", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(Settings.ACTION_SETTINGS));
                }
            });
        }

        // 9. DND Access settings button
        View btnDndAccess = view.findViewById(R.id.btn_open_dnd_access);
        if (btnDndAccess != null) {
            btnDndAccess.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(requireContext(),
                            "Please open your phone's Settings to allow Do Not Disturb Access.",
                            Toast.LENGTH_LONG).show();
                }
            });
        }

        // 10. Review 3D Protection Permission Criteria Stack
        View btnReviewStack = view.findViewById(R.id.btn_review_permission_stack);
        if (btnReviewStack != null) {
            btnReviewStack.setOnClickListener(v -> {
                Intent onboardingIntent = new Intent(requireContext(), com.whis.app.ui.onboarding.OnboardingActivity.class);
                startActivity(onboardingIntent);
            });
        }

        // 11. Emergency Contacts (2 Contacts)
        android.widget.EditText etName1 = view.findViewById(R.id.et_emergency_name_1);
        android.widget.EditText etPhone1 = view.findViewById(R.id.et_emergency_phone_1);
        android.widget.EditText etName2 = view.findViewById(R.id.et_emergency_name_2);
        android.widget.EditText etPhone2 = view.findViewById(R.id.et_emergency_phone_2);
        View btnSaveEmergency = view.findViewById(R.id.btn_save_emergency_contacts);

        if (etName1 != null && etPhone1 != null && etName2 != null && etPhone2 != null) {
            etName1.setText(com.whis.app.core.EmergencyContactStore.getC1Name(requireContext()));
            etPhone1.setText(com.whis.app.core.EmergencyContactStore.getC1CleanDigits(requireContext()));
            etName2.setText(com.whis.app.core.EmergencyContactStore.getC2Name(requireContext()));
            etPhone2.setText(com.whis.app.core.EmergencyContactStore.getC2CleanDigits(requireContext()));
        }

        if (btnSaveEmergency != null) {
            btnSaveEmergency.setOnClickListener(v -> {
                String name1 = etName1 != null ? etName1.getText().toString().trim() : "";
                String phone1 = etPhone1 != null ? etPhone1.getText().toString().trim() : "";
                String name2 = etName2 != null ? etName2.getText().toString().trim() : "";
                String phone2 = etPhone2 != null ? etPhone2.getText().toString().trim() : "";

                // Validate phone numbers — must be a valid 10-digit number
                if (!phone1.isEmpty() && !com.whis.app.core.EmergencyContactStore.isValid10DigitPhone(phone1)) {
                    if (etPhone1 != null) etPhone1.setError("Enter a valid 10-digit phone number");
                    Toast.makeText(requireContext(), "⚠️ Contact 1 must be a valid 10-digit number", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!phone2.isEmpty() && !com.whis.app.core.EmergencyContactStore.isValid10DigitPhone(phone2)) {
                    if (etPhone2 != null) etPhone2.setError("Enter a valid 10-digit phone number");
                    Toast.makeText(requireContext(), "⚠️ Contact 2 must be a valid 10-digit number", Toast.LENGTH_SHORT).show();
                    return;
                }

                com.whis.app.core.EmergencyContactStore.saveContacts(requireContext(), name1, phone1, name2, phone2);

                // Update text fields with clean 10-digit numbers
                if (etPhone1 != null && !phone1.isEmpty()) etPhone1.setText(com.whis.app.core.EmergencyContactStore.getC1CleanDigits(requireContext()));
                if (etPhone2 != null && !phone2.isEmpty()) etPhone2.setText(com.whis.app.core.EmergencyContactStore.getC2CleanDigits(requireContext()));

                Toast.makeText(requireContext(), "✅ Emergency Contacts Saved (" + com.whis.app.core.EmergencyContactStore.getC1Phone(requireContext()) + ")!", Toast.LENGTH_LONG).show();
            });
        }
    }

    private boolean isNotificationAccessGranted() {
        try {
            java.util.Set<String> enabledListeners = androidx.core.app.NotificationManagerCompat
                    .getEnabledListenerPackages(requireContext());
            return enabledListeners.contains(requireContext().getPackageName());
        } catch (Exception e) {
            return false;
        }
    }
}
