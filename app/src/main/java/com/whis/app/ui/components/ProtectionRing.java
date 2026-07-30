package com.whis.app.ui.components;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.whis.app.R;
import com.whis.app.core.WhisVerdict;

/**
 * Circular home-screen protection status indicator (UI_PLAN.md §3.1).
 * <p>
 * Draws a ring whose color reflects the current overall protection status
 * (mapped from {@link WhisVerdict}). The ring is drawn as an arc stroke
 * with a configurable "fill" percentage (0.0–1.0) representing protection
 * coverage, and a track behind it in {@code whis_border} color.
 * <p>
 * A soft outer glow (BlurMaskFilter) is drawn behind the active ring arc,
 * matching the current verdict color. Software layer is required for
 * BlurMaskFilter to render correctly.
 * <p>
 * Usage:
 * <pre>{@code
 * ProtectionRing ring = findViewById(R.id.protection_ring);
 * ring.setStatus(WhisVerdict.TRUSTED, 1.0f);   // fully protected
 * ring.setStatus(WhisVerdict.UNKNOWN, 0.0f);    // not yet set up
 * }</pre>
 */
public class ProtectionRing extends View {

    private static final float STROKE_WIDTH_DP = 10f;
    private static final float GLOW_RADIUS_DP  = 14f;
    private static final float GLOW_INSET_DP   = 2f;
    private static final float START_ANGLE = -90f; // 12-o'clock

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcRect    = new RectF();
    private final RectF glowRect   = new RectF();

    private float fillFraction = 1.0f; // 0.0 – 1.0
    private int ringColor;

    public ProtectionRing(@NonNull Context context) {
        super(context);
        init(context);
    }

    public ProtectionRing(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ProtectionRing(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        // BlurMaskFilter requires software layer rendering
        setLayerType(LAYER_TYPE_SOFTWARE, null);

        float strokePx = dpToPx(context, STROKE_WIDTH_DP);
        float glowPx   = dpToPx(context, GLOW_RADIUS_DP);

        // Track — whis_border, the unfilled background ring
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(strokePx);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);
        trackPaint.setColor(context.getResources().getColor(R.color.whis_border, context.getTheme()));

        // Active ring — defaults to whis_trusted
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(strokePx);
        ringPaint.setStrokeCap(Paint.Cap.ROUND);
        ringColor = context.getResources().getColor(R.color.whis_trusted, context.getTheme());
        ringPaint.setColor(ringColor);

        // Glow paint — same arc, blurred via BlurMaskFilter (not RenderEffect)
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(strokePx * 2.2f);
        glowPaint.setStrokeCap(Paint.Cap.ROUND);
        glowPaint.setColor(ringColor);
        glowPaint.setAlpha(90); // ~35% opacity — soft, not overpowering
        glowPaint.setMaskFilter(new BlurMaskFilter(glowPx, BlurMaskFilter.Blur.NORMAL));
    }

    /**
     * Set the protection status displayed by the ring.
     *
     * @param verdict  the current overall verdict (determines color)
     * @param fraction fill fraction 0.0–1.0 (0 = empty ring, 1 = full circle)
     */
    public void setStatus(@NonNull WhisVerdict verdict, float fraction) {
        this.fillFraction = Math.max(0f, Math.min(1f, fraction));
        int colorRes = verdictToColorRes(verdict);
        ringColor = getContext().getResources().getColor(colorRes, getContext().getTheme());
        ringPaint.setColor(ringColor);
        glowPaint.setColor(ringColor); // glow always matches ring
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float strokePx  = dpToPx(getContext(), STROKE_WIDTH_DP);
        float half       = strokePx / 2f;
        float glowInset  = dpToPx(getContext(), GLOW_INSET_DP);

        arcRect.set(half, half, getWidth() - half, getHeight() - half);
        glowRect.set(half + glowInset, half + glowInset,
                     getWidth() - half - glowInset, getHeight() - half - glowInset);

        // 1. Draw full track
        canvas.drawArc(arcRect, 0f, 360f, false, trackPaint);

        if (fillFraction > 0f) {
            float sweepAngle = 360f * fillFraction;

            // 2. Glow arc first (behind ring) — blurred halo in verdict color
            glowPaint.setAlpha(90);
            canvas.drawArc(glowRect, START_ANGLE, sweepAngle, false, glowPaint);

            // 3. Crisp active ring on top
            canvas.drawArc(arcRect, START_ANGLE, sweepAngle, false, ringPaint);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int defaultSize = (int) dpToPx(getContext(), 120);
        int width  = resolveSize(defaultSize, widthMeasureSpec);
        int height = resolveSize(defaultSize, heightMeasureSpec);
        int size   = Math.min(width, height);
        setMeasuredDimension(size, size);
    }

    private static int verdictToColorRes(@NonNull WhisVerdict verdict) {
        switch (verdict) {
            case TRUSTED:     return R.color.whis_trusted;
            case LIKELY_SAFE: return R.color.whis_likely_safe;
            case UNKNOWN:     return R.color.whis_unknown;
            case SUSPICIOUS:  return R.color.whis_suspicious;
            case HIGH_RISK:   return R.color.whis_high_risk;
            default:          return R.color.whis_unknown;
        }
    }

    private static float dpToPx(Context context, float dp) {
        return dp * context.getResources().getDisplayMetrics().density;
    }
}
