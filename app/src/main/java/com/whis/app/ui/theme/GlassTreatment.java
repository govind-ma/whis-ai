package com.whis.app.ui.theme;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import com.whis.app.R;

/**
 * Utility for applying capability-gated surface styling (UI_PLAN.md §1.2 / §2.1).
 * <p>
 * Used by {@link com.whis.app.ui.alert.AlertRenderer} to style the alert bottom sheet.
 * <p>
 * <b>Blur safety:</b> To prevent text blurring, RenderEffect is never applied directly to
 * layout containers holding text/icon children. Surfaces are rendered with 100% crisp
 * background drawables.
 */
public final class GlassTreatment {

    private GlassTreatment() {
        // Utility class
    }

    /**
     * Applies surface styling to a view root.
     *
     * @param view           root view to style
     * @param context        context
     * @param cornerRadiusDp corner radius in dp
     * @param elevationDp    card elevation in dp
     */
    public static void applyGlassOrSolidSurface(@NonNull View view, @NonNull Context context,
                                                  float cornerRadiusDp, float elevationDp) {
        float cornerPx    = dpToPx(context, cornerRadiusDp);
        float elevationPx = dpToPx(context, elevationDp);
        int surfaceColor  = context.getResources().getColor(R.color.whis_surface, context.getTheme());
        int borderColor   = context.getResources().getColor(R.color.whis_border, context.getTheme());

        applySolidSurface(view, cornerPx, elevationPx, surfaceColor, borderColor);
    }

    private static void applySolidSurface(@NonNull View view, float cornerPx, float elevationPx,
                                           @ColorInt int surfaceColor, @ColorInt int borderColor) {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(cornerPx);
        bg.setColor(surfaceColor);
        bg.setStroke(2, Color.argb(160,
                Color.red(borderColor), Color.green(borderColor), Color.blue(borderColor)));
        view.setBackground(bg);
        view.setElevation(elevationPx);
    }

    private static float dpToPx(Context context, float dp) {
        return dp * context.getResources().getDisplayMetrics().density;
    }
}
