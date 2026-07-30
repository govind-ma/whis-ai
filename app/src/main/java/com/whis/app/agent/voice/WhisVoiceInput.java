package com.whis.app.agent.voice;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;

/**
 * Android SpeechRecognizer wrapper for hands-free voice input (AI_AGENT_PLAN.md Section 4.2 Day 8).
 */
public class WhisVoiceInput {

    public interface VoiceInputListener {
        void onSpeechRecognized(String text);
        void onError(String errorMsg);
    }

    private final Context context;
    private SpeechRecognizer speechRecognizer;

    public WhisVoiceInput(Context context) {
        this.context = context.getApplicationContext();
    }

    public void startListening(String language, VoiceInputListener listener) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            if (listener != null) listener.onError("Voice recognition unavailable on this device");
            return;
        }

        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        if ("Gujarati".equalsIgnoreCase(language)) {
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "gu-IN");
        } else if ("English".equalsIgnoreCase(language)) {
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN");
        } else {
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN");
        }

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {}
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}

            @Override
            public void onError(int error) {
                if (listener != null) listener.onError("Speech recognition error code: " + error);
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty() && listener != null) {
                    listener.onSpeechRecognized(matches.get(0));
                }
            }

            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });

        speechRecognizer.startListening(intent);
    }

    public void stopListening() {
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
        }
    }

    public void destroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
    }
}
