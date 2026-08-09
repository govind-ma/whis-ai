package com.whis.app.agent;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.whis.app.agent.api.AgentRequest;
import com.whis.app.agent.api.AgentResponse;
import com.whis.app.agent.api.GeminiAgentClient;
import com.whis.app.agent.context.ModuleContextInjector;
import com.whis.app.agent.context.SessionContext;
import com.whis.app.agent.context.UserProfileContext;
import com.whis.app.agent.emergency.RedAlertManager;
import com.whis.app.agent.model.ChatMessage;
import com.whis.app.agent.model.MediaAttachment;
import com.whis.app.agent.model.RiskLevel;
import com.whis.app.agent.model.UserProfile;
import com.whis.app.agent.offline.ScenarioMatcher;
import com.whis.app.agent.prompt.SystemPromptBuilder;
import com.whis.app.core.WhisConsentManager;
import com.whis.app.core.WhisFlags;

import java.util.List;

/**
 * ViewModel business logic for AI Agent (AI_AGENT_PLAN.md Section 4.6 Day 7).
 */
public class AgentViewModel {

    public interface ViewModelCallback {
        void onMessageReceived(ChatMessage message);
        void onTriggerRedAlert();
        void onConsentRequired();
        void onError(String errorMessage);
    }

    private final Context context;
    private final GeminiAgentClient geminiClient;
    private final SessionContext sessionContext;
    private boolean forceNetworkFailure = false; // For testing offline fallback

    public AgentViewModel(Context context) {
        this.context = context.getApplicationContext();
        this.geminiClient = new GeminiAgentClient(this.context);
        this.sessionContext = new SessionContext();
    }

    public void setForceNetworkFailure(boolean forceFailure) {
        this.forceNetworkFailure = forceFailure;
    }

    public void sendUserMessage(String userMessage, MediaAttachment attachment, ViewModelCallback callback) {
        if (userMessage == null || userMessage.trim().isEmpty()) return;

        // Step 2: Auto-ensure Consent via Shared WhisConsentManager
        if (!WhisConsentManager.isConsentGiven(context)) {
            WhisConsentManager.saveConsent(context, true);
        }

        UserProfile profile = UserProfileContext.getProfile(context);
        sessionContext.addTurn(ChatMessage.ROLE_USER, userMessage);

        // Build Context & Prompt
        List<WhisFlags.FlagEntry> flags = ModuleContextInjector.getRecentFlags(context, 24);
        List<ChatMessage> history = sessionContext.getHistory();
        String systemPrompt = SystemPromptBuilder.build(context, profile, flags);

        // Call Gemini Client (100% Online Execution)
        AgentRequest request = new AgentRequest(systemPrompt, history, userMessage, attachment);
        geminiClient.sendMessage(request, new GeminiAgentClient.StreamCallback() {
            @Override
            public void onChunk(String textChunk) {
                // Streaming chunk callback
            }

            @Override
            public void onComplete(AgentResponse response) {
                sessionContext.addTurn(ChatMessage.ROLE_AGENT, response.getMessage());
                ChatMessage agentMsg = new ChatMessage(
                        ChatMessage.ROLE_AGENT,
                        response.getMessage(),
                        response.getRiskLevel(),
                        response.getOptionButtons()
                );
                if (callback != null) callback.onMessageReceived(agentMsg);
            }

            @Override
            public void onError(Exception e) {
                String errorMsg = e != null && e.getMessage() != null ? e.getMessage() : "Network error";
                if (callback != null) callback.onError(errorMsg);
            }
        });
    }

    private boolean isNetworkConnected() {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return true;

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                android.net.Network network = cm.getActiveNetwork();
                if (network == null) {
                    NetworkInfo info = cm.getActiveNetworkInfo();
                    return info != null && info.isConnectedOrConnecting();
                }
                android.net.NetworkCapabilities caps = cm.getNetworkCapabilities(network);
                return caps != null && (
                        caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) ||
                        caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                        caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
                );
            } else {
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
            }
        } catch (Exception e) {
            return true;
        }
    }

    public List<ChatMessage> getHistory() {
        return sessionContext.getHistory();
    }

    /** Clears all turns in the current session so the AI starts fresh. */
    public void clearHistory() {
        sessionContext.clearSession();
    }
}
