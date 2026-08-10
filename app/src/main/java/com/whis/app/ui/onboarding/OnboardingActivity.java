package com.whis.app.ui.onboarding;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.whis.app.R;

/**
 * Hosts the onboarding fragment sequence (UI_PLAN.md §2.2).
 * <p>
 * Fragments call {@link #getData()} to read/write the shared {@link OnboardingData},
 * and {@link #goToNext(Fragment)} / {@link #goBack()} to navigate.
 */
public class OnboardingActivity extends AppCompatActivity {

    private OnboardingData data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        boolean isDarkMode = getSharedPreferences("whis_prefs", MODE_PRIVATE)
                .getBoolean("dark_mode", false);
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                isDarkMode ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                           : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
        );
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        data = new OnboardingData();

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.onboarding_container, new WelcomeFragment())
                    .commit();
        }
    }

    /** Shared mutable data model — fragments read and write fields as the user progresses. */
    @NonNull
    public OnboardingData getData() {
        return data;
    }

    /** Navigate forward to the next fragment, adding the current one to the back stack. */
    public void goToNext(@NonNull Fragment next) {
        getSupportFragmentManager()
                .beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .replace(R.id.onboarding_container, next)
                .addToBackStack(null)
                .commit();
    }

    /** Navigate back (pop the fragment back stack). */
    public void goBack() {
        getSupportFragmentManager().popBackStack();
    }
}
