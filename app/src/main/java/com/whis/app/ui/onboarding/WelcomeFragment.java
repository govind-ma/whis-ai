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
 * Step 1 — Welcome screen. Logo, one-line promise, no data collected (UI_PLAN.md §2.2).
 */
public class WelcomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_welcome, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btn_get_started).setOnClickListener(v -> {
            OnboardingActivity host = (OnboardingActivity) requireActivity();
            host.goToNext(new LanguageFragment());
        });
    }
}
