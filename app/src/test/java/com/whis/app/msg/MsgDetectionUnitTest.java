package com.whis.app.msg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.whis.app.core.WhisVerdict;
import com.whis.app.msg.engine.Layer1HeaderChecker;
import com.whis.app.msg.engine.Layer2RuleEngine;
import com.whis.app.msg.engine.WeightedScoreEngine;
import com.whis.app.msg.model.MsgCategory;
import com.whis.app.msg.model.MsgDetectionResult;

import org.junit.Test;

/**
 * Unit tests for MSG module scam detection pipeline and OTP false-positive whitelisting.
 */
public class MsgDetectionUnitTest {

    @Test
    public void testOtpFalsePositiveWhitelisting_DltSender() {
        // Test case specified in Acceptance Criteria:
        // A message containing ONLY a numeric OTP + bank name + expiry statement, sent from a properly
        // DLT-registered sender, MUST resolve to TRUSTED or LIKELY_SAFE — NEVER SUSPICIOUS or HIGH_RISK.

        String sender = "VM-SBIBNK-T";
        String body = "Your State Bank of India OTP is 482910. Valid for 10 minutes. Do not share with anyone.";

        Layer1HeaderChecker.HeaderResult layer1 = Layer1HeaderChecker.check(null, sender);
        assertTrue("Header should be verified DLT Transactional", layer1.dltVerified);
        assertEquals("DLT_TRANSACTIONAL", layer1.identifierType);

        Layer2RuleEngine.RuleResult layer2 = Layer2RuleEngine.analyze(null, sender, body, layer1);
        assertTrue("OTP message should be whitelisted", layer2.isOtpWhitelisted);
        assertEquals(0, layer2.riskScore);
        assertEquals("SAFE", layer2.threatLevel);

        MsgDetectionResult result = WeightedScoreEngine.analyze(null, sender, body);
        assertNotNull(result);
        assertEquals("SMS", result.getSourceType());
        assertEquals(0, result.getRiskScore());
        assertEquals(MsgCategory.TRANSACTION, result.category);
        assertEquals(WhisVerdict.LIKELY_SAFE, result.getVerdict());
        assertFalse("Must never be SUSPICIOUS or HIGH_RISK",
                result.getVerdict() == WhisVerdict.SUSPICIOUS || result.getVerdict() == WhisVerdict.HIGH_RISK);
    }

    @Test
    public void testScamMessageDetection_UrgentPhishing() {
        String sender = "9876543210";
        String body = "Your SBI account is blocked! Update KYC immediately at http://bit.ly/sbi-kyc or call 9876543210 turant.";

        MsgDetectionResult result = WeightedScoreEngine.analyze(null, sender, body);
        assertNotNull(result);
        assertEquals("SMS", result.getSourceType());
        assertTrue("Risk score should be high for phishing scam", result.getRiskScore() >= 70);
        assertEquals(WhisVerdict.HIGH_RISK, result.getVerdict());
        assertEquals("SCAM", result.threatLevel);
    }
}
