package com.whis.app.ui.components;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.whis.app.R;

/**
 * Primary action button — 56dp standard height, {@code type_body_lg} text style (UI_PLAN.md §2.1).
 * <p>
 * Uses {@code whis_trusted} as the default accent color (the app's primary
 * "safe" green), white text. Call {@link #setButtonColor(int)} to override for
 * context-specific actions (e.g., red for emergency).
 * <p>
 * Usage in XML:
 * <pre>{@code
 * <com.whis.app.ui.components.PrimaryButton
 *     android:id="@+id/btn_action"
 *     android:layout_width="match_parent"
 *     android:layout_height="wrap_content"
 *     android:text="I understand and agree" />
 * }</pre>
 */
public class PrimaryButton extends MaterialButton {

    public PrimaryButton(@NonNull Context context) {
        super(context);
        init(context);
    }

    public PrimaryButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PrimaryButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        // 56dp standard touch target height
        setMinHeight(context.getResources().getDimensionPixelSize(R.dimen.whis_touch_standard));

        // type_body_lg — 17sp
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);

        // White text on accent background
        setTextColor(context.getResources().getColor(R.color.whis_surface, context.getTheme()));

        // Default accent — whis_trusted (teal green)
        int accentColor = context.getResources().getColor(R.color.whis_trusted, context.getTheme());
        setBackgroundColor(accentColor);

        // Rounded corners — 12dp to match StatusCard
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dpToPx(context, 12));
        bg.setColor(accentColor);
        setBackground(bg);

        // Center text
        setGravity(Gravity.CENTER);

        // All-caps off — natural casing per type_body_lg
        setAllCaps(false);

        // Horizontal padding
        int hPad = context.getResources().getDimensionPixelSize(R.dimen.whis_spacing_section);
        setPadding(hPad, getPaddingTop(), hPad, getPaddingBottom());
    }

    /**
     * Override the button background color for context-specific actions.
     *
     * @param colorRes a color resource ID (e.g., {@code R.color.whis_high_risk})
     */
    public void setButtonColor(int colorRes) {
        int color = getContext().getResources().getColor(colorRes, getContext().getTheme());
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dpToPx(getContext(), 12));
        bg.setColor(color);
        setBackground(bg);
    }

    private static float dpToPx(Context context, float dp) {
        return dp * context.getResources().getDisplayMetrics().density;
    }
}
