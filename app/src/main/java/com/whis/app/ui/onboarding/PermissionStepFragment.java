package com.whis.app.ui.onboarding;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.whis.app.R;
import com.whis.app.ui.components.PrimaryButton;

/**
 * Reusable full-screen fragment for one permission wizard step (UI_PLAN.md §2.2 step 7).
 * <p>
 * Configured via {@link #newInstance(PermissionStep)}. Shows a plain-language
 * explanation of why the permission is needed, then launches the correct system
 * Intent via {@link PermissionIntentHelper}. Progression is never blocked — the
 * user can always skip.
 */
public class PermissionStepFragment extends Fragment {

    private static final String ARG_STEP_ORDINAL = "step_ordinal";

    private PermissionStep step;
    private TextView tvStatus;
    private boolean returnedFromSettings = false;

    private final ActivityResultLauncher<Intent> settingsLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        returnedFromSettings = true;
                        updateStatusAfterReturn();
                    }
            );

    /** Create a new instance for the given step. */
    public static PermissionStepFragment newInstance(@NonNull PermissionStep step) {
        PermissionStepFragment frag = new PermissionStepFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_STEP_ORDINAL, step.ordinal());
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int ordinal = requireArguments().getInt(ARG_STEP_ORDINAL, 0);
        step = PermissionStep.values()[ordinal];
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_permission_step, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Step indicator
        TextView tvIndicator = view.findViewById(R.id.tv_step_indicator);
        tvIndicator.setText("Step " + step.stepNumber + " of " + PermissionStep.TOTAL_STEPS);

        // Title
        TextView tvTitle = view.findViewById(R.id.tv_perm_title);
        tvTitle.setText(step.title);

        // 3D Permission Stack Setup
        com.whis.app.ui.components.PermissionCardStackView cardStack = view.findViewById(R.id.perm_card_stack);
        if (cardStack != null) {
            cardStack.setActiveIndex(step.ordinal());
            cardStack.setOnStepSelectedListener((selectedStep, index) -> {
                if (selectedStep != step) {
                    OnboardingActivity host = (OnboardingActivity) getActivity();
                    if (host != null) {
                        host.goToNext(PermissionStepFragment.newInstance(selectedStep));
                    }
                }
            });
        }

        // Explanation
        TextView tvExplanation = view.findViewById(R.id.tv_perm_explanation);
        tvExplanation.setText(step.explanation);

        // Manufacturer hint (only for BATTERY_AUTOSTART)
        TextView tvManufacturer = view.findViewById(R.id.tv_manufacturer_hint);
        if (step == PermissionStep.BATTERY_AUTOSTART) {
            String brand = Build.MANUFACTURER;
            tvManufacturer.setText("Detected phone brand: " + brand);
            tvManufacturer.setVisibility(View.VISIBLE);
        }

        // Status text (updated after returning from settings)
        tvStatus = view.findViewById(R.id.tv_perm_status);

        // Grant button
        PrimaryButton btnGrant = view.findViewById(R.id.btn_grant_permission);
        btnGrant.setText(step.buttonLabel);
        btnGrant.setOnClickListener(v -> {
            try {
                Intent intent = PermissionIntentHelper.buildIntent(requireContext(), step);
                settingsLauncher.launch(intent);
            } catch (Exception e) {
                // If the intent can't be resolved, show a helpful message
                tvStatus.setText("Could not open settings. You can enable this later in Settings.");
                tvStatus.setTextColor(requireContext().getResources()
                        .getColor(R.color.whis_suspicious, requireContext().getTheme()));
                tvStatus.setVisibility(View.VISIBLE);
            }
        });

        // Skip button — never blocks progression
        view.findViewById(R.id.btn_skip).setOnClickListener(v -> {
            markSkipped();
            goToNextStep();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (returnedFromSettings) {
            updateStatusAfterReturn();
        }
    }

    private void updateStatusAfterReturn() {
        if (tvStatus == null) return;

        // Show a generic "done" confirmation and enable advancing.
        // Actual permission checks happen on the status screen, not here —
        // we don't block progression.
        tvStatus.setText("✓ Setting opened. Tap Next to continue, or grant the permission if you haven't yet.");
        tvStatus.setTextColor(requireContext().getResources()
                .getColor(R.color.whis_trusted, requireContext().getTheme()));
        tvStatus.setVisibility(View.VISIBLE);

        // Swap the grant button to a "Next" button
        PrimaryButton btnGrant = requireView().findViewById(R.id.btn_grant_permission);
        btnGrant.setText("Next");
        btnGrant.setOnClickListener(v -> {
            markGranted();
            goToNextStep();
        });
    }

    private void goToNextStep() {
        OnboardingActivity host = (OnboardingActivity) requireActivity();
        PermissionStep next = step.next();

        if (next != null) {
            host.goToNext(PermissionStepFragment.newInstance(next));
        } else {
            // All 5 steps complete — onboarding is done
            onOnboardingComplete();
        }
    }

    private void onOnboardingComplete() {
        OnboardingActivity host = (OnboardingActivity) requireActivity();
        OnboardingData data = host.getData();

        // 1. Persist core consent via WhisConsentManager
        com.whis.app.core.WhisConsentManager.setConsentGiven(requireContext(), true);

        // 2. Serialize user profile into whis_user_profile SharedPreferences JSON for AI Agent
        try {
            org.json.JSONObject profileJson = new org.json.JSONObject();
            profileJson.put("name", data.name != null && !data.name.isEmpty() ? data.name : "User");
            profileJson.put("ageGroup", data.ageGroup != null && !data.ageGroup.isEmpty() ? data.ageGroup : "26-40");
            profileJson.put("occupation", "User");
            profileJson.put("language", data.language != null ? data.language : "English");
            profileJson.put("techLevel", data.techLevel != null ? data.techLevel : "Basic");
            profileJson.put("primaryUpi", data.primaryUpi != null ? data.primaryUpi : "GPay");
            profileJson.put("bankName", "Bank");
            if (data.emergencyContactName != null && !data.emergencyContactName.isEmpty()) {
                profileJson.put("emergencyContact", data.emergencyContactName + " (" + data.emergencyContactPhone + ")");
            }

            requireContext().getSharedPreferences("whis_prefs", android.content.Context.MODE_PRIVATE)
                    .edit()
                    .putString("whis_user_profile", profileJson.toString())
                    .putBoolean("onboarding_complete", true)
                    .apply();
        } catch (Exception e) {
            android.util.Log.e("PermissionWizard", "Failed to save whis_user_profile", e);
        }

        android.util.Log.d("PermissionWizard", "Onboarding fully complete. "
                + "Permissions granted: " + data.permissionsGranted
                + ", skipped: " + data.permissionsSkipped);

        android.widget.Toast.makeText(requireContext(),
                "Protection activated!", android.widget.Toast.LENGTH_SHORT).show();

        // Launch main app Activity
        Intent mainIntent = new Intent(requireContext(), com.whis.app.WhisMainActivity.class);
        startActivity(mainIntent);
        requireActivity().finish();
    }

    private void markGranted() {
        OnboardingActivity host = (OnboardingActivity) requireActivity();
        host.getData().permissionsGranted.add(step.name());
    }

    private void markSkipped() {
        OnboardingActivity host = (OnboardingActivity) requireActivity();
        host.getData().permissionsSkipped.add(step.name());
    }
}
