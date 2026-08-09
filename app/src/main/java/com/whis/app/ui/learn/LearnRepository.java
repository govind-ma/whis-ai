package com.whis.app.ui.learn;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Data repository for Whis Knowledge Book (LEARN_PLAN.md).
 * Loads chapters from assets/learn_chapters.json with fallbacks and manages completion status in SharedPreferences.
 */
public class LearnRepository {

    private static final String TAG = "LearnRepository";
    private static final String PREF_NAME = "whis_learn_prefs";
    private static final String KEY_COMPLETED_PREFIX = "completed_";

    private static LearnRepository instance;

    private final Context appContext;
    private final List<LearnChapter> chapters = new ArrayList<>();
    private String bookTitle = "Whis Knowledge Book";
    private String subtitle = "Every scam has a pattern. Once you see the pattern, it stops working on you.";
    private String helpline = "1930";
    private String portal = "cybercrime.gov.in";
    private String reportingNote = "Call 1930 as fast as possible if money has already moved — banks have the best chance of freezing the receiving account within the first hours.";

    private LearnRepository(Context context) {
        this.appContext = context.getApplicationContext();
        loadChapters();
    }

    public static synchronized LearnRepository getInstance(Context context) {
        if (instance == null) {
            instance = new LearnRepository(context);
        }
        return instance;
    }

    private void loadChapters() {
        chapters.clear();
        try {
            InputStream is = appContext.getAssets().open("learn_chapters.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            String jsonStr = new String(buffer, StandardCharsets.UTF_8);
            JSONObject root = new JSONObject(jsonStr);

            if (root.has("bookTitle")) bookTitle = root.getString("bookTitle");
            if (root.has("subtitle")) subtitle = root.getString("subtitle");

            if (root.has("universalReporting")) {
                JSONObject rep = root.getJSONObject("universalReporting");
                if (rep.has("helpline")) helpline = rep.getString("helpline");
                if (rep.has("portal")) portal = rep.getString("portal");
                if (rep.has("note")) reportingNote = rep.getString("note");
            }

            JSONArray jsonChapters = root.getJSONArray("chapters");
            for (int i = 0; i < jsonChapters.length(); i++) {
                JSONObject obj = jsonChapters.getJSONObject(i);

                String id = obj.getString("chapterId");
                String title = obj.getString("title");
                String shortName = obj.optString("shortName", title);

                List<String> triggers = parseJsonArray(obj.optJSONArray("searchTriggers"));
                String whatHappens = obj.optString("whatHappens", "");
                String whyItWorks = obj.optString("whyItWorks", "");
                List<String> doRightNow = parseJsonArray(obj.optJSONArray("doRightNow"));
                String howWhisHelps = obj.optString("howWhisHelps", "");
                List<String> crossRef = parseJsonArray(obj.optJSONArray("crossReference"));
                String sourceConfidence = obj.optString("sourceConfidence", "");

                chapters.add(new LearnChapter(
                        id, title, shortName, triggers, whatHappens, whyItWorks,
                        doRightNow, howWhisHelps, crossRef, sourceConfidence
                ));
            }

            // Load dynamically generated AI story chapters from SharedPreferences
            loadDynamicChapters();
            Log.d(TAG, "Successfully loaded " + chapters.size() + " total chapters.");
        } catch (Exception e) {
            Log.e(TAG, "Failed to load chapters from assets, using fallback content", e);
            loadFallbackChapters();
        }
    }

    public void addDynamicChapter(Context context, LearnChapter chapter) {
        if (chapter == null) return;
        chapters.add(chapter);

        // Save to SharedPreferences
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String existing = prefs.getString("dynamic_stories_json", "[]");
            JSONArray arr = new JSONArray(existing);

            JSONObject obj = new JSONObject();
            obj.put("id", chapter.chapterId);
            obj.put("title", chapter.title);
            obj.put("story", chapter.whatHappens);
            obj.put("whyItWorks", chapter.whyItWorks);
            obj.put("howWhisHelps", chapter.howWhisHelps);
            obj.put("doRightNow", new JSONArray(chapter.doRightNow));
            obj.put("whatNotToDo", new JSONArray(chapter.whatNotToDo));

            arr.put(obj);
            prefs.edit().putString("dynamic_stories_json", arr.toString()).apply();
        } catch (Exception e) {
            Log.e(TAG, "Failed to save dynamic story chapter", e);
        }
    }

    private void loadDynamicChapters() {
        try {
            SharedPreferences prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String jsonStr = prefs.getString("dynamic_stories_json", "[]");
            JSONArray arr = new JSONArray(jsonStr);

            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String id = obj.optString("id", "story_" + i);
                String title = obj.optString("title", "AI Scam Story");
                String story = obj.optString("story", "");
                String why = obj.optString("whyItWorks", "");
                String whis = obj.optString("howWhisHelps", "");

                List<String> doList = parseJsonArray(obj.optJSONArray("doRightNow"));
                List<String> notDoList = parseJsonArray(obj.optJSONArray("whatNotToDo"));

                LearnChapter ch = new LearnChapter(id, title, "AI STORY", "RATING_5_STAR", story, why, doList, whis);
                ch.whatNotToDo = notDoList;
                chapters.add(ch);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse stored dynamic story chapters", e);
        }
    }

    private List<String> parseJsonArray(JSONArray array) {
        List<String> list = new ArrayList<>();
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                list.add(array.optString(i));
            }
        }
        return list;
    }

    private void loadFallbackChapters() {
        // Fallback hardcoded in case assets fails
        List<String> triggers1 = new ArrayList<>();
        triggers1.add("police video call"); triggers1.add("digital arrest"); triggers1.add("CBI called me");
        List<String> steps1 = new ArrayList<>();
        steps1.add("Hang up. There is no such thing as a 'digital arrest' under Indian law.");
        steps1.add("Do not share your OTP, bank details, or make any payment.");
        steps1.add("If you already made a payment, call 1930 immediately.");

        chapters.add(new LearnChapter(
                "digital_arrest", "The Fake Police Video Call", "Digital Arrest",
                triggers1,
                "You get a call or video call from someone claiming to be police, CBI, ED, or a telecom official demanding money to avoid 'digital arrest'.",
                "Works through extreme psychological pressure, fear of arrest, and forcing isolation.",
                steps1,
                "Whis flags calls from reported numbers and shows plain alerts.",
                new ArrayList<>(), "high"
        ));
    }

    public List<LearnChapter> getAllChapters() {
        return new ArrayList<>(chapters);
    }

    public LearnChapter getChapterById(String id) {
        for (LearnChapter ch : chapters) {
            if (ch.chapterId.equals(id)) {
                return ch;
            }
        }
        return chapters.isEmpty() ? null : chapters.get(0);
    }

    public List<LearnChapter> searchChapters(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllChapters();
        }
        List<LearnChapter> result = new ArrayList<>();
        for (LearnChapter ch : chapters) {
            if (ch.matchesSearch(query)) {
                result.add(ch);
            }
        }
        return result;
    }

    public boolean isChapterCompleted(String chapterId) {
        SharedPreferences prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_COMPLETED_PREFIX + chapterId, false);
    }

    public void setChapterCompleted(String chapterId, boolean completed) {
        SharedPreferences prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_COMPLETED_PREFIX + chapterId, completed).apply();
    }

    public int getCompletedCount() {
        int count = 0;
        for (LearnChapter ch : chapters) {
            if (isChapterCompleted(ch.chapterId)) {
                count++;
            }
        }
        return count;
    }

    public int getTotalCount() {
        return chapters.size();
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getHelpline() {
        return helpline;
    }

    public String getPortal() {
        return portal;
    }

    public String getReportingNote() {
        return reportingNote;
    }
}
