package com.whis.app.agent.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Fraud scenario model for offline knowledge base (AI_AGENT_PLAN.md Section 4.2).
 */
public class FraudScenario {

    public String id;
    public List<String> keywords;
    public RiskLevel risk;
    public String responseHindi;
    public String responseGujarati;
    public String responseEnglish;
    public List<String> nextStepsHindi;
    public List<String> nextStepsGujarati;
    public List<String> nextStepsEnglish;
    public boolean escalate;

    public FraudScenario() {
        this.keywords = new ArrayList<>();
        this.nextStepsHindi = new ArrayList<>();
        this.nextStepsGujarati = new ArrayList<>();
        this.nextStepsEnglish = new ArrayList<>();
        this.risk = RiskLevel.MEDIUM;
    }
}
