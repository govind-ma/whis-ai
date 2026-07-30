package com.whis.app.ui.components;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.whis.app.R;

/**
 * Flat clean card component matching strict Kimi Visual Rules.
 * <p>
 * Enforces flat clean background surfaces (#1E1E1E), fine 1dp borders (#2E2E2E),
 * compact radii (10dp), zero shadow elevation, and crisp monochrome contrast.
 */
public class GlassCardView extends FrameLayout {

    private static final float CORNER_RADIUS_DP = 10f;
    private FrameLayout surfaceLayer;

    public GlassCardView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public GlassCardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public GlassCardView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setBackgroundColor(Color.TRANSPARENT);
        float cornerRadiusPx = dpToPx(context, CORNER_RADIUS_DP);

        // Flat clean surface layer
        surfaceLayer = new FrameLayout(context);
        GradientDrawable surfaceBg = new GradientDrawable();
        surfaceBg.setShape(GradientDrawable.RECTANGLE);
        surfaceBg.setCornerRadius(cornerRadiusPx);

        int surfaceColor = context.getResources().getColor(R.color.whis_surface, context.getTheme());
        int borderColor = context.getResources().getColor(R.color.whis_border, context.getTheme());

        surfaceBg.setColor(surfaceColor);
        surfaceBg.setStroke((int) dpToPx(context, 1f), borderColor);

        surfaceLayer.setBackground(surfaceBg);
        super.addView(surfaceLayer, -1,
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        // Flat clean surface: 0dp elevation (no heavy shadows)
        setElevation(0);
        setClipToOutline(true);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (surfaceLayer != null && child != surfaceLayer) {
            surfaceLayer.addView(child, params);
        } else {
            super.addView(child, index, params);
        }
    }

    private static float dpToPx(Context context, float dp) {
        return dp * context.getResources().getDisplayMetrics().density;
    }
}
