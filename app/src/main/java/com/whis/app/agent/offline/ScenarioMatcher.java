package com.whis.app.agent.offline;

import android.content.Context;

import com.whis.app.agent.model.ChatMessage;
import com.whis.app.agent.model.RiskLevel;

import java.util.ArrayList;
import java.util.List;

/**
 * Offline Scenario Matcher (AI_AGENT_PLAN.md Section 4.2).
 * <p>
 * Evaluates user input against offline scenarios to produce an immediate, coherent
 * response in English or Hinglish based on language preference and user input.
 */
public class ScenarioMatcher {

    private ScenarioMatcher() {
        // Utility class
    }

    public static ChatMessage findResponse(Context context, String userMessage, String language) {
        if (userMessage == null) userMessage = "";
        String msg = userMessage.toLowerCase().trim();

        boolean isEnglish = (language != null && language.equalsIgnoreCase("English"))
                || (msg.matches(".*[a-zA-Z].*") && !msg.contains("hai") && !msg.contains("karo") && !msg.contains("mat"));

        String responseText;
        List<String> options = new ArrayList<>();
        RiskLevel risk;

        // ── 1. Complain / 1930 Helpline Procedure ("complain", "coplain", "how to complain", "1930")
        if (msg.contains("complain") || msg.contains("coplain") || msg.contains("1930") || msg.contains("how to")) {
            if (isEnglish) {
                responseText = "To report fraud on 1930: Tap 'Call 1930 Helpline' below, state your bank name, mobile number, and transaction ID. The helpline officer will freeze the stolen funds in the banking system.";
                options.add("Call 1930 Helpline");
                options.add("Call Bank Helpline");
            } else {
                responseText = "1930 pe complaint karne ke liye: 'Call 1930 Helpline' dabaayein, apna bank naam aur transaction ID batayein. Officer turant paise freeze kar dega.";
                options.add("1930 pe call karo");
                options.add("Bank helpline");
            }
            risk = RiskLevel.HIGH;

        // ── 2. Sent Money to Wrong / Different Number ("different number", "send money", "sent money", "wrong number")
        } else if (msg.contains("different number") || msg.contains("send money") || msg.contains("sent money") || msg.contains("wrong number")) {
            if (isEnglish) {
                responseText = "If you sent money to an unknown or scammer's number, call 1930 immediately to freeze the receiver's account. Also open your UPI app (GPay/PhonePe/Paytm) and raise a transaction dispute.";
                options.add("Call 1930 Helpline");
                options.add("Call Bank Helpline");
            } else {
                responseText = "Agar galat ya scammer ke number pe paise chale gaye hain, toh turant 1930 pe call karke receiver ka account freeze karwayein. Apni UPI app mein dispute raise karein.";
                options.add("1930 pe call karo");
                options.add("Bank helpline");
            }
            risk = RiskLevel.CRITICAL;

        // ── 3. Scam / Money Loss ("scam", "scammed", "money", "rupees", "lost", "stolen", "gone")
        } else if (msg.contains("scam") || msg.contains("money") || msg.contains("rupees")
                || msg.contains("lost") || msg.contains("stolen") || msg.contains("gone")) {

            if (isEnglish) {
                responseText = "If you have lost money to a scam, call the National Cyber Crime Helpline 1930 immediately to freeze the transaction. Also report to your bank helpline to block your account/card.";
                options.add("Call 1930 Helpline");
                options.add("Call Bank Helpline");
            } else {
                responseText = "Agar aapke paise scam mein chale gaye hain, toh turant 1930 Cyber Crime Helpline pe call karke transaction freeze karwayein, aur apni bank helpline ko report karein.";
                options.add("1930 pe call karo");
                options.add("Bank helpline");
            }
            risk = RiskLevel.CRITICAL;

        // ── 4. UPI / Payment / Transfer ──────────────────────────────────────
        } else if (msg.contains("upi") || msg.contains("payment") || msg.contains("transfer") || msg.contains("qr")) {

            if (isEnglish) {
                responseText = "UPI fraud is very common. Never pay or enter your UPI PIN to an unknown person. Remember: UPI PIN is ONLY used to SEND money, never to receive money. Call 1930 if you transferred funds.";
                options.add("Call 1930 Helpline");
                options.add("Call Bank Helpline");
            } else {
                responseText = "UPI fraud bahut common hai. Kisi bhi unknown person ko UPI se payment mat karo. Yaad rakho: PIN sirf paise BHEJNE ke liye hota hai, LENE ke liye nahi. Agar transfer kiya hai toh 1930 pe call karo.";
                options.add("1930 pe call karo");
                options.add("Bank helpline");
            }
            risk = RiskLevel.HIGH;

        // ── 5. Call / Phone / Police / Arrest (Digital Arrest scam) ──────────
        } else if (msg.contains("call") || msg.contains("phone") || msg.contains("police") || msg.contains("arrest")) {

            if (isEnglish) {
                responseText = "This sounds like a Digital Arrest scam. Real Police, CBI, or Law Enforcement NEVER arrest anyone over video call or demand money. Disconnect immediately and call 1930.";
                options.add("Disconnect Call");
                options.add("Call 1930 Helpline");
            } else {
                responseText = "Yeh Digital Arrest scam lag raha hai. Real police ya CBI kabhi video call pe arrest nahi karti. Turant call disconnect karo aur 1930 dial karo.";
                options.add("Call disconnect karo");
                options.add("1930 dial karo");
            }
            risk = RiskLevel.CRITICAL;

        // ── 6. OTP / Link / Click (Phishing) ─────────────────────────────────
        } else if (msg.contains("otp") || msg.contains("link") || msg.contains("click")) {

            if (isEnglish) {
                responseText = "Never share any OTP or click suspicious links. Banks and police never ask for OTP or password over SMS/call.";
                options.add("Never Share OTP");
                options.add("Report on 1930");
            } else {
                responseText = "Koi bhi OTP share mat karo — na bank ko, na police ko, na kisi bhi stranger ko. Suspicious link pe click mat karo.";
                options.add("OTP share mat karo");
                options.add("1930 pe report karo");
            }
            risk = RiskLevel.HIGH;

        // ── 7. KYC / Account / Block ──────────────────────────────────────────
        } else if (msg.contains("kyc") || msg.contains("account") || msg.contains("block")) {

            if (isEnglish) {
                responseText = "This may be a KYC scam. Banks never ask for KYC updates over SMS or unsaved calls. Visit your official bank branch to verify.";
                options.add("Visit Bank Branch");
                options.add("Call Bank Helpline");
            } else {
                responseText = "Yeh KYC scam ho sakta hai. Bank kabhi call/SMS pe KYC nahi maangta. Apni bank branch pe jaake verify karo.";
                options.add("Branch pe jaao");
                options.add("Bank helpline call karo");
            }
            risk = RiskLevel.HIGH;

        // ── 8. Default Fallback ───────────────────────────────────────────────
        } else {
            if (isEnglish) {
                responseText = "I am ready to help you. Ask me about any scam message, UPI fraud, fake call, or call the 1930 Cyber Crime Helpline for immediate emergency assistance.";
                options.add("Call 1930 Helpline");
                options.add("View Bank Helplines");
            } else {
                responseText = "Main aapki madad karne ke liye taiyar hoon. UPI, Call, OTP, ya KYC ke baare mein poochein, ya kisi bhi fraud ke liye 1930 Cyber Helpline pe call karein.";
                options.add("1930 pe call karo");
                options.add("Bank helpline dekhein");
            }
            risk = RiskLevel.MEDIUM;
        }

        return new ChatMessage(ChatMessage.ROLE_AGENT, responseText, risk, options);
    }
}
