package com.whis.app.ui.components;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import com.whis.app.R;

/**
 * Reusable flat card component matching strict Kimi Visual Rules.
 * <p>
 * Highlights:
 * <ul>
 *   <li>Flat clean surface background ({@code whis_surface}) with 0dp shadow elevation</li>
 *   <li>Fine 1dp border ({@code whis_border})</li>
 *   <li>Compact 10dp corner radius</li>
 *   <li>100% crisp, high-contrast monochrome text legibility</li>
 * </ul>
 */
public class StatusCard extends CardView {

    public StatusCard(@NonNull Context context) {
        super(context);
        init(context);
    }

    public StatusCard(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public StatusCard(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        int surfaceColor = context.getResources().getColor(R.color.whis_surface, context.getTheme());
        int borderColor = context.getResources().getColor(R.color.whis_border, context.getTheme());

        // Flat surface
        setCardBackgroundColor(surfaceColor);

        // Compact radii: 10dp
        setRadius(dpToPx(context, 10));

        // Flat clean surface: 0dp elevation (no heavy shadows)
        setCardElevation(0);
        setMaxCardElevation(0);

        setPreventCornerOverlap(true);
        setUseCompatPadding(true);

        // Fine 1dp border outline
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius(dpToPx(context, 10));
        shape.setColor(surfaceColor);
        shape.setStroke((int) dpToPx(context, 1), borderColor);
        setBackground(shape);

        // Internal padding from token (16dp)
        int padding = context.getResources().getDimensionPixelSize(R.dimen.whis_spacing_card_padding);
        setContentPadding(padding, padding, padding, padding);
    }

    private static float dpToPx(Context context, float dp) {
        return dp * context.getResources().getDisplayMetrics().density;
    }
}
