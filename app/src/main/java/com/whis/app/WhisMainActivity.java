package com.whis.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

/**
 * Main activity hosting the 5-tab BottomNavigationView and NavHostFragment (UI_PLAN.md §2.4 / §3.1).
 * <p>
 * Top-level destinations: Home, Calls, Messages, Learn, Settings.
 * Max 2 levels deep from any tab per UI_PLAN.md §2.4.
 * <p>
 * Glass card rendering ({@link com.whis.app.ui.components.GlassCardView}) uses
 * {@code RenderEffect} at the individual View level on API 31+ — no window-level
 * blur setup is required or should be done here.
 */
public class WhisMainActivity extends AppCompatActivity {

    private static final String TAG = "WhisMainActivity";
    private static final int REQUEST_CODE_PERMISSIONS = 100;

    /** All runtime permissions Whis requires for full functionality. */
    private static final List<String> getRequiredPermissions() {
        List<String> perms = new ArrayList<>();
        perms.add(Manifest.permission.RECEIVE_SMS);
        perms.add(Manifest.permission.READ_SMS);
        perms.add(Manifest.permission.READ_PHONE_STATE);
        perms.add(Manifest.permission.READ_CONTACTS);
        perms.add(Manifest.permission.READ_CALL_LOG);
        if (Build.VERSION.SDK_INT >= 33) {
            perms.add("android.permission.POST_NOTIFICATIONS");
        }
        return perms;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ── 0. Default Theme setup — Light Mode by default ─────────────────
        boolean isDarkMode = getSharedPreferences("whis_prefs", MODE_PRIVATE)
                .getBoolean("dark_mode", false);
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                isDarkMode ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                           : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
        );

        // ── 1. API Key sanity check ───────────────────────────────────────────
        // BuildConfig.GEMINI_API_KEY is injected from local.properties at build time.
        if (BuildConfig.GEMINI_API_KEY == null || BuildConfig.GEMINI_API_KEY.isEmpty()) {
            Log.e(TAG, "GEMINI_API_KEY is MISSING in local.properties.");
        } else {
            Log.d(TAG, "GEMINI_API_KEY present (" + BuildConfig.GEMINI_API_KEY.length() + " chars).");
        }

        // ── 2. Onboarding gate ────────────────────────────────────────────────
        boolean onboardingDone = getSharedPreferences("whis_prefs", MODE_PRIVATE)
                .getBoolean("onboarding_complete", false);

        if (!onboardingDone) {
            android.content.Intent onboardingIntent = new android.content.Intent(
                    this, com.whis.app.ui.onboarding.OnboardingActivity.class);
            startActivity(onboardingIntent);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
            NavigationUI.setupWithNavController(bottomNav, navController);
        }

        // ── 3. Runtime permissions ────────────────────────────────────────────
        // INTERNET is a normal (install-time) permission — no runtime request needed.
        // The remaining permissions require runtime grant on Android 6.0+.
        requestMissingPermissions();
    }

    /**
     * Requests any REQUIRED_PERMISSIONS not yet granted.
     * Silently skips any that are already granted.
     */
    private void requestMissingPermissions() {
        List<String> missing = new ArrayList<>();
        for (String permission : getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                missing.add(permission);
            }
        }

        if (!missing.isEmpty()) {
            Log.d(TAG, "Requesting " + missing.size() + " missing permission(s): " + missing);
            ActivityCompat.requestPermissions(
                    this,
                    missing.toArray(new String[0]),
                    REQUEST_CODE_PERMISSIONS
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            boolean anyDenied = false;
            for (int i = 0; i < permissions.length; i++) {
                boolean granted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                Log.d(TAG, permissions[i] + " → " + (granted ? "GRANTED" : "DENIED"));
                if (!granted) anyDenied = true;
            }
            if (anyDenied) {
                android.widget.Toast.makeText(this,
                        "Permissions are required for Whis to automatically screen scam calls and SMS.",
                        android.widget.Toast.LENGTH_LONG).show();
            }
        }
    }
}
