package com.whis.app.ui.components;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.whis.app.R;
import com.whis.app.ui.onboarding.PermissionStep;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom 3D Stacked Permission Criteria View.
 * <p>
 * Displays 5 permission cards in a vertical 3D perspective stack (receding cards
 * above and below, with the focused card raised, scaled up, and fully opaque).
 * <p>
 * Supports step-by-step progress and interactive card tapping.
 */
public class PermissionCardStackView extends FrameLayout {

    public interface OnStepSelectedListener {
        void onStepSelected(PermissionStep step, int index);
    }

    private final List<CardView> cardViews = new ArrayList<>();
    private final List<TextView> subtitleViews = new ArrayList<>();
    private final List<TextView> statusViews = new ArrayList<>();

    private OnStepSelectedListener listener;
    private int activeIndex = 0;

    private static final int[] ICON_RES = new int[]{
            R.drawable.ic_category_contact,
            R.drawable.ic_nav_messages,
            R.drawable.ic_shield_check,
            R.drawable.ic_category_scam_link,
            R.drawable.ic_nav_calls
    };

    private static final int[] BG_COLORS = new int[]{
            0xFF8E44AD, // Purple
            0xFFE67E22, // Orange/Coral
            0xFF27AE60, // Emerald Green
            0xFFE74C3C, // Crimson Red
            0xFF16A085  // Teal Cyan
    };

    public PermissionCardStackView(@NonNull Context context) {
        super(context);
        init();
    }

    public PermissionCardStackView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PermissionCardStackView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setClipChildren(false);
        setClipToPadding(false);

        PermissionStep[] steps = PermissionStep.values();
        for (int i = 0; i < steps.length; i++) {
            PermissionStep step = steps[i];
            CardView card = createPermissionCard(step, i);
            cardViews.add(card);
            addView(card);
        }

        post(this::updateStackPositionsImmediate);
    }

    public void setOnStepSelectedListener(OnStepSelectedListener listener) {
        this.listener = listener;
    }

    public void setActiveIndex(int index) {
        if (index < 0 || index >= PermissionStep.values().length) return;
        this.activeIndex = index;
        animateStackPositions();
    }

    public int getActiveIndex() {
        return activeIndex;
    }

    public PermissionStep getActiveStep() {
        return PermissionStep.values()[activeIndex];
    }

    public void updateStepStatus(int index, String statusText, boolean isGranted) {
        if (index >= 0 && index < statusViews.size()) {
            TextView tvStatus = statusViews.get(index);
            tvStatus.setText(statusText);
            tvStatus.setTextColor(isGranted ? 0xFF27AE60 : 0xFF7F8C8D);
        }
    }

    private CardView createPermissionCard(PermissionStep step, int index) {
        CardView card = new CardView(getContext());
        card.setRadius(dpToPx(18));
        card.setCardElevation(dpToPx(8));
        card.setUseCompatPadding(false);

        FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(76)
        );
        cardParams.gravity = Gravity.CENTER;
        card.setLayoutParams(cardParams);

        // Content Container
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        layout.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));
        layout.setBackgroundColor(0xFFFFFFFF);

        // 1. Icon Box Tile (Square tile with rounded corners)
        CardView iconTile = new CardView(getContext());
        iconTile.setRadius(dpToPx(12));
        iconTile.setCardElevation(0);
        iconTile.setCardBackgroundColor(BG_COLORS[index % BG_COLORS.length]);

        LinearLayout.LayoutParams tileParams = new LinearLayout.LayoutParams(dpToPx(44), dpToPx(44));
        tileParams.setMargins(0, 0, dpToPx(14), 0);
        iconTile.setLayoutParams(tileParams);

        ImageView ivIcon = new ImageView(getContext());
        ivIcon.setImageResource(ICON_RES[index % ICON_RES.length]);
        ivIcon.setColorFilter(0xFFFFFFFF);
        ivIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(dpToPx(24), dpToPx(24));
        iconParams.gravity = Gravity.CENTER;
        ivIcon.setLayoutParams(iconParams);
        iconTile.addView(ivIcon);

        // 2. Title & Subtitle Text Group
        LinearLayout textGroup = new LinearLayout(getContext());
        textGroup.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        textGroup.setLayoutParams(textParams);

        TextView tvTitle = new TextView(getContext());
        tvTitle.setText(step.title);
        tvTitle.setTextSize(16);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setTextColor(0xFF1E293B);

        TextView tvSubtitle = new TextView(getContext());
        tvSubtitle.setText(getShortDescription(step));
        tvSubtitle.setTextSize(12);
        tvSubtitle.setTextColor(0xFF64748B);
        subtitleViews.add(tvSubtitle);

        textGroup.addView(tvTitle);
        textGroup.addView(tvSubtitle);

        // 3. Status Badge Text
        TextView tvStatus = new TextView(getContext());
        tvStatus.setText("Step " + step.stepNumber);
        tvStatus.setTextSize(12);
        tvStatus.setTypeface(null, android.graphics.Typeface.BOLD);
        tvStatus.setTextColor(0xFF94A3B8);
        statusViews.add(tvStatus);

        layout.addView(iconTile);
        layout.addView(textGroup);
        layout.addView(tvStatus);

        card.addView(layout);

        // Click handler to select this card
        card.setOnClickListener(v -> {
            setActiveIndex(index);
            if (listener != null) {
                listener.onStepSelected(step, index);
            }
        });

        return card;
    }

    private String getShortDescription(PermissionStep step) {
        switch (step) {
            case CALLER_ID_ROLE:    return "Default Caller ID & Scam Protection";
            case NOTIFICATION_ACCESS:return "Screen SMS & WhatsApp phishing links";
            case BATTERY_AUTOSTART:  return "Keep protection active in background";
            case FULL_SCREEN_ALERT: return "Show full-screen alert during live scam";
            case DND_BYPASS:        return "Alert even when Do Not Disturb is ON";
            default:                 return "Security Criteria";
        }
    }

    private void updateStackPositionsImmediate() {
        int total = cardViews.size();
        float stepOffsetY = dpToPx(52);

        for (int i = 0; i < total; i++) {
            CardView card = cardViews.get(i);
            int diff = i - activeIndex;

            float targetY = diff * stepOffsetY;
            float targetScale = 1.0f - Math.min(0.24f, Math.abs(diff) * 0.06f);
            float targetAlpha = diff == 0 ? 1.0f : (1.0f - Math.min(0.60f, Math.abs(diff) * 0.20f));
            float targetElevation = diff == 0 ? dpToPx(16) : dpToPx(4);

            card.setTranslationY(targetY);
            card.setScaleX(targetScale);
            card.setScaleY(targetScale);
            card.setAlpha(targetAlpha);
            card.setCardElevation(targetElevation);

            // Active card styling
            if (diff == 0) {
                card.setCardBackgroundColor(0xFFFFFFFF);
            } else {
                card.setCardBackgroundColor(0xFFF8FAFC);
            }
        }
    }

    private void animateStackPositions() {
        int total = cardViews.size();
        float stepOffsetY = dpToPx(52);
        FastOutSlowInInterpolator interpolator = new FastOutSlowInInterpolator();

        AnimatorSet animSet = new AnimatorSet();
        List<android.animation.Animator> animators = new ArrayList<>();

        for (int i = 0; i < total; i++) {
            CardView card = cardViews.get(i);
            int diff = i - activeIndex;

            float targetY = diff * stepOffsetY;
            float targetScale = 1.0f - Math.min(0.24f, Math.abs(diff) * 0.06f);
            float targetAlpha = diff == 0 ? 1.0f : (1.0f - Math.min(0.60f, Math.abs(diff) * 0.20f));
            float targetElevation = diff == 0 ? dpToPx(16) : dpToPx(4);

            ObjectAnimator transY = ObjectAnimator.ofFloat(card, View.TRANSLATION_Y, card.getTranslationY(), targetY);
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(card, View.SCALE_X, card.getScaleX(), targetScale);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(card, View.SCALE_Y, card.getScaleY(), targetScale);
            ObjectAnimator alpha = ObjectAnimator.ofFloat(card, View.ALPHA, card.getAlpha(), targetAlpha);

            card.setCardElevation(targetElevation);
            if (diff == 0) {
                card.setCardBackgroundColor(0xFFFFFFFF);
            } else {
                card.setCardBackgroundColor(0xFFF8FAFC);
            }

            animators.add(transY);
            animators.add(scaleX);
            animators.add(scaleY);
            animators.add(alpha);
        }

        animSet.playTogether(animators);
        animSet.setDuration(350);
        animSet.setInterpolator(interpolator);
        animSet.start();
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
