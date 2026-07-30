package com.whis.app.agent.api;

import com.whis.app.agent.model.ChatMessage;
import com.whis.app.agent.model.MediaAttachment;

import java.util.List;

/**
 * Request container for Gemini API calls (AI_AGENT_PLAN.md Section 4.2 Day 6).
 */
public class AgentRequest {

    public String systemPrompt;
    public List<ChatMessage> history;
    public String userMessage;
    public MediaAttachment attachment;

    public AgentRequest(String systemPrompt, List<ChatMessage> history, String userMessage, MediaAttachment attachment) {
        this.systemPrompt = systemPrompt;
        this.history = history;
        this.userMessage = userMessage;
        this.attachment = attachment;
    }
}
