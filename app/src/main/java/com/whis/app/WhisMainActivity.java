package com.whis.app;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Placeholder launcher activity.
 * <p>
 * The UI module will replace this with the real onboarding / home activity.
 * This exists solely to make the foundation APK installable and verifiable.
 */
public class WhisMainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // UI module will provide real layouts. For now, just show the app launches.
        setTitle("Whis — Foundation");
    }
}
