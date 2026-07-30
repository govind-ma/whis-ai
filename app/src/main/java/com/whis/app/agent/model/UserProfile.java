package com.whis.app.agent.model;

/**
 * User profile model captured during onboarding (AI_AGENT_PLAN.md Section 4.2 & Adjustment #2).
 */
public class UserProfile {

    public String name;
    public String ageGroup;        // "18-25", "26-40", "41-60", "60+"
    public String occupation;      // "Student", "Professional", "Business", "Housewife", "Senior"
    public String language;        // "Hindi", "Gujarati", "English"
    public String techLevel;       // "Basic", "Intermediate", "Advanced"
    public String primaryUpi;      // "Google Pay", "PhonePe", "Paytm", "BHIM", "Bank App"
    public String bankName;        // "SBI", "HDFC", "ICICI", "Axis", "Kotak", etc.
    public String emergencyContact; // Phone number or null

    public UserProfile() {
        this.name = "User";
        this.ageGroup = "26-40";
        this.occupation = "Professional";
        this.language = "Hindi";
        this.techLevel = "Basic";
        this.primaryUpi = "Google Pay";
        this.bankName = "SBI";
        this.emergencyContact = null;
    }

    public UserProfile(String name, String ageGroup, String occupation, String language,
                       String techLevel, String primaryUpi, String bankName, String emergencyContact) {
        this.name = name;
        this.ageGroup = ageGroup;
        this.occupation = occupation;
        this.language = language;
        this.techLevel = techLevel;
        this.primaryUpi = primaryUpi;
        this.bankName = bankName;
        this.emergencyContact = emergencyContact;
    }
}
