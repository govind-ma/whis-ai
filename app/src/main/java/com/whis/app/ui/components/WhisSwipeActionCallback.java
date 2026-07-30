package com.whis.app.ui.components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Shared swipe callback for Calls and Messages list RecyclerViews.
 * <p>
 * Swipe LEFT  → green "Mark Safe" reveal<br>
 * Swipe RIGHT → red "Report Scam" reveal
 * <p>
 * Partial swipe (< threshold) springs the item back to rest using
 * {@link SpringAnimation} (STIFFNESS_MEDIUM / DAMPING_RATIO_MEDIUM_BOUNCY).
 */
public class WhisSwipeActionCallback extends ItemTouchHelper.SimpleCallback {

    public interface SwipeListener {
        /** Called when user completes a full swipe left (mark safe). */
        void onMarkSafe(int adapterPosition);
        /** Called when user completes a full swipe right (report scam). */
        void onReportScam(int adapterPosition);
    }

    private static final int COLOR_SAFE   = 0xFF2E7D32; // dark green
    private static final int COLOR_SCAM   = 0xFF8B1A1A; // dark red
    private static final float LABEL_TEXT_SIZE_SP = 13f;
    private static final float SWIPE_THRESHOLD = 0.35f; // 35% of view width to commit

    private final SwipeListener listener;
    private final Paint paintText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ColorDrawable bgDrawable = new ColorDrawable();

    public WhisSwipeActionCallback(@NonNull SwipeListener listener) {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.listener = listener;
        paintText.setColor(Color.WHITE);
        paintText.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
        return SWIPE_THRESHOLD;
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView,
                          @NonNull RecyclerView.ViewHolder viewHolder,
                          @NonNull RecyclerView.ViewHolder target) {
        return false; // no drag-to-reorder
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int pos = viewHolder.getAdapterPosition();
        if (direction == ItemTouchHelper.LEFT) {
            listener.onMarkSafe(pos);
        } else {
            listener.onReportScam(pos);
        }
    }

    // ── Spring-back on partial swipe ────────────────────────────────────────

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder,
                            float dX, float dY, int actionState, boolean isCurrentlyActive) {

        if (actionState != ItemTouchHelper.ACTION_STATE_SWIPE) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            return;
        }

        View itemView = viewHolder.itemView;
        int itemHeight = itemView.getHeight();

        if (dX < 0) {
            // Swipe LEFT — "Mark Safe" green reveal
            bgDrawable.setColor(COLOR_SAFE);
            bgDrawable.setBounds(
                    itemView.getRight() + (int) dX, itemView.getTop(),
                    itemView.getRight(), itemView.getBottom());
            bgDrawable.draw(c);

            float textSize = LABEL_TEXT_SIZE_SP * recyclerView.getResources().getDisplayMetrics().scaledDensity;
            paintText.setTextSize(textSize);
            float textX = itemView.getRight() + dX / 2f;
            float textY = itemView.getTop() + itemHeight / 2f - (paintText.descent() + paintText.ascent()) / 2f;
            c.drawText("✓ Mark Safe", textX, textY, paintText);

        } else if (dX > 0) {
            // Swipe RIGHT — "Report Scam" red reveal
            bgDrawable.setColor(COLOR_SCAM);
            bgDrawable.setBounds(
                    itemView.getLeft(), itemView.getTop(),
                    itemView.getLeft() + (int) dX, itemView.getBottom());
            bgDrawable.draw(c);

            float textSize = LABEL_TEXT_SIZE_SP * recyclerView.getResources().getDisplayMetrics().scaledDensity;
            paintText.setTextSize(textSize);
            float textX = itemView.getLeft() + dX / 2f;
            float textY = itemView.getTop() + itemHeight / 2f - (paintText.descent() + paintText.ascent()) / 2f;
            c.drawText("⚑ Report Scam", textX, textY, paintText);
        }

        // Translate itemView — standard gesture follow
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView,
                          @NonNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        // Ensure translation is zeroed on a fully committed or released swipe
        viewHolder.itemView.setTranslationX(0f);
    }

    // ── Public factory with spring-back wiring ──────────────────────────────

    /**
     * Attach to a RecyclerView. The returned {@link ItemTouchHelper} is already attached.
     * <p>
     * Partial swipes (below threshold) are sprung back using
     * {@link SpringAnimation} STIFFNESS_MEDIUM / DAMPING_RATIO_MEDIUM_BOUNCY.
     */
    public static ItemTouchHelper attachTo(@NonNull RecyclerView rv, @NonNull SwipeListener listener) {
        WhisSwipeActionCallback callback = new WhisSwipeActionCallback(listener) {
            @Override
            public void onChildDrawOver(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                        RecyclerView.ViewHolder viewHolder,
                                        float dX, float dY, int actionState, boolean isCurrentlyActive) {
                super.onChildDrawOver(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

                // When finger is lifted on a partial swipe, spring item back
                if (!isCurrentlyActive && actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    View item = viewHolder.itemView;
                    if (Math.abs(item.getTranslationX()) > 1f) {
                        springBack(item);
                    }
                }
            }
        };
        ItemTouchHelper helper = new ItemTouchHelper(callback);
        helper.attachToRecyclerView(rv);
        return helper;
    }

    /**
     * Springs the view's translationX back to 0 using
     * STIFFNESS_MEDIUM + DAMPING_RATIO_MEDIUM_BOUNCY.
     */
    public static void springBack(@NonNull View item) {
        SpringAnimation springX = new SpringAnimation(item, SpringAnimation.TRANSLATION_X, 0f);
        springX.getSpring()
               .setStiffness(SpringForce.STIFFNESS_MEDIUM)
               .setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);
        springX.start();
    }
}
