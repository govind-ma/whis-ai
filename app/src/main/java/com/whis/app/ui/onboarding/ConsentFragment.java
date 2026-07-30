package com.whis.app.ui.onboarding;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.whis.app.R;

/**
 * Step 6 — Single consent screen with exact draft copy from UI_PLAN.md §2.3.
 * <p>
 * Three labeled sections under one visual roof. Only section 2 (community
 * reporting) has an opt-in/out toggle — sections 1 and 3 are core to the
 * app functioning, explained but not separately gated.
 */
public class ConsentFragment extends Fragment {

    private static final String TAG = "ConsentFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_consent, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Switch communitySwitch = view.findViewById(R.id.switch_community);

        view.findViewById(R.id.btn_consent_agree).setOnClickListener(v -> {
            OnboardingActivity host = (OnboardingActivity) requireActivity();
            OnboardingData data = host.getData();

            data.communityReportingOptIn = communitySwitch.isChecked();

            Log.d(TAG, "Consent accepted:"
                    + " lang=" + data.language
                    + " name=" + data.name
                    + " age=" + data.ageGroup
                    + " tech=" + data.techLevel
                    + " upi=" + data.primaryUpi
                    + " emergencyName=" + data.emergencyContactName
                    + " emergencyPhone=" + data.emergencyContactPhone
                    + " community=" + data.communityReportingOptIn);

            // Advance to permission wizard step 1 (§2.2 step 7)
            host.goToNext(PermissionStepFragment.newInstance(PermissionStep.CALLER_ID_ROLE));
        });
    }
}
