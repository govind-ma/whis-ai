package com.whis.app.msg.engine;

import android.content.Context;

import com.whis.app.core.WhisFlags;
import com.whis.app.msg.model.MsgDetectionResult;

/**
 * Weighted Score Engine Orchestrator (MSG_PLAN.md Section 4.2 Step 8 & 9).
 * <p>
 * Sequentially evaluates Layers 1 through 5:
 * <ol>
 *   <li>Layer 1: Header format / Contact check</li>
 *   <li>Layer 2: Weighted rule engine &amp; OTP whitelisting</li>
 *   <li>Layer 3: On-device TFLite ML classifier</li>
 *   <li>Layer 4: Consent-gated URL reputation check</li>
 *   <li>Layer 5: Rate-gated Gemini cloud fallback</li>
 * </ol>
 * <p>
 * Writes to {@link WhisFlags} if final verdict is {@code SUSPICIOUS} or {@code HIGH_RISK}.
 */
public class WeightedScoreEngine {

    public static final int LAYER_1_HEADER = 1 << 0;
    public static final int LAYER_2_RULES = 1 << 1;
    public static final int LAYER_3_TFLITE = 1 << 2;
    public static final int LAYER_4_URL = 1 << 3;
    public static final int LAYER_5_GEMINI = 1 << 4;

    private WeightedScoreEngine() {
        // Utility class
    }

    /**
     * Analyze an incoming SMS message.
     *
     * @param context android context (nullable for unit tests)
     * @param sender  SMS sender header or phone number
     * @param body    SMS body text
     * @return populated {@link MsgDetectionResult}
     */
    public static MsgDetectionResult analyze(Context context, String sender, String body) {
        MsgDetectionResult result = new MsgDetectionResult();
        result.sender = (sender != null) ? sender.trim() : "";
        result.timestamp = System.currentTimeMillis();

        int layersUsed = 0;

        // ── LAYER 1: Header Format & Contact Check ─────────────────────────────
        layersUsed |= LAYER_1_HEADER;
        Layer1HeaderChecker.HeaderResult layer1 = Layer1HeaderChecker.check(context, result.sender);
        result.identifierType = layer1.identifierType;
        result.dltVerified = layer1.dltVerified;

        if (layer1.isContact) {
            result.category = layer1.categoryHint;
            result.riskScore = 0;
            result.threatLevel = "SAFE";
            result.reasonText = "Saved contact in your phonebook";
            result.layersUsed = layersUsed;
            result.resolveVerdict(true);
            return result;
        }

        // ── LAYER 2: Rule Engine & OTP Whitelisting ────────────────────────────
        layersUsed |= LAYER_2_RULES;
        Layer2RuleEngine.RuleResult layer2 = Layer2RuleEngine.analyze(context, result.sender, body, layer1);

        // Critical False-Positive OTP Whitelisting Exit
        if (layer2.isOtpWhitelisted) {
            result.category = layer2.category;
            result.riskScore = 0;
            result.threatLevel = "SAFE";
            result.reasonText = layer2.reason;
            result.layersUsed = layersUsed;
            result.resolveVerdict(false);
            return result;
        }

        int score = layer2.riskScore;
        result.category = layer2.category;
        result.reasonText = layer2.reason;

        // ── LAYER 3: On-Device ML Classifier (if ambiguous) ────────────────────
        if (score >= 20 && score <= 65) {
            layersUsed |= LAYER_3_TFLITE;
            Layer3TfliteClassifier.ClassificationResult layer3 = Layer3TfliteClassifier.classify(context, body);
            if (layer3.modelInferenceRan) {
                int mlContribution = Math.round(layer3.scamProbability * 40);
                score = Math.min(100, score + mlContribution);
            }
        }

        // ── LAYER 4: Consent-Gated URL Reputation Check ────────────────────────
        Layer4UrlChecker.UrlCheckResult layer4 = Layer4UrlChecker.check(context, body);
        if (layer4.containsUrl) {
            result.containsUrl = true;
            result.expandedUrl = layer4.expandedUrl;

            if (layer4.consentGiven) {
                layersUsed |= LAYER_4_URL;
                score = Math.min(100, score + layer4.riskDelta);
                if (layer4.isSuspiciousDomain) {
                    result.reasonText += " • Malicious or suspicious web domain link";
                }
            }
        }

        // ── LAYER 5: Gemini Cloud Analysis (runs for all non-trivially-safe SMS) ─
        if (context != null && score >= 30) {
            layersUsed |= LAYER_5_GEMINI;
            Layer5GeminiFallback.GeminiResult layer5 = Layer5GeminiFallback.evaluate(context, result.sender, body);
            if (layer5.ran) {
                result.isGeminiEvaluated = true;
                // Override score based on Gemini's verdict
                if ("SCAM".equalsIgnoreCase(layer5.verdict)) {
                    score = Math.max(score, 75);
                } else if ("SUSPICIOUS".equalsIgnoreCase(layer5.verdict)) {
                    score = Math.max(score, 40);
                } else if ("SAFE".equalsIgnoreCase(layer5.verdict) && score < 40) {
                    score = Math.min(score, 20); // Gemini says safe — trust it for borderline cases
                }
                // Adopt category from Gemini if it adds more specificity
                if (layer5.category != null && !"UNKNOWN".equals(layer5.category)) {
                    result.reasonText = result.reasonText + " • AI: " + layer5.reason;
                } else if (layer5.reason != null && !layer5.reason.isEmpty()) {
                    result.reasonText = result.reasonText + " • AI: " + layer5.reason;
                }
            }
        }

        // Finalize score & threat level
        result.riskScore = Math.min(100, Math.max(0, score));
        if (result.riskScore >= 70) {
            result.threatLevel = "SCAM";
        } else if (result.riskScore >= 40) {
            result.threatLevel = "SUSPICIOUS";
        } else {
            result.threatLevel = "SAFE";
        }

        result.layersUsed = layersUsed;
        result.resolveVerdict(false);

        // Write to whis_flags if SUSPICIOUS or HIGH_RISK (Adjustment #6)
        if (context != null && (result.verdict == com.whis.app.core.WhisVerdict.SUSPICIOUS || result.verdict == com.whis.app.core.WhisVerdict.HIGH_RISK)) {
            WhisFlags.addSmsFlag(context, result.sender, body, result.reasonText, result.verdict);
        }

        return result;
    }
}
