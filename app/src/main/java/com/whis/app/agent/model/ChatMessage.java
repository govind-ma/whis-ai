package com.whis.app.agent.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Chat message model for AI Agent conversations (AI_AGENT_PLAN.md Section 4.2).
 */
public class ChatMessage {

    public static final String ROLE_USER = "user";
    public static final String ROLE_AGENT = "agent";

    public String role;             // "user" | "agent"
    public String content;
    public long timestamp;
    public RiskLevel riskLevel;
    public List<String> optionButtons;

    public ChatMessage() {
        this.timestamp = System.currentTimeMillis();
        this.optionButtons = new ArrayList<>();
        this.riskLevel = RiskLevel.LOW;
    }

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
        this.optionButtons = new ArrayList<>();
        this.riskLevel = RiskLevel.LOW;
    }

    public ChatMessage(String role, String content, RiskLevel riskLevel, List<String> optionButtons) {
        this.role = role;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
        this.riskLevel = riskLevel != null ? riskLevel : RiskLevel.LOW;
        this.optionButtons = optionButtons != null ? optionButtons : new ArrayList<>();
    }
}
