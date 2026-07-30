package com.whis.app.agent.api;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.whis.app.BuildConfig;
import com.whis.app.agent.model.ChatMessage;
import com.whis.app.agent.model.RiskLevel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Gemini REST API client using OkHttp (AI_AGENT_PLAN.md Section 4.4).
 * <p>
 * Calls: POST https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent
 * API key sourced from BuildConfig.GEMINI_API_KEY (defined in app/build.gradle).
 */
public class GeminiAgentClient {

    private static final String TAG = "GeminiAgentClient";
    private static final String ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";
    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");

    public interface StreamCallback {
        void onChunk(String textChunk);
        void onComplete(AgentResponse response);
        void onError(Exception e);
    }

    private final Context context;
    private final OkHttpClient httpClient;
    private final Handler mainHandler;

    public GeminiAgentClient(Context context) {
        this.context = context.getApplicationContext();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Send message to Gemini AI via REST API.
     * Network call is made asynchronously via OkHttp's enqueue().
     * Callbacks are delivered on the main thread.
     */
    public void sendMessage(AgentRequest request, StreamCallback callback) {
        if (request == null || callback == null) return;

        String apiKey = BuildConfig.GEMINI_API_KEY != null ? BuildConfig.GEMINI_API_KEY.trim() : "";

        // ── GROQ API PATH (gsk_...) ──────────────────────────────────────────
        if (apiKey.startsWith("gsk_")) {
            sendGroqMessage(apiKey, request, callback);
            return;
        }

        // ── GOOGLE GEMINI PATH ──────────────────────────────────────────────
        String requestBodyJson = buildRequestBody(request);
        if (requestBodyJson == null) {
            mainHandler.post(() -> callback.onError(new IllegalStateException("Failed to build request body")));
            return;
        }

        String url = ENDPOINT + apiKey;

        RequestBody body = RequestBody.create(requestBodyJson, JSON_TYPE);
        Request httpRequest = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", "WhisApp/1.0")
                .build();

        httpClient.newCall(httpRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Network request failed", e);
                mainHandler.post(() -> callback.onError(e));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "empty";
                    Log.e(TAG, "API error " + response.code() + ": " + errorBody);
                    
                    // If 503 Service Unavailable or 429 Too Many Requests, auto-retry with gemini-1.5-flash
                    if ((response.code() == 503 || response.code() == 429) && url.contains("gemini-2.5-flash")) {
                        Log.w(TAG, "gemini-2.5-flash high demand (503/429). Retrying with gemini-1.5-flash...");
                        String fallbackUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + BuildConfig.GEMINI_API_KEY;
                        Request fallbackReq = httpRequest.newBuilder().url(fallbackUrl).build();
                        httpClient.newCall(fallbackReq).enqueue(this);
                        return;
                    }

                    mainHandler.post(() -> callback.onError(
                            new IOException("Gemini API error " + response.code() + ": " + errorBody)));
                    return;
                }

                String responseBodyStr = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "Raw Gemini response: " + responseBodyStr);

                try {
                    JSONObject root = new JSONObject(responseBodyStr);
                    JSONArray candidates = root.getJSONArray("candidates");
                    JSONObject content = candidates.getJSONObject(0).getJSONObject("content");
                    JSONArray parts = content.getJSONArray("parts");
                    String rawText = parts.getJSONObject(0).getString("text");

                    mainHandler.post(() -> callback.onChunk(rawText));

                    AgentResponse parsed = parseResponse(rawText);
                    mainHandler.post(() -> callback.onComplete(parsed));

                } catch (Exception e) {
                    Log.e(TAG, "Failed to parse Gemini response", e);
                    mainHandler.post(() -> callback.onError(e));
                }
            }
        });
    }

    private void sendGroqMessage(String apiKey, AgentRequest request, StreamCallback callback) {
        try {
            JSONObject root = new JSONObject();
            root.put("model", "llama-3.3-70b-versatile");

            JSONArray messages = new JSONArray();

            if (request.systemPrompt != null && !request.systemPrompt.isEmpty()) {
                JSONObject sys = new JSONObject();
                sys.put("role", "system");
                sys.put("content", request.systemPrompt);
                messages.put(sys);
            }

            if (request.history != null) {
                for (ChatMessage msg : request.history) {
                    JSONObject turn = new JSONObject();
                    turn.put("role", ChatMessage.ROLE_USER.equals(msg.role) ? "user" : "assistant");
                    turn.put("content", msg.content);
                    messages.put(turn);
                }
            }

            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", request.userMessage);
            messages.put(userMsg);

            root.put("messages", messages);

            RequestBody body = RequestBody.create(root.toString(), JSON_TYPE);
            Request httpRequest = new Request.Builder()
                    .url("https://api.groq.com/openai/v1/chat/completions")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("User-Agent", "WhisApp/1.0")
                    .build();

            httpClient.newCall(httpRequest).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Groq network request failed", e);
                    mainHandler.post(() -> callback.onError(e));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "empty";
                        Log.e(TAG, "Groq API error " + response.code() + ": " + errorBody);

                        // If 429 Rate Limit, auto-fallback from 70B to high-throughput 8B instant model (30,000 TPM)
                        if (response.code() == 429 && root.optString("model").equals("llama-3.3-70b-versatile")) {
                            Log.w(TAG, "Groq 70B rate limit hit (429). Auto-switching to high-throughput llama-3.1-8b-instant...");
                            try {
                                root.put("model", "llama-3.1-8b-instant");
                                Request bodyReq = httpRequest.newBuilder()
                                        .post(RequestBody.create(root.toString(), JSON_TYPE))
                                        .build();
                                httpClient.newCall(bodyReq).enqueue(this);
                                return;
                            } catch (Exception e) {
                                Log.e(TAG, "Failed to switch Groq fallback model", e);
                            }
                        }

                        mainHandler.post(() -> callback.onError(
                                new IOException("Groq API error " + response.code() + ": " + errorBody)));
                        return;
                    }

                    String resStr = response.body() != null ? response.body().string() : "";
                    Log.d(TAG, "Raw Groq response: " + resStr);

                    try {
                        JSONObject resObj = new JSONObject(resStr);
                        JSONArray choices = resObj.getJSONArray("choices");
                        JSONObject firstChoice = choices.getJSONObject(0);
                        JSONObject msgObj = firstChoice.getJSONObject("message");
                        String rawText = msgObj.getString("content");

                        mainHandler.post(() -> callback.onChunk(rawText));

                        AgentResponse parsed = parseResponse(rawText);
                        mainHandler.post(() -> callback.onComplete(parsed));

                    } catch (Exception e) {
                        Log.e(TAG, "Failed to parse Groq response", e);
                        mainHandler.post(() -> callback.onError(e));
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Failed to build Groq request", e);
            mainHandler.post(() -> callback.onError(e));
        }
    }

    /**
     * Build the JSON request body for the Gemini generateContent API.
     *
     * <pre>
     * {
     *   "contents": [
     *     { "role": "user", "parts": [{"text": "HISTORY + USER_MESSAGE"}] }
     *   ],
     *   "systemInstruction": {
     *     "parts": [{"text": "SYSTEM_PROMPT"}]
     *   }
     * }
     * </pre>
     */
    private String buildRequestBody(AgentRequest request) {
        try {
            JSONObject root = new JSONObject();

            // Build contents array — flatten history + new user message into a single user turn
            // so we keep things simple and within the single-turn format required.
            StringBuilder conversationText = new StringBuilder();
            if (request.history != null) {
                for (ChatMessage msg : request.history) {
                    String roleLabel = ChatMessage.ROLE_AGENT.equals(msg.role) ? "Assistant" : "User";
                    conversationText.append(roleLabel).append(": ").append(msg.content).append("\n\n");
                }
            }
            conversationText.append("User: ").append(request.userMessage != null ? request.userMessage : "");

            JSONObject part = new JSONObject();
            part.put("text", conversationText.toString());

            JSONArray partsArray = new JSONArray();
            partsArray.put(part);

            JSONObject userContent = new JSONObject();
            userContent.put("role", "user");
            userContent.put("parts", partsArray);

            JSONArray contentsArray = new JSONArray();
            contentsArray.put(userContent);

            root.put("contents", contentsArray);

            // System instruction
            if (request.systemPrompt != null && !request.systemPrompt.isEmpty()) {
                JSONObject sysPart = new JSONObject();
                sysPart.put("text", request.systemPrompt);

                JSONArray sysPartsArray = new JSONArray();
                sysPartsArray.put(sysPart);

                JSONObject systemInstruction = new JSONObject();
                systemInstruction.put("parts", sysPartsArray);

                root.put("systemInstruction", systemInstruction);
            }

            return root.toString();
        } catch (Exception e) {
            Log.e(TAG, "Failed to build request body", e);
            return null;
        }
    }

    /**
     * Parse the raw text response from Gemini.
     * Expects a JSON block with message, risk_level, option_buttons, action_flags.
     * Falls back gracefully if the model returns plain text instead of JSON.
     */
    public static AgentResponse parseResponse(String rawText) {
        AgentResponse response = new AgentResponse();
        if (rawText == null || rawText.trim().isEmpty()) {
            response.setMessage("Kripya punah prayas karein.");
            return response;
        }

        try {
            // Strip optional markdown code fences
            String clean = rawText.trim();
            if (clean.startsWith("```json")) {
                clean = clean.substring(7);
            } else if (clean.startsWith("```")) {
                clean = clean.substring(3);
            }
            if (clean.endsWith("```")) {
                clean = clean.substring(0, clean.length() - 3);
            }

            JSONObject json = new JSONObject(clean.trim());
            String msgText = json.optString("message", "");
            if (msgText.trim().isEmpty()) {
                msgText = clean.trim();
            }
            response.setMessage(msgText);

            String riskStr = json.optString("risk_level", "LOW");
            try {
                response.setRiskLevel(RiskLevel.valueOf(riskStr.toUpperCase()));
            } catch (Exception e) {
                response.setRiskLevel(RiskLevel.LOW);
            }

            JSONArray btnArr = json.optJSONArray("option_buttons");
            if (btnArr != null) {
                for (int i = 0; i < btnArr.length(); i++) {
                    response.getOptionButtons().add(btnArr.getString(i));
                }
            }

            JSONArray flagArr = json.optJSONArray("action_flags");
            if (flagArr != null) {
                for (int i = 0; i < flagArr.length(); i++) {
                    response.getActionFlags().add(flagArr.getString(i));
                }
            }

        } catch (Exception e) {
            // Model returned plain text — use it directly as the message
            response.setMessage(rawText.trim());
            response.setRiskLevel(RiskLevel.MEDIUM);
        }

        return response;
    }
}
