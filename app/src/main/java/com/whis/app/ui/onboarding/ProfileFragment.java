package com.whis.app.ui.onboarding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.whis.app.R;

/**
 * Step 3 — Basic profile: name, age group (large single-select), techLevel
 * (Basic/Advanced two-button choice). UI_PLAN.md §2.2 step 3.
 */
public class ProfileFragment extends Fragment {

    private String selectedAge = "";
    private String selectedTech = "Basic";

    private Button btnAge1830, btnAge3145, btnAge4660, btnAge60Plus;
    private Button btnTechBasic, btnTechAdvanced;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditText etName = view.findViewById(R.id.et_name);

        // Age group buttons
        btnAge1830 = view.findViewById(R.id.btn_age_18_30);
        btnAge3145 = view.findViewById(R.id.btn_age_31_45);
        btnAge4660 = view.findViewById(R.id.btn_age_46_60);
        btnAge60Plus = view.findViewById(R.id.btn_age_60_plus);
        Button[] ageButtons = { btnAge1830, btnAge3145, btnAge4660, btnAge60Plus };
        String[] ageValues = { "18-30", "31-45", "46-60", "60+" };

        for (int i = 0; i < ageButtons.length; i++) {
            final String val = ageValues[i];
            ageButtons[i].setOnClickListener(v -> {
                selectedAge = val;
                highlightSelected(ageButtons, (Button) v);
            });
        }

        // Tech level buttons
        btnTechBasic = view.findViewById(R.id.btn_tech_basic);
        btnTechAdvanced = view.findViewById(R.id.btn_tech_advanced);
        Button[] techButtons = { btnTechBasic, btnTechAdvanced };
        String[] techValues = { "Basic", "Advanced" };

        // Default highlight Basic
        highlightSelected(techButtons, btnTechBasic);

        for (int i = 0; i < techButtons.length; i++) {
            final String val = techValues[i];
            techButtons[i].setOnClickListener(v -> {
                selectedTech = val;
                highlightSelected(techButtons, (Button) v);
            });
        }

        // Next button
        view.findViewById(R.id.btn_profile_next).setOnClickListener(v -> {
            OnboardingActivity host = (OnboardingActivity) requireActivity();
            OnboardingData data = host.getData();

            data.name = etName.getText().toString().trim();
            data.ageGroup = selectedAge;
            data.techLevel = selectedTech;

            host.goToNext(new UpiSelectFragment());
        });
    }

    /** Visual feedback: selected button gets whis_trusted tint, others reset. */
    private void highlightSelected(Button[] group, Button selected) {
        for (Button btn : group) {
            if (btn == selected) {
                btn.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.whis_trusted));
                btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.whis_surface));
            } else {
                btn.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.whis_border));
                btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.whis_text_hi));
            }
        }
    }
}
