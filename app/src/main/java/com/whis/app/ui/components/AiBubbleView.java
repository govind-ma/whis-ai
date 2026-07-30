package com.whis.app.ui.components;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.whis.app.agent.AgentLauncher;

/**
 * Floating AI Bubble component (UI & AI Agent integration).
 * <p>
 * Visuals matching prompt & reference image:
 * <ul>
 *   <li>Inner sphere: Polished black orb with glossy 3D specular highlight and shine</li>
 *   <li>Outer ring: Electric cyan/blue ring rotating continuously and slowly</li>
 * </ul>
 * <p>
 * Interactive animations:
 * <ul>
 *   <li>{@link #triggerAlertJiggle()}: Temporarily 3x rotation speed for 500ms on new alert</li>
 *   <li>Drag interaction: Scale 1.05 & elevation 16dp on move; SpringAnimation spring-back on release</li>
 *   <li>Idle breathing: 5s inactivity triggers scale pulse 1.0 → 1.03 → 1.0 on 3000ms loop</li>
 * </ul>
 */
public class AiBubbleView extends View {

    private static final long ROTATION_DURATION_MS = 3000; // 3 seconds normal rotation
    private static final float CLICK_DRAG_TOLERANCE_PX = 10f;
    private static final long IDLE_DELAY_MS = 5000; // 5 seconds idle threshold

    private final Paint outerRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringGlowPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint spherePaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shinePaint     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shineSpecularPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF ringRect = new RectF();

    private float rotationAngle = 0f;
    private ValueAnimator rotationAnimator;

    // Touch dragging tracking
    private float dX, dY;
    private float downRawX, downRawY;
    private boolean isDragging = false;

    // Idle breathing & spring animations
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ObjectAnimator idleBreathingAnimator;
    private SpringAnimation scaleXSpring;
    private SpringAnimation scaleYSpring;

    private final Runnable idleRunnable = this::startIdleBreathing;

    public AiBubbleView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public AiBubbleView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public AiBubbleView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setClickable(true);
        setFocusable(true);

        // Outer rotating cyan ring paint
        outerRingPaint.setStyle(Paint.Style.STROKE);
        outerRingPaint.setStrokeWidth(dpToPx(context, 4f));
        outerRingPaint.setStrokeCap(Paint.Cap.ROUND);

        // Outer glow
        ringGlowPaint.setStyle(Paint.Style.STROKE);
        ringGlowPaint.setStrokeWidth(dpToPx(context, 8f));
        ringGlowPaint.setColor(Color.argb(80, 0, 229, 255)); // cyan glow

        // Specular shine paint
        shinePaint.setAntiAlias(true);
        shinePaint.setColor(Color.argb(220, 255, 255, 255));
        shinePaint.setStyle(Paint.Style.FILL);

        shineSpecularPaint.setAntiAlias(true);
        shineSpecularPaint.setColor(Color.argb(120, 255, 255, 255));
        shineSpecularPaint.setStyle(Paint.Style.FILL);

        // Rotation animator
        rotationAnimator = ValueAnimator.ofFloat(0f, 360f);
        rotationAnimator.setDuration(ROTATION_DURATION_MS);
        rotationAnimator.setRepeatCount(ValueAnimator.INFINITE);
        rotationAnimator.setInterpolator(new LinearInterpolator());
        rotationAnimator.addUpdateListener(animation -> {
            rotationAngle = (float) animation.getAnimatedValue();
            invalidate();
        });

        // SpringAnimations for scale spring-back
        scaleXSpring = new SpringAnimation(this, SpringAnimation.SCALE_X, 1.0f);
        scaleXSpring.getSpring()
                .setStiffness(SpringForce.STIFFNESS_MEDIUM)
                .setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);

        scaleYSpring = new SpringAnimation(this, SpringAnimation.SCALE_Y, 1.0f);
        scaleYSpring.getSpring()
                .setStiffness(SpringForce.STIFFNESS_MEDIUM)
                .setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);

        resetIdleTimer();
    }

    // ── Public API 1: Trigger Alert Jiggle ─────────────────────────────────

    /**
     * Temporarily increases rotation speed 3x for 500ms then returns to normal speed.
     * Called when a new alert is received.
     */
    public void triggerAlertJiggle() {
        if (rotationAnimator == null) return;

        rotationAnimator.setDuration(ROTATION_DURATION_MS / 3); // 3x speed

        mainHandler.postDelayed(() -> {
            if (rotationAnimator != null) {
                rotationAnimator.setDuration(ROTATION_DURATION_MS); // return to normal
            }
        }, 500);
    }

    // ── Public API 2 & 3: Idle Breathing & Interactions ───────────────────

    private void resetIdleTimer() {
        mainHandler.removeCallbacks(idleRunnable);
        stopIdleBreathing();
        mainHandler.postDelayed(idleRunnable, IDLE_DELAY_MS);
    }

    private void startIdleBreathing() {
        if (idleBreathingAnimator != null && idleBreathingAnimator.isRunning()) return;

        PropertyValuesHolder pvhX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.03f, 1.0f);
        PropertyValuesHolder pvhY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.03f, 1.0f);

        idleBreathingAnimator = ObjectAnimator.ofPropertyValuesHolder(this, pvhX, pvhY);
        idleBreathingAnimator.setDuration(3000);
        idleBreathingAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        idleBreathingAnimator.setRepeatMode(ObjectAnimator.RESTART);
        idleBreathingAnimator.setInterpolator(new FastOutSlowInInterpolator());
        idleBreathingAnimator.start();
    }

    private void stopIdleBreathing() {
        if (idleBreathingAnimator != null) {
            idleBreathingAnimator.cancel();
            setScaleX(1.0f);
            setScaleY(1.0f);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (rotationAnimator != null && !rotationAnimator.isRunning()) {
            rotationAnimator.start();
        }
        resetIdleTimer();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (rotationAnimator != null) {
            rotationAnimator.cancel();
        }
        stopIdleBreathing();
        mainHandler.removeCallbacks(idleRunnable);
        super.onDetachedFromWindow();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int defaultSize = (int) dpToPx(getContext(), 64f);
        int width  = resolveSize(defaultSize, widthMeasureSpec);
        int height = resolveSize(defaultSize, heightMeasureSpec);
        int size   = Math.min(width, height);
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width  = getWidth();
        float height = getHeight();
        float cx = width / 2f;
        float cy = height / 2f;

        float ringRadius  = (Math.min(width, height) / 2f) - dpToPx(getContext(), 5f);
        float sphereRadius = ringRadius * 0.72f;

        // 1. Draw Outer Glowing Cyan Ring (rotating slowly)
        ringRect.set(cx - ringRadius, cy - ringRadius, cx + ringRadius, cy + ringRadius);

        canvas.save();
        canvas.rotate(rotationAngle, cx, cy);

        // Linear gradient on the rotating ring
        Shader ringShader = new LinearGradient(
                ringRect.left, ringRect.top, ringRect.right, ringRect.bottom,
                new int[]{
                        Color.parseColor("#80D8FF"),
                        Color.parseColor("#00E5FF"),
                        Color.parseColor("#0091EA"),
                        Color.parseColor("#80D8FF")
                },
                new float[]{0f, 0.4f, 0.8f, 1f},
                Shader.TileMode.CLAMP
        );
        outerRingPaint.setShader(ringShader);

        // Draw soft ambient glow arc
        canvas.drawCircle(cx, cy, ringRadius, ringGlowPaint);

        // Draw primary rotating ring
        canvas.drawCircle(cx, cy, ringRadius, outerRingPaint);

        canvas.restore();

        // 2. Draw Shiny Black Sphere in center
        float sphereOffsetX = cx - (sphereRadius * 0.3f);
        float sphereOffsetY = cy - (sphereRadius * 0.35f);

        RadialGradient sphereGradient = new RadialGradient(
                sphereOffsetX, sphereOffsetY, sphereRadius * 1.3f,
                new int[]{
                        Color.parseColor("#455A64"),
                        Color.parseColor("#1C252B"),
                        Color.parseColor("#0A0E12"),
                        Color.parseColor("#040608")
                },
                new float[]{0f, 0.35f, 0.75f, 1f},
                Shader.TileMode.CLAMP
        );
        spherePaint.setShader(sphereGradient);
        canvas.drawCircle(cx, cy, sphereRadius, spherePaint);

        // 3. Draw Glossy Specular Reflection Highlight
        float shineX = cx - (sphereRadius * 0.32f);
        float shineY = cy - (sphereRadius * 0.32f);
        float shineRadiusX = sphereRadius * 0.24f;
        float shineRadiusY = sphereRadius * 0.38f;

        canvas.save();
        canvas.rotate(-30f, shineX, shineY);
        RectF shineRect = new RectF(
                shineX - shineRadiusX, shineY - shineRadiusY,
                shineX + shineRadiusX, shineY + shineRadiusY
        );
        canvas.drawOval(shineRect, shinePaint);

        // Secondary subtle specular accent
        RectF accentRect = new RectF(
                shineX + (shineRadiusX * 0.6f), shineY + (shineRadiusY * 0.8f),
                shineX + (shineRadiusX * 1.2f), shineY + (shineRadiusY * 1.3f)
        );
        canvas.drawOval(accentRect, shineSpecularPaint);

        canvas.restore();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float rawX = event.getRawX();
        float rawY = event.getRawY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                resetIdleTimer();
                downRawX = rawX;
                downRawY = rawY;
                dX = getX() - rawX;
                dY = getY() - rawY;
                isDragging = false;
                animate().scaleX(1.05f).scaleY(1.05f).setDuration(100).start();
                setElevation(dpToPx(getContext(), 16f));
                return true;

            case MotionEvent.ACTION_MOVE:
                resetIdleTimer();
                float deltaX = Math.abs(rawX - downRawX);
                float deltaY = Math.abs(rawY - downRawY);

                if (deltaX > CLICK_DRAG_TOLERANCE_PX || deltaY > CLICK_DRAG_TOLERANCE_PX) {
                    isDragging = true;
                    setX(rawX + dX);
                    setY(rawY + dY);
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                resetIdleTimer();
                setElevation(dpToPx(getContext(), 4f));

                // Spring back scale to 1.0 using SpringAnimation
                scaleXSpring.animateToFinalPosition(1.0f);
                scaleYSpring.animateToFinalPosition(1.0f);

                if (event.getAction() == MotionEvent.ACTION_UP && !isDragging) {
                    performClick();
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        super.performClick();
        AgentLauncher.launch(getContext());
        return true;
    }

    private static float dpToPx(Context context, float dp) {
        return dp * context.getResources().getDisplayMetrics().density;
    }
}
