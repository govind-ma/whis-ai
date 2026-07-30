package com.whis.app.agent.api;

import com.whis.app.agent.model.RiskLevel;

import java.util.ArrayList;
import java.util.List;

/**
 * Parsed response container from Gemini AI calls (AI_AGENT_PLAN.md Section 4.2 Day 6).
 */
public class AgentResponse {

    private String message;
    private RiskLevel riskLevel;
    private List<String> optionButtons;
    private List<String> actionFlags;

    public AgentResponse() {
        this.message = "";
        this.riskLevel = RiskLevel.LOW;
        this.optionButtons = new ArrayList<>();
        this.actionFlags = new ArrayList<>();
    }

    public AgentResponse(String message, RiskLevel riskLevel, List<String> optionButtons, List<String> actionFlags) {
        this.message = message != null ? message : "";
        this.riskLevel = riskLevel != null ? riskLevel : RiskLevel.LOW;
        this.optionButtons = optionButtons != null ? optionButtons : new ArrayList<>();
        this.actionFlags = actionFlags != null ? actionFlags : new ArrayList<>();
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }

    public List<String> getOptionButtons() { return optionButtons; }
    public List<String> getActionFlags() { return actionFlags; }
}
