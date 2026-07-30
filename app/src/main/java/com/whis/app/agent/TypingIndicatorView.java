package com.whis.app.agent;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Animated 3-dot typing indicator for Whis AI Agent chat.
 * <p>
 * Three dots sequentially animate translateY 0 → -8dp → 0 with staggered
 * start delays (dot1=0ms, dot2=150ms, dot3=300ms), looping infinitely.
 * Call {@link #startAnimation()} to begin and {@link #stopAnimation()} to halt.
 */
public class TypingIndicatorView extends View {

    private static final int DOT_COUNT = 3;
    private static final float DOT_RADIUS_DP = 4f;
    private static final float DOT_SPACING_DP = 10f;
    private static final long STAGGER_MS = 150L;
    private static final long DURATION_MS = 500L;

    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ObjectAnimator[] animators = new ObjectAnimator[DOT_COUNT];
    private final float[] dotOffsetY = new float[DOT_COUNT];

    public TypingIndicatorView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public TypingIndicatorView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TypingIndicatorView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        dotPaint.setColor(0xFF9A9A9A); // whis_text_mid equivalent
        dotPaint.setStyle(Paint.Style.FILL);

        float dropPx = dpToPx(8f);

        for (int i = 0; i < DOT_COUNT; i++) {
            final int idx = i;
            // Animate a synthetic float property that drives dotOffsetY[idx]
            ObjectAnimator anim = ObjectAnimator.ofFloat(this, "dot" + i + "OffsetY", 0f, -dropPx, 0f);
            anim.setDuration(DURATION_MS);
            anim.setStartDelay(i * STAGGER_MS);
            anim.setRepeatCount(ObjectAnimator.INFINITE);
            anim.setRepeatMode(ObjectAnimator.RESTART);
            anim.setInterpolator(new DecelerateInterpolator(1.5f));
            animators[i] = anim;
        }
    }

    // --- Synthetic property setters called by ObjectAnimator via reflection ---

    @SuppressWarnings("unused")
    public void setDot0OffsetY(float v) { dotOffsetY[0] = v; invalidate(); }
    @SuppressWarnings("unused")
    public void setDot1OffsetY(float v) { dotOffsetY[1] = v; invalidate(); }
    @SuppressWarnings("unused")
    public void setDot2OffsetY(float v) { dotOffsetY[2] = v; invalidate(); }

    @SuppressWarnings("unused")
    public float getDot0OffsetY() { return dotOffsetY[0]; }
    @SuppressWarnings("unused")
    public float getDot1OffsetY() { return dotOffsetY[1]; }
    @SuppressWarnings("unused")
    public float getDot2OffsetY() { return dotOffsetY[2]; }

    /** Start the infinite 3-dot bounce animation. */
    public void startAnimation() {
        for (ObjectAnimator a : animators) {
            if (!a.isRunning()) a.start();
        }
    }

    /** Stop all animators and reset dot positions. */
    public void stopAnimation() {
        for (int i = 0; i < DOT_COUNT; i++) {
            animators[i].cancel();
            dotOffsetY[i] = 0f;
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float radius = dpToPx(DOT_RADIUS_DP);
        float spacing = dpToPx(DOT_SPACING_DP);
        float totalWidth = (DOT_COUNT - 1) * spacing;
        float startX = (getWidth() - totalWidth) / 2f;
        float centerY = getHeight() / 2f;

        for (int i = 0; i < DOT_COUNT; i++) {
            float cx = startX + i * spacing;
            float cy = centerY + dotOffsetY[i];
            canvas.drawCircle(cx, cy, radius, dotPaint);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        float spacing = dpToPx(DOT_SPACING_DP);
        float radius = dpToPx(DOT_RADIUS_DP);
        int desiredW = (int) ((DOT_COUNT - 1) * spacing + radius * 2 + dpToPx(24));
        int desiredH = (int) (dpToPx(8f) * 2 + radius * 4); // headroom for Y travel
        setMeasuredDimension(
                resolveSize(desiredW, widthMeasureSpec),
                resolveSize(desiredH, heightMeasureSpec)
        );
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnimation(); // prevent leaks when view is removed
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
