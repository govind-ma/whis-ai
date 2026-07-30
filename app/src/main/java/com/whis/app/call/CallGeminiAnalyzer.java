package com.whis.app.call;

import android.content.Context;
import android.util.Log;

import com.whis.app.BuildConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Synchronous Gemini REST API caller for incoming call classification.
 * <p>
 * MUST be invoked from a background thread (called by {@link CallFilterService}
 * which runs inside a {@link androidx.core.app.JobIntentService}).
 * <p>
 * Timeout: 3000 ms connect + read (as required by call real-time UX).
 * On any failure, returns a safe {@link CallGeminiResult} — never throws.
 */
public class CallGeminiAnalyzer {

    private static final String TAG = "CallGeminiAnalyzer";

    private static final String ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";
    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");

    /** Shared client with tight 3-second timeouts for call-time responsiveness. */
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
            .writeTimeout(3, TimeUnit.SECONDS)
            .build();

    // ── Result container ──────────────────────────────────────────────────────

    public static class CallGeminiResult {
        /** Whether the API actually responded (false = fallback was used). */
        public final boolean ran;
        /** "SAFE" | "SUSPICIOUS" | "SCAM" */
        public final String riskLevel;
        /** "TRUSTED" | "LIKELY_SAFE" | "SUSPICIOUS" | "UNKNOWN" | "DIGITAL_ARREST" | "IMPERSONATION" */
        public final String category;
        /** One-line Hinglish explanation. */
        public final String reason;

        public CallGeminiResult(boolean ran, String riskLevel, String category, String reason) {
            this.ran = ran;
            this.riskLevel = riskLevel != null ? riskLevel : "SUSPICIOUS";
            this.category  = category  != null ? category  : "UNKNOWN";
            this.reason    = reason    != null ? reason    : "";
        }
    }

    private CallGeminiAnalyzer() {}

    // ── Public entry point ────────────────────────────────────────────────────

    /**
     * Classify an incoming call number via Gemini API.
     * Runs synchronously — MUST be called from a background thread.
     *
     * @param phoneNumber raw incoming number (e.g. "+919876543210")
     * @param contactName display name from contacts, or "Unknown"
     * @return {@link CallGeminiResult} — never null
     */
    public static CallGeminiResult analyze(Context context, String phoneNumber, String contactName) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return fallback("Empty phone number");
        }

        String apiKey = BuildConfig.GEMINI_API_KEY != null ? BuildConfig.GEMINI_API_KEY.trim() : "";

        // ── GROQ PATH (gsk_...) ──────────────────────────────────────────────
        if (apiKey.startsWith("gsk_")) {
            return callGroqApi(apiKey, phoneNumber, contactName);
        }

        // ── GOOGLE GEMINI PATH ──────────────────────────────────────────────
        String requestJson = buildRequestJson(phoneNumber, contactName);
        if (requestJson == null) {
            return fallback("Failed to build request JSON");
        }

        String url = ENDPOINT + apiKey;
        RequestBody body = RequestBody.create(requestJson, JSON_TYPE);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", "WhisApp/1.0")
                .build();

        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errBody = response.body() != null ? response.body().string() : "empty";
                Log.e(TAG, "Gemini API error " + response.code() + ": " + errBody);
                return fallback("API error " + response.code());
            }

            String responseStr = response.body() != null ? response.body().string() : "";
            Log.d(TAG, "Call Gemini raw response: " + responseStr);
            return parseResponse(responseStr);

        } catch (IOException e) {
            Log.e(TAG, "Call Gemini network failure", e);
            return fallback("Network timeout or error");
        }
    }

    private static CallGeminiResult callGroqApi(String apiKey, String phoneNumber, String contactName) {
        try {
            JSONObject root = new JSONObject();
            root.put("model", "llama-3.3-70b-versatile");

            JSONArray messages = new JSONArray();

            JSONObject sysMsg = new JSONObject();
            sysMsg.put("role", "system");
            sysMsg.put("content", "You are an AI call classifier. Assess incoming call: return strictly JSON with risk_level (SAFE, SUSPICIOUS, SCAM), category (TRUSTED, DIGITAL_ARREST, IMPERSONATION, UNKNOWN), and reason.");
            messages.put(sysMsg);

            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", "Incoming Call Number: " + phoneNumber + "\nContact Name: " + contactName);
            messages.put(userMsg);

            root.put("messages", messages);

            RequestBody body = RequestBody.create(root.toString(), JSON_TYPE);
            Request request = new Request.Builder()
                    .url("https://api.groq.com/openai/v1/chat/completions")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("User-Agent", "WhisApp/1.0")
                    .build();

            try (Response response = HTTP_CLIENT.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errBody = response.body() != null ? response.body().string() : "empty";
                    Log.e(TAG, "Groq API error " + response.code() + ": " + errBody);
                    return fallback("Groq error " + response.code());
                }

                String responseStr = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "Call Groq raw response: " + responseStr);

                JSONObject resObj = new JSONObject(responseStr);
                JSONArray choices = resObj.getJSONArray("choices");
                String rawText = choices.getJSONObject(0).getJSONObject("message").getString("content").trim();

                if (rawText.startsWith("```json")) rawText = rawText.substring(7);
                else if (rawText.startsWith("```")) rawText = rawText.substring(3);
                if (rawText.endsWith("```")) rawText = rawText.substring(0, rawText.length() - 3);

                JSONObject inner = new JSONObject(rawText.trim());
                String riskLevel = inner.optString("risk_level", "SUSPICIOUS").toUpperCase();
                String category  = inner.optString("category", "UNKNOWN");
                String reason    = inner.optString("reason", "AI analysis complete");

                if (!riskLevel.equals("SAFE") && !riskLevel.equals("SCAM")) {
                    riskLevel = "SUSPICIOUS";
                }

                return new CallGeminiResult(true, riskLevel, category, reason);
            }

        } catch (Exception e) {
            Log.e(TAG, "Groq call failed", e);
            return fallback("Groq analysis error");
        }
    }

    // ── Prompt & request builder ──────────────────────────────────────────────

    private static String buildRequestJson(String phoneNumber, String contactName) {
        try {
            String prompt = "You are Whis, an Indian cyber fraud detection AI.\n"
                    + "Analyze this incoming call and classify it.\n\n"
                    + "Phone Number: " + phoneNumber + "\n"
                    + "Saved As: " + (contactName != null ? contactName : "Unknown") + "\n\n"
                    + "Consider these Indian scam patterns:\n"
                    + "- Numbers starting with +92 (Pakistan) claiming to be Indian officials\n"
                    + "- Numbers claiming to be CBI, ED, Police, Customs\n"
                    + "- International numbers claiming to be TRAI\n"
                    + "- Numbers reported for digital arrest scams\n"
                    + "- Courier scam numbers\n\n"
                    + "Respond ONLY in this exact JSON format:\n"
                    + "{\n"
                    + "  \"risk_level\": \"SAFE\" or \"SUSPICIOUS\" or \"SCAM\",\n"
                    + "  \"category\": \"TRUSTED\" or \"LIKELY_SAFE\" or \"SUSPICIOUS\" or \"UNKNOWN\" or \"DIGITAL_ARREST\" or \"IMPERSONATION\",\n"
                    + "  \"reason\": \"one line explanation in Hinglish\"\n"
                    + "}\n\n"
                    + "Do not add any text outside the JSON.";

            JSONObject part = new JSONObject();
            part.put("text", prompt);

            JSONArray partsArr = new JSONArray();
            partsArr.put(part);

            JSONObject content = new JSONObject();
            content.put("role", "user");
            content.put("parts", partsArr);

            JSONArray contentsArr = new JSONArray();
            contentsArr.put(content);

            JSONObject root = new JSONObject();
            root.put("contents", contentsArr);

            return root.toString();
        } catch (Exception e) {
            Log.e(TAG, "buildRequestJson failed", e);
            return null;
        }
    }

    // ── Response parser ───────────────────────────────────────────────────────

    private static CallGeminiResult parseResponse(String rawResponse) {
        try {
            // Unwrap Gemini envelope: candidates[0].content.parts[0].text
            JSONObject root       = new JSONObject(rawResponse);
            JSONArray  candidates = root.getJSONArray("candidates");
            JSONObject contentObj = candidates.getJSONObject(0).getJSONObject("content");
            JSONArray  parts      = contentObj.getJSONArray("parts");
            String text = parts.getJSONObject(0).getString("text").trim();

            // Strip optional markdown fences
            if (text.startsWith("```json")) text = text.substring(7);
            else if (text.startsWith("```")) text = text.substring(3);
            if (text.endsWith("```")) text = text.substring(0, text.length() - 3);

            JSONObject inner = new JSONObject(text.trim());

            String riskLevel = inner.optString("risk_level", "SUSPICIOUS").toUpperCase();
            String category  = inner.optString("category", "UNKNOWN");
            String reason    = inner.optString("reason", "AI analysis complete");

            // Normalise risk_level
            if (!riskLevel.equals("SAFE") && !riskLevel.equals("SCAM")) {
                riskLevel = "SUSPICIOUS";
            }

            Log.d(TAG, "Call parsed: risk=" + riskLevel + " cat=" + category + " reason=" + reason);
            return new CallGeminiResult(true, riskLevel, category, reason);

        } catch (Exception e) {
            Log.e(TAG, "Failed to parse Gemini call response", e);
            return fallback("AI response parse error");
        }
    }

    private static CallGeminiResult fallback(String reason) {
        return new CallGeminiResult(false, "SUSPICIOUS", "UNKNOWN", reason);
    }
}
