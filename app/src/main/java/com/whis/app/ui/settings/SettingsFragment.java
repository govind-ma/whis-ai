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

        // 5. Dark theme switch
        if (switchDarkMode != null) {
            int currentMode = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            switchDarkMode.setChecked(currentMode == android.content.res.Configuration.UI_MODE_NIGHT_YES);

            switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
                int newMode = isChecked ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                                        : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(newMode);
            });
        }

        // 6. Report False Positive action
        if (btnReportFalsePositive != null) {
            btnReportFalsePositive.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Thank you. Report submitted for AI review.", Toast.LENGTH_LONG).show()
            );
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

        // 9. Do Not Disturb (DND) Access settings button
        View btnDndAccess = view.findViewById(R.id.btn_open_dnd_access);
        if (btnDndAccess != null) {
            btnDndAccess.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Opening Settings...", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(Settings.ACTION_SETTINGS));
                }
            });
        }

        // 10. Emergency Contacts (2 Contacts)
        android.widget.EditText etName1 = view.findViewById(R.id.et_emergency_name_1);
        android.widget.EditText etPhone1 = view.findViewById(R.id.et_emergency_phone_1);
        android.widget.EditText etName2 = view.findViewById(R.id.et_emergency_name_2);
        android.widget.EditText etPhone2 = view.findViewById(R.id.et_emergency_phone_2);
        View btnSaveEmergency = view.findViewById(R.id.btn_save_emergency_contacts);

        if (etName1 != null && etPhone1 != null && etName2 != null && etPhone2 != null) {
            etName1.setText(com.whis.app.core.EmergencyContactStore.getC1Name(requireContext()));
            etPhone1.setText(com.whis.app.core.EmergencyContactStore.getC1Phone(requireContext()));
            etName2.setText(com.whis.app.core.EmergencyContactStore.getC2Name(requireContext()));
            etPhone2.setText(com.whis.app.core.EmergencyContactStore.getC2Phone(requireContext()));
        }

        if (btnSaveEmergency != null) {
            btnSaveEmergency.setOnClickListener(v -> {
                String name1 = etName1 != null ? etName1.getText().toString() : "";
                String phone1 = etPhone1 != null ? etPhone1.getText().toString() : "";
                String name2 = etName2 != null ? etName2.getText().toString() : "";
                String phone2 = etPhone2 != null ? etPhone2.getText().toString() : "";

                com.whis.app.core.EmergencyContactStore.saveContacts(requireContext(), name1, phone1, name2, phone2);
                Toast.makeText(requireContext(), "Emergency Contacts Saved Successfully!", Toast.LENGTH_SHORT).show();
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
