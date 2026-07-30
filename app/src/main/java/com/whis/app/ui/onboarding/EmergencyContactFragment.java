package com.whis.app.ui.onboarding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.whis.app.R;

/**
 * Step 5 — Emergency contact: name + phone number (UI_PLAN.md §2.2 step 5).
 * Feeds AI Agent's SMS alert feature directly.
 */
public class EmergencyContactFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_emergency_contact, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditText etName = view.findViewById(R.id.et_contact_name);
        EditText etPhone = view.findViewById(R.id.et_contact_phone);

        view.findViewById(R.id.btn_emergency_next).setOnClickListener(v -> {
            OnboardingActivity host = (OnboardingActivity) requireActivity();
            OnboardingData data = host.getData();

            data.emergencyContactName = etName.getText().toString().trim();
            data.emergencyContactPhone = etPhone.getText().toString().trim();

            host.goToNext(new ConsentFragment());
        });
    }
}
