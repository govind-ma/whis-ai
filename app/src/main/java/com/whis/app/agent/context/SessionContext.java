package com.whis.app.agent.context;

import com.whis.app.agent.model.ChatMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds conversation history in memory (AI_AGENT_PLAN.md Section 4.2 Day 3).
 * Keeps last 10 turns for context continuity.
 */
public class SessionContext {

    private static final int MAX_TURNS = 10;
    private final List<ChatMessage> history;

    public SessionContext() {
        this.history = new ArrayList<>();
    }

    public synchronized void addTurn(String role, String content) {
        history.add(new ChatMessage(role, content));
        while (history.size() > MAX_TURNS) {
            history.remove(0);
        }
    }

    public synchronized List<ChatMessage> getHistory() {
        return new ArrayList<>(history);
    }

    public synchronized void clearSession() {
        history.clear();
    }
}
