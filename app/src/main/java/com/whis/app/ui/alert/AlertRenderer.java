package com.whis.app.ui.alert;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.whis.app.R;
import com.whis.app.core.ConfidenceSource;
import com.whis.app.core.DetectionResult;
import com.whis.app.core.WhisVerdict;
import com.whis.app.ui.components.PrimaryButton;
import com.whis.app.ui.components.RiskTag;

/**
 * Shared AlertRenderer utility (UI_PLAN.md §1.6 / §2.1 / §3.3).
 * <p>
 * Enforces cross-module consistency across Call and Message alerts:
 * <ul>
 *   <li>Maps {@link WhisVerdict} to design token colors and {@link RiskTag}</li>
 *   <li>Enforces certainty language rules (UI_PLAN.md §1.6) — only {@link ConfidenceSource#CONTACT_MATCH}
 *       or {@link ConfidenceSource#VERIFIED_SERIES} may use "verified" / "confirmed" wording</li>
 *   <li>Renders alert UI as a bottom sheet with <b>exactly two action buttons</b> (Truecaller precedent)</li>
 *   <li>Provides pre-styled {@link NotificationCompat.Builder} for heads-up notification fallbacks</li>
 * </ul>
 */
public final class AlertRenderer {

    public static final String CHANNEL_ID_WHIS_ALERTS = "whis_scam_alerts";

    private AlertRenderer() {
        // Utility class
    }

    /**
     * Interface for bottom sheet action callbacks.
     */
    public interface AlertActionListener {
        /** Fired when primary button is clicked (e.g. Block / Warn). */
        void onPrimaryAction();

        /** Fired when secondary button is clicked (e.g. Ask Whis AI). */
        void onSecondaryAction();
    }

    /**
     * Formats alert explanation copy applying the confidence source language rules (UI_PLAN.md §1.6).
     *
     * @param result the detection result
     * @return non-null formatted string
     */
    @NonNull
    public static String formatAlertCopy(@NonNull DetectionResult result) {
        ConfidenceSource source = extractConfidenceSource(result);
        String reason = result.getReasonText();

        if (reason == null || reason.isEmpty()) {
            reason = "Unclassified activity detected.";
        }

        switch (source) {
            case CONTACT_MATCH:
                return "Confirmed contact: " + reason;

            case VERIFIED_SERIES:
                return "Verified sender: " + reason;

            case COMMUNITY_REPORT:
                return "Reported by other users: " + reason;

            case PATTERN_MATCH:
                return "Matches a known pattern: " + reason;

            case AI_ANALYSIS:
                return "Flagged by danger analysis: " + reason;

            default:
                return "Notice: " + reason;
        }
    }

    /**
     * Builds and displays a standard scam alert {@link BottomSheetDialog} with exactly 2 buttons (UI_PLAN.md §2.1).
     *
     * @param context  context
     * @param result   the detection result to render
     * @param listener callback listener for primary/secondary actions
     * @return the shown {@link BottomSheetDialog}
     */
    @NonNull
    public static BottomSheetDialog showBottomSheetAlert(@NonNull Context context,
                                                           @NonNull DetectionResult result,
                                                           @Nullable AlertActionListener listener) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_alert, null);
        
        // Apply capability-gated glassmorphic treatment (UI_PLAN.md §1.2 / §1.4)
        com.whis.app.ui.theme.GlassTreatment.applyGlassOrSolidSurface(view, context, 16f, 4f);

        dialog.setContentView(view);

        RiskTag riskTag = view.findViewById(R.id.alert_risk_tag);
        TextView tvTitle = view.findViewById(R.id.alert_title);
        TextView tvCopy = view.findViewById(R.id.alert_copy);
        TextView tvScore = view.findViewById(R.id.alert_score);
        PrimaryButton btnPrimary = view.findViewById(R.id.btn_alert_primary);
        Button btnSecondary = view.findViewById(R.id.btn_alert_secondary);

        WhisVerdict verdict = result.getVerdict();
        riskTag.setVerdict(verdict);

        String reason = result.getReasonText() != null ? result.getReasonText() : "";

        if (verdict == WhisVerdict.TRUSTED || "CONTACT".equalsIgnoreCase(result.getIdentifierType())) {
            tvTitle.setText("🟢 Saved Contact");
            tvCopy.setText(reason.isEmpty() ? "Saved contact in your address book. 100% verified safe." : reason);
            tvScore.setText("Risk Score: 0/100 (Safe)");
            btnPrimary.setButtonColor(R.color.whis_trusted);
            btnPrimary.setText("Dismiss");
        } else if (verdict == WhisVerdict.HIGH_RISK) {
            tvTitle.setText("🚨 Cyber Scam Analysis");
            tvCopy.setText("🚨 SCAM DIAGNOSIS & REASON:\n\n" + reason);
            tvScore.setText("Risk Score: " + result.getRiskScore() + "/100 (HIGH RISK)");
            btnPrimary.setButtonColor(R.color.whis_high_risk);
            btnPrimary.setText("Block & Protect");
        } else {
            tvTitle.setText("ℹ️ Unknown Number Details");
            tvCopy.setText("Standard activity from an un-saved number.\n\nSummary: " + (reason.isEmpty() ? "No financial scam patterns detected." : reason));
            tvScore.setText("Risk Score: " + result.getRiskScore() + "/100 (Neutral)");
            btnPrimary.setButtonColor(R.color.whis_suspicious);
            btnPrimary.setText("Dismiss");
        }

        btnSecondary.setText("Ask Whis AI");

        // Exactly two buttons — click handlers
        btnPrimary.setOnClickListener(v -> {
            dialog.dismiss();
            if (listener != null) listener.onPrimaryAction();
        });

        btnSecondary.setOnClickListener(v -> {
            dialog.dismiss();
            if (listener != null) listener.onSecondaryAction();
        });

        dialog.show();
        return dialog;
    }

    /**
     * Creates a {@link NotificationCompat.Builder} pre-styled with Whis visual tokens and copy rules (UI_PLAN.md §3.3).
     *
     * @param context context
     * @param result  the detection result
     * @return pre-configured notification builder
     */
    @NonNull
    public static NotificationCompat.Builder createNotificationBuilder(@NonNull Context context,
                                                                         @NonNull DetectionResult result) {
        ensureNotificationChannel(context);

        WhisVerdict verdict = result.getVerdict();
        String title = verdictToTitle(verdict, result.getSourceType());
        String contentText = formatAlertCopy(result);

        int colorRes = verdictToColorRes(verdict);
        int color = context.getResources().getColor(colorRes, context.getTheme());

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_WHIS_ALERTS)
                .setSmallIcon(R.drawable.ic_nav_home)
                .setContentTitle(title)
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText))
                .setColor(color)
                .setPriority(verdict == WhisVerdict.HIGH_RISK ? NotificationCompat.PRIORITY_MAX : NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        return builder;
    }

    private static ConfidenceSource extractConfidenceSource(DetectionResult result) {
        if (result instanceof StubDetectionResult) {
            return ((StubDetectionResult) result).getConfidenceSource();
        }
        // Default heuristics for generic DetectionResult
        if (result.getVerdict() == WhisVerdict.TRUSTED) {
            return ConfidenceSource.CONTACT_MATCH;
        } else if (result.getVerdict() == WhisVerdict.LIKELY_SAFE) {
            return ConfidenceSource.VERIFIED_SERIES;
        }
        return ConfidenceSource.PATTERN_MATCH;
    }

    private static String verdictToTitle(WhisVerdict verdict, String sourceType) {
        String sourceLabel = "CALL".equalsIgnoreCase(sourceType) ? "Call" : "Message";

        switch (verdict) {
            case HIGH_RISK:   return "DANGEROUS " + sourceLabel.toUpperCase() + " ALERT";
            case SUSPICIOUS:  return "Suspicious " + sourceLabel + " Detected";
            case LIKELY_SAFE: return "Likely Safe " + sourceLabel;
            case TRUSTED:     return "Trusted " + sourceLabel;
            default:          return "Screened " + sourceLabel;
        }
    }

    private static int verdictToColorRes(WhisVerdict verdict) {
        switch (verdict) {
            case TRUSTED:     return R.color.whis_trusted;
            case LIKELY_SAFE: return R.color.whis_likely_safe;
            case UNKNOWN:     return R.color.whis_unknown;
            case SUSPICIOUS:  return R.color.whis_suspicious;
            case HIGH_RISK:   return R.color.whis_high_risk;
            default:          return R.color.whis_unknown;
        }
    }

    private static void ensureNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null && nm.getNotificationChannel(CHANNEL_ID_WHIS_ALERTS) == null) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID_WHIS_ALERTS,
                        "Whis Scam Alerts",
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("Critical notifications for screened call and message scam warnings.");
                nm.createNotificationChannel(channel);
            }
        }
    }
}
