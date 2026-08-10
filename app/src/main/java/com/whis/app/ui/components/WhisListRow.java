package com.whis.app.ui.components;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.whis.app.R;
import com.whis.app.core.WhisVerdict;

/**
 * Shared list-row component — icon + title + subtitle + optional {@link RiskTag}
 * (UI_PLAN.md §3.1).
 * <p>
 * Minimum 56dp tall ({@code whis_list_row_min_height}). No text truncation or
 * ellipsis on any risk-relevant text — the row wraps to accommodate full content
 * per UI_PLAN.md §2.1.
 * <p>
 * Usage:
 * <pre>{@code
 * WhisListRow row = new WhisListRow(context);
 * row.setIcon(R.drawable.ic_call);
 * row.setTitle("Unknown Caller");
 * row.setSubtitle("+91 98765 43210 · 2 min ago");
 * row.setVerdict(WhisVerdict.SUSPICIOUS);
 * container.addView(row);
 * }</pre>
 */
public class WhisListRow extends LinearLayout {

    private ImageView iconView;
    private TextView titleView;
    private TextView subtitleView;
    private RiskTag riskTag;
    private RiskTag blockedTag;

    public WhisListRow(@NonNull Context context) {
        super(context);
        init(context);
    }

    public WhisListRow(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public WhisListRow(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setMinimumHeight(context.getResources().getDimensionPixelSize(R.dimen.whis_list_row_min_height));

        int itemSpacing = context.getResources().getDimensionPixelSize(R.dimen.whis_spacing_item);
        int cardPadding = context.getResources().getDimensionPixelSize(R.dimen.whis_spacing_card_padding);
        setPadding(cardPadding, itemSpacing, cardPadding, itemSpacing);

        // ── Icon with tinted circle background ───────────────────────────
        int iconContainerSize = dpToPx(context, 40);
        FrameLayout iconContainer = new FrameLayout(context);
        iconContainer.setLayoutParams(new LayoutParams(iconContainerSize, iconContainerSize));

        // Default tint circle: whis_unknown gray at 15% opacity
        int defaultBgColor = context.getResources().getColor(R.color.whis_unknown, context.getTheme());
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setShape(GradientDrawable.OVAL);
        iconBg.setColor((38 << 24) | (defaultBgColor & 0x00FFFFFF)); // 15% alpha
        iconContainer.setBackground(iconBg);

        iconView = new ImageView(context);
        int iconSize = dpToPx(context, 22);
        FrameLayout.LayoutParams iconLP = new FrameLayout.LayoutParams(iconSize, iconSize);
        iconLP.gravity = Gravity.CENTER;
        iconView.setLayoutParams(iconLP);
        iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        iconContainer.addView(iconView);

        LayoutParams containerParams = new LayoutParams(iconContainerSize, iconContainerSize);
        containerParams.setMarginEnd(itemSpacing);
        iconContainer.setLayoutParams(containerParams);
        addView(iconContainer);

        // ── Text column (title + subtitle, no truncation) ────────────────
        LinearLayout textColumn = new LinearLayout(context);
        textColumn.setOrientation(VERTICAL);
        LayoutParams textParams = new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textColumn.setLayoutParams(textParams);

        titleView = new TextView(context);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16); // type_body
        titleView.setTextColor(context.getResources().getColor(R.color.whis_text_hi, context.getTheme()));
        // No ellipsis, no maxLines — risk-relevant text must never be truncated
        titleView.setLayoutParams(new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        textColumn.addView(titleView);

        subtitleView = new TextView(context);
        subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13); // type_caption
        subtitleView.setTextColor(context.getResources().getColor(R.color.whis_text_mid, context.getTheme()));
        subtitleView.setLayoutParams(new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        textColumn.addView(subtitleView);

        addView(textColumn);

        // ── RiskTag (optional, hidden by default) ────────────────────────
        riskTag = new RiskTag(context);
        LayoutParams tagParams = new LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tagParams.setMarginStart(itemSpacing);
        riskTag.setLayoutParams(tagParams);
        riskTag.setVisibility(GONE);
        addView(riskTag);

        // ── Blocked chip (hidden by default, shown when number is blocked) ─
        blockedTag = new RiskTag(context);
        LayoutParams blockedTagParams = new LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blockedTagParams.setMarginStart(dpToPx(context, 4));
        blockedTag.setLayoutParams(blockedTagParams);
        blockedTag.setVisibility(GONE);
        addView(blockedTag);
    }

    /** Set the leading icon drawable resource. */
    public void setIcon(@DrawableRes int resId) {
        iconView.setImageResource(resId);
        iconView.setVisibility(VISIBLE);
    }

    /** Hide the icon (e.g. for rows that don't need one). */
    public void hideIcon() {
        iconView.setVisibility(GONE);
    }

    /** Set the title text (no truncation — wraps fully). */
    public void setTitle(@NonNull String text) {
        titleView.setText(text);
    }

    /** Set the subtitle text (no truncation — wraps fully). */
    public void setSubtitle(@NonNull String text) {
        subtitleView.setText(text);
        subtitleView.setVisibility(VISIBLE);
    }

    /** Hide the subtitle row. */
    public void hideSubtitle() {
        subtitleView.setVisibility(GONE);
    }

    /**
     * Show a {@link RiskTag} for the given verdict. Pass {@code null} to hide.
     */
    public void setVerdict(@Nullable WhisVerdict verdict) {
        if (verdict == null) {
            riskTag.setVisibility(GONE);
        } else {
            riskTag.setVerdict(verdict);
            riskTag.setVisibility(VISIBLE);
        }
    }

    /**
     * Show or hide the secondary "🚫 Blocked" chip.
     * When shown, also dims the row slightly to indicate the number is silenced.
     *
     * @param isBlocked true to show the Blocked chip; false to hide it
     */
    public void setBlockedOverlay(boolean isBlocked) {
        if (isBlocked) {
            blockedTag.setVerdict(WhisVerdict.BLOCKED);
            blockedTag.setVisibility(VISIBLE);
            // Dim the title to signal this call is silenced
            titleView.setAlpha(0.55f);
            subtitleView.setAlpha(0.55f);
        } else {
            blockedTag.setVisibility(GONE);
            titleView.setAlpha(1.0f);
            subtitleView.setAlpha(1.0f);
        }
    }

    /**
     * Apply clean background card tinting based on verdict:
     * - Known Contact / Trusted → Light Green tint
     * - Unknown Normal → Light Yellow tint
     * - Confirmed Scam → Light Red tint
     */
    /**
     * Apply clean neutral card background and direct verdict tag styling to the right badge.
     */
    public void setVerdictStyle(@Nullable WhisVerdict verdict, @Nullable String identifierType) {
        setVerdict(verdict);

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setCornerRadius(dpToPx(getContext(), 12));

        boolean isDark = (getContext().getResources().getConfiguration().uiMode &
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES;

        // Clean neutral card background — 30% badge on right carries the color
        int bg = isDark ? 0xFF18181C : 0xFFF5F5F7;
        int stroke = isDark ? 0xFF2A2A30 : 0xFFE0E0E5;

        cardBg.setColor(bg);
        cardBg.setStroke(dpToPx(getContext(), 1), stroke);

        setBackground(cardBg);
    }

    private static int dpToPx(Context context, float dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5f);
    }
}
