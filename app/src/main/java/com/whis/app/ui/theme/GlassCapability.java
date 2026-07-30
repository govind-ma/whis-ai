package com.whis.app.ui.theme;

import android.os.Build;

/**
 * Single gate for real-blur capability (UI_PLAN.md §1.2 / §1.4).
 * <p>
 * {@code android:backgroundBlurRadius} (and {@code Window.setBackgroundBlurEnabled()})
 * are only available on API 31 (Android 12, "S") and above. All blur decisions in the
 * UI layer must query this class — never check {@code Build.VERSION.SDK_INT} directly
 * elsewhere for this purpose.
 * <p>
 * On API 31+, the system may still honour {@code isBlurEnabled()} returning false
 * (e.g. low-powered device flag, power-save mode). The {@link #canUseRealBlur()} method
 * only checks the API level; runtime availability is handled by the window flag in
 * {@code WhisMainActivity}.
 */
public final class GlassCapability {

    private GlassCapability() {
        // Utility class — do not instantiate
    }

    /**
     * Returns {@code true} on API 31+ (Android 12 "S"), where
     * {@code android:backgroundBlurRadius} and {@code RenderEffect} are available.
     *
     * @return true if real backdrop blur can be applied
     */
    public static boolean canUseRealBlur() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
    }
}
