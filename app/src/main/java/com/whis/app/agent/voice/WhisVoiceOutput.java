package com.whis.app.agent.voice;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

/**
 * Android TextToSpeech wrapper for accessibility voice response (AI_AGENT_PLAN.md Section 4.2 Day 8).
 */
public class WhisVoiceOutput implements TextToSpeech.OnInitListener {

    private TextToSpeech tts;
    private boolean isInitialized = false;

    public WhisVoiceOutput(Context context) {
        tts = new TextToSpeech(context.getApplicationContext(), this);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true;
            tts.setLanguage(new Locale("hi", "IN"));
        }
    }

    public void speak(String text, String language, boolean isSenior) {
        if (!isInitialized || tts == null || text == null) return;

        if ("Gujarati".equalsIgnoreCase(language)) {
            tts.setLanguage(new Locale("gu", "IN"));
        } else if ("English".equalsIgnoreCase(language)) {
            tts.setLanguage(Locale.ENGLISH);
        } else {
            tts.setLanguage(new Locale("hi", "IN"));
        }

        if (isSenior) {
            tts.setSpeechRate(0.75f); // Slower speech rate for elderly users
        } else {
            tts.setSpeechRate(1.0f);
        }

        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "WhisVoiceID");
    }

    public void stop() {
        if (tts != null) {
            tts.stop();
        }
    }

    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
    }
}
