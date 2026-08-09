package com.whis.app.ui.learn;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.whis.app.agent.api.AgentRequest;
import com.whis.app.agent.api.AgentResponse;
import com.whis.app.agent.api.GeminiAgentClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * AI Scam Story Generator — Generates real-world, engaging scam stories
 * (Bank Scam, KYC Scam, Money Scam, Digital Arrest, etc.) with practical solutions
 * (What To Do & What Not To Do).
 */
public class LearnStoryGenerator {

    public interface StoryCallback {
        void onStoryGenerated(LearnChapter newChapter);
        void onError(String error);
    }

    private static final String[] TOPICS = new String[]{
            "Bank Account Freeze & Urgent Fraud Department Call",
            "KYC Update Mandate & APK Download Link SMS",
            "UPI Money Refund & Fake Payment QR Code Trap",
            "Digital Arrest & Fake CBI Police Officer WhatsApp Video Call",
            "Instant Paperless Loan App & Contact List Blackmail",
            "Electricity Disconnection Power Cut Notice SMS",
            "Telegram WFH Prepaid Task & Crypto Investment Scam",
            "FedEx Courier Customs Clearance & Drug Package Extortion",
            "Credit Card Reward Points Expiry & Fake Banking Page"
    };

    public static void generateNewStory(Context context, StoryCallback callback) {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        int randomIndex = (int) (Math.random() * TOPICS.length);
        String selectedTopic = TOPICS[randomIndex];

        String prompt = "Generate a real-world, engaging and realistic scam story about: " + selectedTopic + ".\n"
                + "The story MUST include what happened (the realistic incident) AND how to solve that situation if it happens to someone.\n"
                + "Return ONLY a valid JSON object with no markdown formatting:\n"
                + "{\n"
                + "  \"id\": \"story_" + System.currentTimeMillis() + "\",\n"
                + "  \"title\": \"[Catchy Realistic Scam Story Title]\",\n"
                + "  \"category\": \"[Bank Scam / KYC Scam / Money Scam / Digital Arrest / Loan App Scam]\",\n"
                + "  \"story\": \"[3-paragraph realistic narrative explaining what happened during this scam incident]\",\n"
                + "  \"why_it_works\": \"[Explanation of the trick scammers used to create panic or greed]\",\n"
                + "  \"what_to_do\": [\"Step 1 to solve this problem\", \"Step 2\", \"Step 3\"],\n"
                + "  \"what_not_to_do\": [\"Mistake 1 to avoid\", \"Mistake 2\", \"Mistake 3\"]\n"
                + "}";

        GeminiAgentClient client = new GeminiAgentClient(context);
        client.sendMessage(new AgentRequest(prompt, new ArrayList<>(), prompt, null), new GeminiAgentClient.StreamCallback() {
            @Override
            public void onChunk(String textChunk) {}

            @Override
            public void onComplete(AgentResponse response) {
                try {
                    String raw = response.getMessage().replaceAll("```json", "").replaceAll("```", "").trim();
                    JSONObject json = new JSONObject(raw);

                    LearnChapter ch = parseChapterFromJson(json, selectedTopic);
                    mainHandler.post(() -> callback.onStoryGenerated(ch));
                } catch (Exception e) {
                    LearnChapter fallback = generateLocalFallbackStory(selectedTopic);
                    mainHandler.post(() -> callback.onStoryGenerated(fallback));
                }
            }

            @Override
            public void onError(Exception e) {
                LearnChapter fallback = generateLocalFallbackStory(selectedTopic);
                mainHandler.post(() -> callback.onStoryGenerated(fallback));
            }
        });
    }

    private static LearnChapter parseChapterFromJson(JSONObject json, String topic) {
        String id = json.optString("id", "story_" + System.currentTimeMillis());
        String title = json.optString("title", topic + " Case File");
        String category = json.optString("category", "SCAM STORY");
        String story = json.optString("story", "A realistic scam situation story.");
        String why = json.optString("why_it_works", "Scammers use panic, fear, or urgency to stop you from thinking clearly.");

        List<String> doList = new ArrayList<>();
        JSONArray doArr = json.optJSONArray("what_to_do");
        if (doArr != null) {
            for (int i = 0; i < doArr.length(); i++) doList.add(doArr.optString(i));
        }
        if (doList.isEmpty()) doList = Arrays.asList("Hang up immediately.", "Call official bank helpline directly.", "Report to 1930 Cybercrime Helpline.");

        List<String> notDoList = new ArrayList<>();
        JSONArray notDoArr = json.optJSONArray("what_not_to_do");
        if (notDoArr != null) {
            for (int i = 0; i < notDoArr.length(); i++) notDoList.add(notDoArr.optString(i));
        }
        if (notDoList.isEmpty()) notDoList = Arrays.asList("Never share OTP or PIN.", "Never download unknown APKs.", "Never transfer money to clear your name.");

        LearnChapter ch = new LearnChapter(
                id,
                title,
                category.toUpperCase(),
                "RATING_5_STAR",
                story,
                why,
                doList,
                "Whis screens suspicious calls & messages automatically to protect you."
        );
        ch.whatNotToDo = notDoList;
        return ch;
    }

    public static LearnChapter generateLocalFallbackStory(String topic) {
        String id = "story_" + System.currentTimeMillis();
        String title = topic + ": The Real Incident";
        String category = topic.contains("KYC") ? "KYC SCAM" : (topic.contains("Bank") ? "BANK SCAM" : "MONEY SCAM");
        
        String story = "Sharmaji received an urgent SMS claiming his bank account was blocked due to pending KYC verification. The message contained a link: 'Click here to update KYC within 2 hours or account will be permanently frozen.'\n\nWorried about his savings, Sharmaji clicked the link and downloaded a file called 'BankKYC.apk'. A caller claiming to be a bank official then guided him to enter his bank details and share the OTP received on his phone.\n\nWithin minutes, Rs 75,000 was debited. Fortunately, Sharmaji immediately called the National Cybercrime Helpline 1930 and contacted his bank to freeze the beneficiary account before the money was withdrawn.";

        List<String> doList = Arrays.asList(
                "Call 1930 National Cybercrime Helpline immediately within 2 hours of payment.",
                "Contact your bank's official customer care number to freeze your debit card & banking access.",
                "Always verify account status directly by visiting your bank branch or official banking app."
        );

        List<String> notDoList = Arrays.asList(
                "Never click links or download APK files sent via SMS or WhatsApp for KYC updates.",
                "Never share OTPs, PINs, or passwords with anyone claiming to be a bank official.",
                "Never panic when given short deadlines like 'account frozen in 2 hours'."
        );

        LearnChapter ch = new LearnChapter(
                id,
                title,
                category,
                "RATING_5_STAR",
                story,
                "Creation of artificial urgency (2-hour deadline) causes panic, preventing victims from verifying with official sources.",
                doList,
                "Whis automatically flags SMS messages containing suspicious APK download links."
        );
        ch.whatNotToDo = notDoList;
        return ch;
    }
}

