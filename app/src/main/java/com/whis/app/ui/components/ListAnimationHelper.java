package com.whis.app.ui.components;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Static helpers for list entrance and empty-state animations used by
 * CallsFragment, MessagesFragment, and similar list screens.
 */
public final class ListAnimationHelper {

    private static final int STAGGER_MAX_POSITION = 5; // cap so off-screen items don't over-delay
    private static final long STAGGER_MS = 60L;
    private static final long ITEM_DURATION_MS = 300L;
    private static final long FLOAT_DURATION_MS = 2000L;

    private ListAnimationHelper() { /* static only */ }

    // ── 1. Staggered slide-up entrance on RecyclerView ─────────────────────

    /**
     * Returns an {@link RecyclerView.AdapterDataObserver} that triggers staggered
     * slide-up animation (translateY +40dp → 0, alpha 0→1, 300ms, 60ms stagger,
     * capped at position 5) for every item visible after the first data load.
     * <p>
     * Register via {@code adapter.registerAdapterDataObserver(helper)}.
     */
    public static RecyclerView.AdapterDataObserver staggeredEntranceObserver(
            @NonNull RecyclerView recyclerView) {

        return new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                recyclerView.post(() -> animateAll(recyclerView));
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                recyclerView.post(() -> animateAll(recyclerView));
            }
        };
    }

    private static void animateAll(@NonNull RecyclerView recyclerView) {
        int childCount = recyclerView.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = recyclerView.getChildAt(i);
            if (child == null) continue;

            int cappedPos = Math.min(i, STAGGER_MAX_POSITION);
            long delay = cappedPos * STAGGER_MS;
            float fromY = dpToPx(recyclerView, 40f);

            child.setAlpha(0f);
            child.setTranslationY(fromY);

            FastOutSlowInInterpolator interp = new FastOutSlowInInterpolator();

            ObjectAnimator slideUp = ObjectAnimator.ofFloat(child, View.TRANSLATION_Y, fromY, 0f);
            slideUp.setDuration(ITEM_DURATION_MS);
            slideUp.setStartDelay(delay);
            slideUp.setInterpolator(interp);

            ObjectAnimator fadeIn = ObjectAnimator.ofFloat(child, View.ALPHA, 0f, 1f);
            fadeIn.setDuration(ITEM_DURATION_MS);
            fadeIn.setStartDelay(delay);
            fadeIn.setInterpolator(interp);

            AnimatorSet set = new AnimatorSet();
            set.playTogether(slideUp, fadeIn);
            set.start();
        }
    }

    // ── 2. Empty state icon float animation ────────────────────────────────

    /**
     * Starts infinite translateY float on the given view:
     * 0 → -8dp → 0, 2000ms loop, sine-like DecelerateInterpolator.
     */
    public static void startIconFloat(@NonNull View iconView) {
        float dropPx = dpToPx(iconView, 8f);
        ObjectAnimator floatAnim = ObjectAnimator.ofFloat(
                iconView, View.TRANSLATION_Y, 0f, -dropPx, 0f);
        floatAnim.setDuration(FLOAT_DURATION_MS);
        floatAnim.setRepeatCount(ObjectAnimator.INFINITE);
        floatAnim.setRepeatMode(ObjectAnimator.RESTART);
        floatAnim.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        floatAnim.start();

        // Tag so we can cancel later on detach
        iconView.setTag(floatAnim);
    }

    /** Cancel a float animation previously started by {@link #startIconFloat(View)}. */
    public static void stopIconFloat(@NonNull View iconView) {
        Object tag = iconView.getTag();
        if (tag instanceof ObjectAnimator) {
            ((ObjectAnimator) tag).cancel();
            iconView.setTranslationY(0f);
        }
    }

    // ── Utility ─────────────────────────────────────────────────────────────

    private static float dpToPx(@NonNull View view, float dp) {
        return dp * view.getResources().getDisplayMetrics().density;
    }
}
