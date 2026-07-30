package com.whis.app.msg.model;

import com.whis.app.core.ConfidenceSource;
import com.whis.app.core.DetectionResult;
import com.whis.app.core.WhisVerdict;

/**
 * Shared detection result for SMS message analysis — MSG_PLAN.md Section 4.1.
 * <p>
 * Implements {@link DetectionResult} from {@code com.whis.app.core} per
 * MASTER_PLAN.md Section 3.1, 3.2 & 7.1.
 */
public class MsgDetectionResult implements DetectionResult {

    public String sender;
    public MsgCategory category;
    public String threatLevel;          // "SAFE" | "SUSPICIOUS" | "SCAM"
    public int riskScore;               // 0–100. HIGHER = MORE dangerous. 0=safe, 100=scam.
    public WhisVerdict verdict;         // TRUSTED | LIKELY_SAFE | UNKNOWN | SUSPICIOUS | HIGH_RISK
    public String reasonText;           // Primary human-readable reason
    public String identifierType;       // "CONTACT" | "DLT_TRANSACTIONAL" | "DLT_SERVICE" | "DLT_PROMOTIONAL" | "UNKNOWN_HEADER" | etc.
    public long timestamp;
    public int layersUsed;              // Bitmask of layers that contributed
    public boolean containsUrl;
    public String expandedUrl;
    public boolean dltVerified;
    public boolean isContact;
    public boolean isGeminiEvaluated;

    public MsgDetectionResult() {
        this.timestamp = System.currentTimeMillis();
        this.category = MsgCategory.GENERAL;
        this.threatLevel = "SAFE";
        this.verdict = WhisVerdict.UNKNOWN;
        this.identifierType = "UNKNOWN_HEADER";
        this.riskScore = 0;
    }

    /**
     * Map internal threat level and DLT status to canonical {@link WhisVerdict}.
     */
    public void resolveVerdict(boolean isContact) {
        this.isContact = isContact;
        if ("SCAM".equalsIgnoreCase(threatLevel) || riskScore >= 75) {
            this.verdict = WhisVerdict.HIGH_RISK;
            this.threatLevel = "SCAM";
        } else if ("SUSPICIOUS".equalsIgnoreCase(threatLevel) || (riskScore >= 40 && riskScore < 75)) {
            this.verdict = WhisVerdict.SUSPICIOUS;
            this.threatLevel = "SUSPICIOUS";
        } else {
            this.threatLevel = "SAFE";
            if (isContact) {
                this.verdict = WhisVerdict.TRUSTED;
            } else if (dltVerified) {
                this.verdict = WhisVerdict.LIKELY_SAFE;
            } else {
                this.verdict = WhisVerdict.LIKELY_SAFE;
            }
        }
    }

    // ── DetectionResult Interface Implementation ────────────────────────────

    @Override
    public String getSourceType() {
        return "SMS";
    }

    @Override
    public int getRiskScore() {
        if (riskScore < 0) return 0;
        if (riskScore > 100) return 100;
        return riskScore;
    }

    @Override
    public WhisVerdict getVerdict() {
        return verdict != null ? verdict : WhisVerdict.UNKNOWN;
    }

    @Override
    public String getReasonText() {
        return reasonText != null ? reasonText : "";
    }

    @Override
    public String getIdentifierType() {
        return identifierType != null ? identifierType : "";
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Map confidence source per MASTER_PLAN.md Section 7.1:
     * DLT header -> VERIFIED_SERIES
     * contact -> CONTACT_MATCH
     * Gemini-resolved -> AI_ANALYSIS
     * else -> PATTERN_MATCH
     */
    @Override
    public ConfidenceSource getConfidenceSource() {
        if (isContact || "CONTACT".equalsIgnoreCase(identifierType)) {
            return ConfidenceSource.CONTACT_MATCH;
        } else if (dltVerified || (identifierType != null && identifierType.startsWith("DLT_"))) {
            return ConfidenceSource.VERIFIED_SERIES;
        } else if (isGeminiEvaluated || (layersUsed & (1 << 4)) != 0) {
            return ConfidenceSource.AI_ANALYSIS;
        } else {
            return ConfidenceSource.PATTERN_MATCH;
        }
    }
}
