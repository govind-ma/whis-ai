package com.whis.app.agent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.whis.app.agent.context.UserProfileContext;
import com.whis.app.agent.emergency.RedAlertManager;
import com.whis.app.agent.model.ChatMessage;
import com.whis.app.agent.model.RiskLevel;
import com.whis.app.agent.model.UserProfile;
import com.whis.app.agent.offline.ScenarioMatcher;
import com.whis.app.agent.prompt.SystemPromptBuilder;

import org.junit.Test;

import java.util.ArrayList;

/**
 * Acceptance criteria unit tests for AI Chat Agent Module.
 */
public class AgentUnitTest {

    @Test
    public void testSystemPromptHardcodedFacts() {
        UserProfile profile = new UserProfile(
                "Ramesh Kumar", "60+", "Senior", "Hindi", "Basic", "PhonePe", "SBI", "9876543210"
        );

        String prompt = SystemPromptBuilder.build(null, profile, new ArrayList<>());

        assertNotNull(prompt);
        // Print assembled prompt for acceptance criteria verification
        System.out.println("=== ASSEMBLED SYSTEM PROMPT ===");
        System.out.println(prompt);
        System.out.println("===============================");

        // Verify hardcoded facts exist in assembled prompt
        assertTrue("Must contain 1930 Cyber Crime Helpline", prompt.contains("1930"));
        assertTrue("Must contain bank helpline", prompt.contains("1800-11-2211"));
        assertTrue("Must contain RBI circular citation", prompt.contains("DBR.No.Leg.BC.78/09.07.005/2017-18"));
        assertTrue("Must contain BNS legal citation", prompt.contains("Section 318"));
        assertTrue("Must contain IT Act citation", prompt.contains("Section 66D"));
        assertTrue("Must contain user name", prompt.contains("Ramesh Kumar"));
    }

    @Test
    public void testOfflineScenarioMatcherFallback() {
        // Test offline scenario matching when network is unavailable
        ChatMessage response = ScenarioMatcher.findResponse(null, "Mera UPI collect request approve ho gaya", "Hindi");
        assertNotNull(response);
        assertEquals(ChatMessage.ROLE_AGENT, response.role);
        assertTrue("Response text should be coherent offline guidance", response.content.contains("UPI"));
        assertTrue("Should include option buttons", response.optionButtons.size() > 0);
    }

    @Test
    public void testRedAlertInstantTrigger() {
        // Test Red Alert instant API-free trigger on critical keywords
        RiskLevel risk = RedAlertManager.assess("CBI officer line pe hai digital arrest kar rahe hain");
        assertEquals("Critical keyword must trigger RiskLevel.CRITICAL instantly", RiskLevel.CRITICAL, risk);
    }

    @Test
    public void testAgentLauncherSignatures() {
        // Confirm AgentLauncher signatures compile and exist
        assertNotNull(AgentLauncher.class);
    }
}
