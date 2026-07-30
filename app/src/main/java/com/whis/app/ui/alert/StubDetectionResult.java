package com.whis.app.ui.alert;

import com.whis.app.core.ConfidenceSource;
import com.whis.app.core.DetectionResult;
import com.whis.app.core.WhisVerdict;

/**
 * Concrete {@link DetectionResult} implementation used by the alert detail screen (UI_PLAN.md §3.3).
 * <p>
 * Carries real detection data passed from the Calls/Messages feed to {@link AlertDetailFragment}.
 * The former {@code createSampleSet()} factory has been removed — feeds now start empty on fresh
 * install and populate only from real AI-analysed data.
 */
public class StubDetectionResult implements DetectionResult {

    private final String sourceType;
    private final int riskScore;
    private final WhisVerdict verdict;
    private final String reasonText;
    private final String identifierType;
    private final ConfidenceSource confidenceSource;
    private final long timestamp;

    public StubDetectionResult(String sourceType, int riskScore, WhisVerdict verdict,
                               String reasonText, String identifierType,
                               ConfidenceSource confidenceSource, long timestamp) {
        this.sourceType = sourceType;
        this.riskScore = riskScore;
        this.verdict = verdict;
        this.reasonText = reasonText;
        this.identifierType = identifierType;
        this.confidenceSource = confidenceSource;
        this.timestamp = timestamp;
    }

    @Override public String getSourceType()       { return sourceType; }
    @Override public int    getRiskScore()         { return riskScore; }
    @Override public WhisVerdict getVerdict()      { return verdict; }
    @Override public String getReasonText()        { return reasonText; }
    @Override public String getIdentifierType()    { return identifierType; }
    @Override public long   getTimestamp()         { return timestamp; }
    @Override public ConfidenceSource getConfidenceSource() { return confidenceSource; }
}
