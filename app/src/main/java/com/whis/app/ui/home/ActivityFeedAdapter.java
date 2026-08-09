package com.whis.app.ui.home;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.recyclerview.widget.RecyclerView;

import com.whis.app.R;
import com.whis.app.core.DetectionResult;
import com.whis.app.core.WhisVerdict;
import com.whis.app.ui.alert.AlertRenderer;
import com.whis.app.ui.components.WhisListRow;

import java.util.List;

/**
 * RecyclerView Adapter for the Home dashboard activity feed and Calls/Messages feeds (UI_PLAN.md §3.1).
 * <p>
 * Accepts any {@link DetectionResult} implementation — real call/SMS AI results, not stub data.
 * Uses {@link WhisListRow} components tagged with {@link com.whis.app.ui.components.RiskTag}.
 *
 * <h3>Category icon mapping</h3>
 * The row icon is chosen from {@link #categoryIconRes(String)} based on
 * {@link DetectionResult#getIdentifierType()}, which carries the signal type
 * (CONTACT, DLT_REGISTERED, UNKNOWN_MOBILE, COMMUNITY_REPORT, SCAM_LINK).
 * <p>
 * The raw identifier token is <b>never</b> rendered as text — only the icon appears.
 * The row title is {@link AlertRenderer#formatAlertCopy(DetectionResult)} alone.
 */
public class ActivityFeedAdapter extends RecyclerView.Adapter<ActivityFeedAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(DetectionResult item, int position);
    }

    public interface OnSelectionChangeListener {
        void onSelectionChanged(int count);
    }

    private final List<DetectionResult> items;
    private final OnItemClickListener listener;
    private OnSelectionChangeListener selectionChangeListener;
    private final java.util.Set<Integer> selectedPositions = new java.util.HashSet<>();

    public ActivityFeedAdapter(@NonNull List<DetectionResult> items,
                               @NonNull OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    public void setOnSelectionChangeListener(OnSelectionChangeListener listener) {
        this.selectionChangeListener = listener;
    }

    private void notifySelectionChanged() {
        if (selectionChangeListener != null) {
            selectionChangeListener.onSelectionChanged(selectedPositions.size());
        }
    }

    public void toggleSelection(int position) {
        if (selectedPositions.contains(position)) {
            selectedPositions.remove(position);
        } else {
            selectedPositions.add(position);
        }
        notifyItemChanged(position);
        notifySelectionChanged();
    }

    public void selectAll() {
        selectedPositions.clear();
        for (int i = 0; i < items.size(); i++) {
            selectedPositions.add(i);
        }
        notifyDataSetChanged();
        notifySelectionChanged();
    }

    public void unselectAll() {
        selectedPositions.clear();
        notifyDataSetChanged();
        notifySelectionChanged();
    }

    public List<DetectionResult> getSelectedItems() {
        List<DetectionResult> selected = new java.util.ArrayList<>();
        for (Integer pos : selectedPositions) {
            if (pos >= 0 && pos < items.size()) {
                selected.add(items.get(pos));
            }
        }
        return selected;
    }

    public int getSelectedCount() {
        return selectedPositions.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        WhisListRow row = new WhisListRow(parent.getContext());
        row.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        return new ViewHolder(row);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DetectionResult item = items.get(position);
        WhisListRow row = holder.row;

        row.setIcon(categoryIconRes(item.getIdentifierType()));

        // Clean title & subtitle formatting
        row.setTitle(formatCleanTitle(item));
        row.setSubtitle(formatCleanSubtitle(item));

        WhisVerdict verdict = item.getVerdict();
        if ("CONTACT".equalsIgnoreCase(item.getIdentifierType()) || "SAVED_CONTACT".equalsIgnoreCase(item.getIdentifierType())) {
            verdict = WhisVerdict.TRUSTED;
        }
        row.setVerdictStyle(verdict, item.getIdentifierType());

        boolean isSelected = selectedPositions.contains(position);
        if (isSelected) {
            row.setAlpha(0.9f);
            row.setBackgroundColor(0x3341C85A); // Green selection tint
        } else {
            row.setAlpha(1.0f);
            row.setBackgroundColor(0x00000000);
        }

        // Use a stable click reference via ViewHolder to avoid stale position
        row.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_ID) {
                if (!selectedPositions.isEmpty()) {
                    toggleSelection(pos);
                } else {
                    listener.onItemClick(item, pos);
                }
            }
        });

        row.setOnLongClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_ID) {
                toggleSelection(pos);
                return true;
            }
            return false;
        });

        // ── Staggered slide-up entrance animation ───────────────────────────
        row.setAlpha(0f);
        row.setTranslationY(dpToPx(row, 30f));

        long startDelay = position * 80L;
        FastOutSlowInInterpolator interpolator = new FastOutSlowInInterpolator();

        ObjectAnimator slideUp = ObjectAnimator.ofFloat(row, View.TRANSLATION_Y, dpToPx(row, 30f), 0f);
        slideUp.setDuration(300);
        slideUp.setStartDelay(startDelay);
        slideUp.setInterpolator(interpolator);

        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(row, View.ALPHA, 0f, 1f);
        fadeIn.setDuration(300);
        fadeIn.setStartDelay(startDelay);
        fadeIn.setInterpolator(interpolator);

        AnimatorSet entrance = new AnimatorSet();
        entrance.playTogether(slideUp, fadeIn);
        entrance.start();
    }

    private static String formatCleanTitle(DetectionResult item) {
        String reason = item.getReasonText() != null ? item.getReasonText() : "";

        if (item.getVerdict() == WhisVerdict.TRUSTED || "CONTACT".equalsIgnoreCase(item.getIdentifierType())) {
            if (reason.contains(" (")) {
                return reason.substring(0, reason.indexOf(" (")).trim();
            }
            return "Saved Contact";
        } else if (item.getVerdict() == WhisVerdict.HIGH_RISK) {
            return "🚨 Scam Detected";
        } else {
            return "Unknown Number";
        }
    }

    private static String formatCleanSubtitle(DetectionResult item) {
        CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                item.getTimestamp(),
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
        );
        String reason = item.getReasonText() != null ? item.getReasonText() : "";

        // Extract phone number if present
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\+?\\d{10,13}").matcher(reason);
        if (m.find()) {
            return m.group() + " · " + relativeTime;
        }
        return relativeTime.toString();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * Maps a detection category token to its semantic icon drawable.
     * <p>
     * This is the single source of truth for the category → icon mapping.
     * No raw token strings are ever displayed as text in any list.
     *
     * @param identifierType raw category token from {@link DetectionResult#getIdentifierType()}
     * @return drawable resource ID for the category icon
     */
    @DrawableRes
    public static int categoryIconRes(@NonNull String identifierType) {
        switch (identifierType.toUpperCase()) {
            case "CONTACT":
            case "SAVED_CONTACT":   return R.drawable.ic_category_contact;
            case "DLT_REGISTERED":  return R.drawable.ic_category_dlt_verified;
            case "UNKNOWN_MOBILE":  return R.drawable.ic_category_unknown;
            case "COMMUNITY_REPORT":return R.drawable.ic_category_community_report;
            case "SCAM_CALL":
            case "SCAM_LINK":       return R.drawable.ic_category_scam_link;
            default:                return R.drawable.ic_nav_calls;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final WhisListRow row;

        ViewHolder(@NonNull WhisListRow row) {
            super(row);
            this.row = row;
        }
    }

    /** Converts dp to pixels using the view's display metrics. */
    private static float dpToPx(@NonNull View view, float dp) {
        return dp * view.getResources().getDisplayMetrics().density;
    }
}
