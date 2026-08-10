package com.whis.app.ui.components;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.whis.app.R;
import com.whis.app.core.WhisVerdict;

/**
 * Small label component that renders the correct color + text for a {@link WhisVerdict} tier.
 * <p>
 * Maps each verdict to its token color from {@code colors.xml} and displays a
 * human-readable label. Uses a rounded-rect pill shape with the verdict color
 * as background at 15% opacity and the verdict color as text color (WCAG AA
 * compliant per UI_PLAN.md §2.1).
 * <p>
 * Usage:
 * <pre>{@code
 * RiskTag tag = findViewById(R.id.risk_tag);
 * tag.setVerdict(WhisVerdict.HIGH_RISK);
 * }</pre>
 */
public class RiskTag extends AppCompatTextView {

    private static final float CORNER_RADIUS_DP = 6f;
    private static final int BG_ALPHA = 38; // ~15% of 255

    public RiskTag(@NonNull Context context) {
        super(context);
        init();
    }

    public RiskTag(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RiskTag(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setGravity(Gravity.CENTER);
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13); // type_caption floor
        int hPad = dpToPx(10);
        int vPad = dpToPx(4);
        setPadding(hPad, vPad, hPad, vPad);
        setSingleLine(true);
    }

    /**
     * Set the verdict tier. Updates label text, text color, and pill background.
     *
     * @param verdict the {@link WhisVerdict} to display
     */
    public void setVerdict(@NonNull WhisVerdict verdict) {
        int colorRes = verdictToColorRes(verdict);
        String label = verdictToLabel(verdict);

        int color = getContext().getResources().getColor(colorRes, getContext().getTheme());

        setText(label);
        setTextColor(color);

        // Pill background — verdict color at 15% opacity
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dpToPx(CORNER_RADIUS_DP));
        // Set background color with alpha
        int bgColor = (BG_ALPHA << 24) | (color & 0x00FFFFFF);
        bg.setColor(bgColor);
        setBackground(bg);

        // ── Animation Layer 3: Badge chip scale-in ─────────────────────────
        // Scale 0.7 → 1.0 + alpha 0 → 1, 200ms, FastOutSlowInInterpolator
        setScaleX(0.7f);
        setScaleY(0.7f);
        setAlpha(0f);

        FastOutSlowInInterpolator interp = new FastOutSlowInInterpolator();

        ObjectAnimator sx = ObjectAnimator.ofFloat(this, View.SCALE_X, 0.7f, 1.0f);
        sx.setDuration(200);
        sx.setInterpolator(interp);

        ObjectAnimator sy = ObjectAnimator.ofFloat(this, View.SCALE_Y, 0.7f, 1.0f);
        sy.setDuration(200);
        sy.setInterpolator(interp);

        ObjectAnimator fa = ObjectAnimator.ofFloat(this, View.ALPHA, 0f, 1.0f);
        fa.setDuration(200);
        fa.setInterpolator(interp);

        AnimatorSet chipIn = new AnimatorSet();
        chipIn.playTogether(sx, sy, fa);
        chipIn.start();
    }

    private static int verdictToColorRes(@NonNull WhisVerdict verdict) {
        switch (verdict) {
            case TRUSTED:     return R.color.whis_trusted;
            case LIKELY_SAFE: return R.color.whis_likely_safe;
            case UNKNOWN:     return R.color.whis_unknown;
            case SUSPICIOUS:  return R.color.whis_suspicious;
            case HIGH_RISK:   return R.color.whis_high_risk;
            case BLOCKED:     return R.color.whis_high_risk; // dark red reuse
            default:          return R.color.whis_unknown;
        }
    }

    private static String verdictToLabel(@NonNull WhisVerdict verdict) {
        switch (verdict) {
            case TRUSTED:     return "Trusted";
            case LIKELY_SAFE: return "Likely Safe";
            case UNKNOWN:     return "Unknown";
            case SUSPICIOUS:  return "Suspicious";
            case HIGH_RISK:   return "High Risk";
            case BLOCKED:     return "🚫 Blocked";
            default:          return "Unknown";
        }
    }

    private int dpToPx(float dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }
}
