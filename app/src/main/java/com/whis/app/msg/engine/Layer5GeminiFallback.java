package com.whis.app.msg.engine;

import android.content.Context;
import android.util.Log;

import com.whis.app.BuildConfig;
import com.whis.app.msg.util.GeminiRateLimiter;

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
 * Layer 5 — Gemini REST API Fallback (MSG_PLAN.md Section 4.2 &amp; 4.3).
 * <p>
 * Calls Gemini 2.5 Flash via synchronous OkHttp {@code execute()} — safe because
 * this method is always invoked from a background {@link androidx.core.app.JobIntentService}
 * worker thread, never the main thread.
 * <p>
 * Rate-gated by {@link GeminiRateLimiter}. On any network error or parse failure
 * the method returns a fallback {@link GeminiResult} so the pipeline never crashes.
 */
public class Layer5GeminiFallback {

    private static final String TAG = "Layer5GeminiFallback";

    private static final String ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";
    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");

    /** Shared client — configured with 3-second timeouts per audit requirement. */
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
            .writeTimeout(3, TimeUnit.SECONDS)
            .build();

    // ── Result container ──────────────────────────────────────────────────────

    public static class GeminiResult {
        public final boolean ran;
        public final String verdict;    // "SAFE" | "SUSPICIOUS" | "SCAM"
        public final float confidence;
        public final String reason;
        public final String category;   // "DLT_VERIFIED" | "SCAM_LINK" | "OTP_FRAUD" | "KYC_FRAUD" | "COMMUNITY_FLAG" | "UNKNOWN"

        public GeminiResult(boolean ran, String verdict, float confidence,
                            String reason, String category) {
            this.ran = ran;
            this.verdict = verdict;
            this.confidence = confidence;
            this.reason = reason;
            this.category = category != null ? category : "UNKNOWN";
        }

        /** Backward-compat constructor for callers that don't need category. */
        public GeminiResult(boolean ran, String verdict, float confidence, String reason) {
            this(ran, verdict, confidence, reason, "UNKNOWN");
        }
    }

    private Layer5GeminiFallback() {
        // Utility class
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Request Gemini API fallback verdict if rate limiter permits.
     * Runs synchronously — MUST be called from a background thread.
     *
     * @param context android context
     * @param sender  SMS sender header or phone number
     * @param body    SMS body text
     * @return {@link GeminiResult} — never null
     */
    public static GeminiResult evaluate(Context context, String sender, String body) {
        if (context == null || body == null) {
            return fallback("Rate limit / invalid input");
        }

        if (!GeminiRateLimiter.tryAcquire(context)) {
            return fallback("Gemini API rate limit exceeded — falling back to suspicious");
        }

        return callGeminiApi(sender, body);
    }

    // ── Gemini REST call ──────────────────────────────────────────────────────

    private static GeminiResult callGeminiApi(String sender, String body) {
        String apiKey = BuildConfig.GEMINI_API_KEY != null ? BuildConfig.GEMINI_API_KEY.trim() : "";

        // ── GROQ PATH (gsk_...) ──────────────────────────────────────────────
        if (apiKey.startsWith("gsk_")) {
            return callGroqApi(apiKey, sender, body);
        }

        // ── GOOGLE GEMINI PATH ──────────────────────────────────────────────
        String prompt = buildPrompt(sender, body);
        String requestJson = buildRequestJson(prompt);
        if (requestJson == null) {
            return fallback("Failed to build request JSON");
        }

        String url = ENDPOINT + apiKey;
        RequestBody requestBody = RequestBody.create(requestJson, JSON_TYPE);
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", "WhisApp/1.0")
                .build();

        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errBody = response.body() != null ? response.body().string() : "empty";
                Log.e(TAG, "Gemini API error " + response.code() + ": " + errBody);
                return fallback("Gemini API error " + response.code());
            }

            String responseStr = response.body() != null ? response.body().string() : "";
            Log.d(TAG, "Layer5 Gemini raw response: " + responseStr);
            return parseGeminiResponse(responseStr);

        } catch (IOException e) {
            Log.e(TAG, "Layer5 network failure", e);
            return fallback("Network error: " + e.getMessage());
        }
    }

    private static GeminiResult callGroqApi(String apiKey, String sender, String body) {
        try {
            String prompt = buildPrompt(sender, body);

            JSONObject root = new JSONObject();
            root.put("model", "llama-3.3-70b-versatile");

            JSONArray messages = new JSONArray();
            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", prompt);
            messages.put(userMsg);

            root.put("messages", messages);

            RequestBody requestBody = RequestBody.create(root.toString(), JSON_TYPE);
            Request request = new Request.Builder()
                    .url("https://api.groq.com/openai/v1/chat/completions")
                    .post(requestBody)
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
                Log.d(TAG, "Layer5 Groq raw response: " + responseStr);

                JSONObject resObj = new JSONObject(responseStr);
                JSONArray choices = resObj.getJSONArray("choices");
                String rawText = choices.getJSONObject(0).getJSONObject("message").getString("content").trim();

                return parseGeminiResponse(rawText);
            }

        } catch (Exception e) {
            Log.e(TAG, "Layer5 Groq network failure", e);
            return fallback("Groq error: " + e.getMessage());
        }
    }

    // ── Prompt builder ────────────────────────────────────────────────────────

    /**
     * Builds the classification prompt sent to Gemini.
     * Instructs model to return ONLY a JSON object — no surrounding text.
     */
    private static String buildPrompt(String sender, String body) {
        return "You are Whis, an Indian cyber fraud detection AI.\n"
                + "Analyze this SMS and classify it.\n\n"
                + "Sender: " + (sender != null ? sender : "unknown") + "\n"
                + "Message: " + body + "\n\n"
                + "Respond ONLY in this exact JSON format:\n"
                + "{\n"
                + "  \"risk_level\": \"SAFE\" or \"SUSPICIOUS\" or \"SCAM\",\n"
                + "  \"category\": \"DLT_VERIFIED\" or \"SCAM_LINK\" or \"OTP_FRAUD\" or \"KYC_FRAUD\" or \"COMMUNITY_FLAG\" or \"UNKNOWN\",\n"
                + "  \"reason\": \"one line explanation in Hinglish\"\n"
                + "}\n\n"
                + "Do not add any text outside the JSON.";
    }

    /** Wraps prompt into the Gemini generateContent REST request body. */
    private static String buildRequestJson(String prompt) {
        try {
            JSONObject part = new JSONObject();
            part.put("text", prompt);

            org.json.JSONArray partsArr = new org.json.JSONArray();
            partsArr.put(part);

            JSONObject content = new JSONObject();
            content.put("role", "user");
            content.put("parts", partsArr);

            org.json.JSONArray contentsArr = new org.json.JSONArray();
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

    /**
     * Extracts the text from {@code candidates[0].content.parts[0].text}
     * then parses the inner JSON for risk_level, category, reason.
     */
    private static GeminiResult parseGeminiResponse(String rawResponse) {
        try {
            // Unwrap Gemini envelope
            JSONObject root = new JSONObject(rawResponse);
            org.json.JSONArray candidates = root.getJSONArray("candidates");
            JSONObject contentObj = candidates.getJSONObject(0).getJSONObject("content");
            org.json.JSONArray parts = contentObj.getJSONArray("parts");
            String text = parts.getJSONObject(0).getString("text").trim();

            // Strip markdown fences if model added them
            if (text.startsWith("```json")) text = text.substring(7);
            else if (text.startsWith("```")) text = text.substring(3);
            if (text.endsWith("```")) text = text.substring(0, text.length() - 3);

            JSONObject inner = new JSONObject(text.trim());

            String riskLevel = inner.optString("risk_level", "SUSPICIOUS").toUpperCase();
            String category  = inner.optString("category", "UNKNOWN");
            String reason    = inner.optString("reason", "AI analysis complete");

            // Normalise risk_level to the three allowed values
            if (!riskLevel.equals("SAFE") && !riskLevel.equals("SCAM")) {
                riskLevel = "SUSPICIOUS";
            }

            // Map risk → confidence score
            float confidence = switch (riskLevel) {
                case "SCAM"       -> 0.9f;
                case "SUSPICIOUS" -> 0.6f;
                default           -> 0.2f;  // SAFE
            };

            Log.d(TAG, "Layer5 parsed: risk=" + riskLevel + " cat=" + category + " reason=" + reason);
            return new GeminiResult(true, riskLevel, confidence, reason, category);

        } catch (Exception e) {
            Log.e(TAG, "Failed to parse Gemini response", e);
            return fallback("AI response parse error");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static GeminiResult fallback(String reason) {
        return new GeminiResult(false, "SUSPICIOUS", 0.5f, reason, "UNKNOWN");
    }
}
