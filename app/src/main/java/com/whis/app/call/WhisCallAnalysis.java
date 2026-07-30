package com.whis.app.call;

import com.whis.app.core.ConfidenceSource;
import com.whis.app.core.DetectionResult;
import com.whis.app.core.WhisVerdict;

/**
 * Call screening detection result — MASTER_PLAN.md Section 3.1 & 7.1.
 * Implements {@link DetectionResult} from {@code com.whis.app.core}.
 */
public class WhisCallAnalysis implements DetectionResult {

    public String incomingNumber;
    public int riskScore;              // 0=safe, 100=scam
    public WhisVerdict verdict;        // TRUSTED | LIKELY_SAFE | UNKNOWN | SUSPICIOUS | HIGH_RISK
    public String reasonText;          // Human-readable reason
    public String identifierType;      // "CONTACT" | "1600_SERIES" | "UNKNOWN_MOBILE" | etc.
    public long timestamp;

    public boolean wasInContacts;
    public boolean is1600Series;
    public int communityReportCount;

    public WhisCallAnalysis() {
        this.timestamp = System.currentTimeMillis();
        this.verdict = WhisVerdict.UNKNOWN;
        this.identifierType = "UNKNOWN_MOBILE";
        this.riskScore = 50;
    }

    public WhisCallAnalysis(String incomingNumber, int riskScore, WhisVerdict verdict,
                            String reasonText, String identifierType, boolean wasInContacts,
                            boolean is1600Series, int communityReportCount) {
        this.incomingNumber = incomingNumber;
        this.riskScore = riskScore;
        this.verdict = verdict;
        this.reasonText = reasonText;
        this.identifierType = identifierType;
        this.wasInContacts = wasInContacts;
        this.is1600Series = is1600Series;
        this.communityReportCount = communityReportCount;
        this.timestamp = System.currentTimeMillis();
    }

    @Override
    public String getSourceType() {
        return "CALL";
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
     * wasInContacts -> CONTACT_MATCH
     * 1600/1601 series -> VERIFIED_SERIES
     * communityReportCount > 0 -> COMMUNITY_REPORT
     * else -> PATTERN_MATCH
     */
    @Override
    public ConfidenceSource getConfidenceSource() {
        if (wasInContacts) {
            return ConfidenceSource.CONTACT_MATCH;
        } else if (is1600Series || "1600_SERIES".equalsIgnoreCase(identifierType)) {
            return ConfidenceSource.VERIFIED_SERIES;
        } else if (communityReportCount > 0 || "COMMUNITY_REPORT".equalsIgnoreCase(identifierType)) {
            return ConfidenceSource.COMMUNITY_REPORT;
        } else {
            return ConfidenceSource.PATTERN_MATCH;
        }
    }
}
