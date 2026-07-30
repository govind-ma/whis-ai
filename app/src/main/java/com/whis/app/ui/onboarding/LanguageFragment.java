package com.whis.app.ui.onboarding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.whis.app.R;

/**
 * Step 2 — Language selection. Must come first since every subsequent screen
 * renders in the chosen language (UI_PLAN.md §2.2).
 */
public class LanguageFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_language, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View.OnClickListener langClick = v -> {
            OnboardingActivity host = (OnboardingActivity) requireActivity();
            OnboardingData data = host.getData();

            int id = v.getId();
            if (id == R.id.btn_english) {
                data.language = "English";
            } else if (id == R.id.btn_hindi) {
                data.language = "Hindi";
            } else if (id == R.id.btn_gujarati) {
                data.language = "Gujarati";
            }

            host.goToNext(new ProfileFragment());
        };

        view.findViewById(R.id.btn_english).setOnClickListener(langClick);
        view.findViewById(R.id.btn_hindi).setOnClickListener(langClick);
        view.findViewById(R.id.btn_gujarati).setOnClickListener(langClick);
    }
}
