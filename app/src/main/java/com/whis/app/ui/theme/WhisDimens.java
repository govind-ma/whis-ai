package com.whis.app.ui.theme;

/**
 * Java constants mirroring {@code dimens.xml} design tokens for programmatic use.
 * <p>
 * Source of truth: UI_PLAN.md §2.1 — Spacing (8dp grid) and Touch targets.
 * <p>
 * Values are in <b>dp</b> (density-independent pixels). Convert to px at runtime via
 * {@code TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, metrics)}.
 * <p>
 * Prefer the XML resource ({@code @dimen/whis_*}) in layouts; use these constants
 * only in code paths that cannot reference resources (e.g. custom View math,
 * programmatic layout params).
 */
public final class WhisDimens {

    private WhisDimens() {
        // Static constants only — do not instantiate.
    }

    // ── Spacing (8dp base grid) ──────────────────────────────────────────
    /** Base grid unit. */
    public static final int SPACING_BASE = 8;
    /** Inter-item spacing within a list or group. */
    public static final int SPACING_ITEM = 12;
    /** Internal padding inside cards. */
    public static final int SPACING_CARD_PADDING = 16;
    /** Screen-edge horizontal/vertical margin. */
    public static final int SPACING_SCREEN_MARGIN = 20;
    /** Vertical spacing between major sections. */
    public static final int SPACING_SECTION = 24;

    // ── Touch targets ────────────────────────────────────────────────────
    /** Absolute minimum interactive element size (Google/WCAG guideline). */
    public static final int TOUCH_FLOOR = 48;
    /** Standard primary-action button / bottom-nav item size. */
    public static final int TOUCH_STANDARD = 56;
    /** Minimum list-row height. */
    public static final int LIST_ROW_MIN_HEIGHT = 56;
}
