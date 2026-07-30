package com.whis.app.ui.alert;

import com.whis.app.core.ConfidenceSource;
import com.whis.app.core.WhisVerdict;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit test verifying AlertRenderer language rules and stub dataset generation.
 */
public class AlertRendererTest {

    @Test
    public void testStubDatasetGeneration() {
        List<StubDetectionResult> samples = StubDetectionResult.createSampleSet();
        assertEquals(6, samples.size());
    }

    @Test
    public void testCertaintyLanguageRules() {
        // CONTACT_MATCH and VERIFIED_SERIES must use strict certainty words
        StubDetectionResult contact = new StubDetectionResult(
                "CALL", 0, WhisVerdict.TRUSTED, "Mom", "CONTACT", ConfidenceSource.CONTACT_MATCH, System.currentTimeMillis()
        );
        String contactCopy = AlertRenderer.formatAlertCopy(contact);
        assertTrue(contactCopy.contains("Confirmed contact"));

        StubDetectionResult dlt = new StubDetectionResult(
                "SMS", 10, WhisVerdict.LIKELY_SAFE, "HDFC Bank", "DLT", ConfidenceSource.VERIFIED_SERIES, System.currentTimeMillis()
        );
        String dltCopy = AlertRenderer.formatAlertCopy(dlt);
        assertTrue(dltCopy.contains("Verified sender"));

        // COMMUNITY_REPORT, PATTERN_MATCH, AI_ANALYSIS must use softer wording
        StubDetectionResult comm = new StubDetectionResult(
                "CALL", 80, WhisVerdict.HIGH_RISK, "Scam reports", "REPORTS", ConfidenceSource.COMMUNITY_REPORT, System.currentTimeMillis()
        );
        String commCopy = AlertRenderer.formatAlertCopy(comm);
        assertTrue(commCopy.contains("Reported by other users"));

        StubDetectionResult pattern = new StubDetectionResult(
                "CALL", 70, WhisVerdict.SUSPICIOUS, "Unknown series", "UNKNOWN", ConfidenceSource.PATTERN_MATCH, System.currentTimeMillis()
        );
        String patternCopy = AlertRenderer.formatAlertCopy(pattern);
        assertTrue(patternCopy.contains("Matches a known pattern"));

        StubDetectionResult ai = new StubDetectionResult(
                "SMS", 95, WhisVerdict.HIGH_RISK, "Phishing link", "LINK", ConfidenceSource.AI_ANALYSIS, System.currentTimeMillis()
        );
        String aiCopy = AlertRenderer.formatAlertCopy(ai);
        assertTrue(aiCopy.contains("Flagged by danger analysis"));
    }
}
