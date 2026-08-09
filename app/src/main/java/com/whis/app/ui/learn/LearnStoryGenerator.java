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
 * AI Scam Story Generator — Generates real-world, funny & mature Hinglish scam awareness stories
 * with clear outcomes (What To Do / What Not To Do) and interactive Q&A questions.
 */
public class LearnStoryGenerator {

    public interface StoryCallback {
        void onStoryGenerated(LearnChapter newChapter);
        void onError(String error);
    }

    private static final String[] TOPICS = new String[]{
            "Digital Arrest & Fake CBI Officer Video Call",
            "Free Birthday Cake & Delivery OTP Scam",
            "Sweety's AnyDesk Remote Desktop Love Fraud",
            "Customs Parcel & Illegal Drugs Threat",
            "Fake Electricity Bill Disconnection Link",
            "WFH Telegram Prepaid Task Scam"
    };

    public static void generateNewStory(Context context, StoryCallback callback) {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        int randomIndex = (int) (Math.random() * TOPICS.length);
        String selectedTopic = TOPICS[randomIndex];

        String prompt = "Generate a real-world, engaging, funny and mature Hinglish scam awareness story about: " + selectedTopic + ".\n"
                + "Return ONLY a valid JSON object with no markdown formatting:\n"
                + "{\n"
                + "  \"id\": \"story_" + System.currentTimeMillis() + "\",\n"
                + "  \"title\": \"[Funny Catchy Hinglish Title]\",\n"
                + "  \"story\": \"[3-paragraph funny & realistic story narrative in Hinglish about how someone almost got trapped and learned a lesson]\",\n"
                + "  \"what_to_do\": [\"Step 1\", \"Step 2\", \"Step 3\"],\n"
                + "  \"what_not_to_do\": [\"Mistake 1\", \"Mistake 2\", \"Mistake 3\"],\n"
                + "  \"qna\": [\n"
                + "    {\"q\": \"Question 1\", \"yes_correct\": false, \"explanation\": \"Explanation 1\"},\n"
                + "    {\"q\": \"Question 2\", \"yes_correct\": true, \"explanation\": \"Explanation 2\"}\n"
                + "  ]\n"
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
                    // Fallback to local rich story generator
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
        String title = json.optString("title", "The Unexpected Scam Lesson");
        String story = json.optString("story", "A funny real-life story about how Ramesh Uncle spotted a scam.");

        List<String> doList = new ArrayList<>();
        JSONArray doArr = json.optJSONArray("what_to_do");
        if (doArr != null) {
            for (int i = 0; i < doArr.length(); i++) doList.add(doArr.optString(i));
        }
        if (doList.isEmpty()) doList = Arrays.asList("Disconnect suspicious calls immediately.", "Verify official website directly.", "Call 1930 Helpline if money was sent.");

        List<String> notDoList = new ArrayList<>();
        JSONArray notDoArr = json.optJSONArray("what_not_to_do");
        if (notDoArr != null) {
            for (int i = 0; i < notDoArr.length(); i++) notDoList.add(notDoArr.optString(i));
        }
        if (notDoList.isEmpty()) notDoList = Arrays.asList("Never share OTP or UPI PIN.", "Never download AnyDesk/TeamViewer.", "Never pay money to avoid video call arrest.");

        LearnChapter ch = new LearnChapter(
                id,
                title,
                "AI STORY CASE",
                "RATING_5_STAR",
                story,
                "Fear of police or quick temptation disables logical thinking.",
                doList,
                "Whis screens calls in real time and alerts family instantly."
        );
        ch.whatNotToDo = notDoList;
        return ch;
    }

    public static LearnChapter generateLocalFallbackStory(String topic) {
        String id = "story_" + System.currentTimeMillis();
        String title = "Ramesh Uncle's $5,000 CBI Video Call Trap";
        String story = "Ramesh Uncle was enjoying his afternoon tea when a WhatsApp video call rang from a guy in a police uniform claiming to be a CBI Officer. The officer shouted: 'Rameshji! A parcel with illegal SIM cards was seized in Mumbai in your name! You are under Digital Arrest!'\n\nRamesh Uncle got nervous and was about to transfer Rs 50,000 to 'clear his name'. But his daughter walked in, saw the officer wearing a cheap uniform with a fake badge, and laughed: 'Papa, real police never conducts digital arrests over WhatsApp!'\n\nRamesh Uncle disconnected, blocked the number, and saved his hard-earned savings while laughing at the scammer's bad acting.";

        List<String> doList = Arrays.asList(
                "Disconnect WhatsApp video calls claiming to be police or CBI.",
                "Tell your family members immediately before making any transfer.",
                "Call National Cybercrime Helpline 1930 if money was debited."
        );

        List<String> notDoList = Arrays.asList(
                "Never pay money or 'security deposits' to clear your name.",
                "Never share your bank details, OTPs, or UPI PIN over video calls.",
                "Never stay isolated — scammers insist you don't tell anyone."
        );

        LearnChapter ch = new LearnChapter(
                id,
                title,
                "AI STORY CASE",
                "RATING_5_STAR",
                story,
                "Scammers use fear and urgency to bypass your normal logic.",
                doList,
                "Whis flags fake police numbers automatically."
        );
        ch.whatNotToDo = notDoList;
        return ch;
    }
}
