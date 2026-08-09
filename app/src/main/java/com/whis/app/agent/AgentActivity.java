package com.whis.app.agent;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.whis.app.R;
import com.whis.app.agent.context.UserProfileContext;
import com.whis.app.agent.model.ChatMessage;
import com.whis.app.agent.model.UserProfile;

/**
 * Host Activity for Whis AI Agent Chat (AI_AGENT_PLAN.md Section 4.7 Day 9).
 * <p>
 * UI animation layers added (original AI logic untouched):
 * <ol>
 *   <li>Message bubble slide-up: translateY +20dp → 0, alpha 0→1, 250ms, FastOutSlowInInterpolator</li>
 *   <li>Typing indicator: {@link TypingIndicatorView} shown while awaiting AI response</li>
 *   <li>Input border: focus animates stroke color #2A2A2A → #41C85A over 200ms (ValueAnimator)</li>
 *   <li>Send button: 360° rotation in 300ms on every click (ObjectAnimator)</li>
 * </ol>
 */
public class AgentActivity extends AppCompatActivity {

    // ── Constants ────────────────────────────────────────────────────────────
    private static final int COLOR_BORDER_IDLE  = 0xFF2A2A2A;
    private static final int COLOR_BORDER_FOCUS = 0xFF41C85A;
    private static final int COLOR_USER_BUBBLE  = 0xFF2A3A2A; // dark green tint for user
    private static final int COLOR_WHIS_BUBBLE  = 0xFF1E1E1E; // whis_surface

    // ── Views ────────────────────────────────────────────────────────────────
    private LinearLayout chatContainer;
    private ScrollView scrollView;
    private EditText etInput;
    private ImageButton btnSend;
    private FrameLayout typingIndicatorRow;
    private TypingIndicatorView typingIndicator;
    private GradientDrawable inputBorderDrawable;

    // ── Business logic ───────────────────────────────────────────────────────
    private AgentViewModel viewModel;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agent);

        // Wire views
        chatContainer      = findViewById(R.id.chat_container);
        scrollView         = findViewById(R.id.scroll_view);
        etInput            = findViewById(R.id.et_input);
        btnSend            = findViewById(R.id.btn_send);
        typingIndicatorRow = findViewById(R.id.typing_indicator_row);
        typingIndicator    = findViewById(R.id.typing_indicator);

        // Setup animated input border
        setupInputBorder();

        // Ensure scrolling to bottom when input is focused or keyboard appears
        etInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                scrollToBottom();
            }
        });

        scrollView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (bottom < oldBottom) {
                scrollToBottom();
            }
        });

        // Send button click → rotation + send logic
        btnSend.setOnClickListener(v -> {
            animateSendButton(v);
            dispatchSendMessage();
        });

        // ── Clear Chat button ─────────────────────────────────────────────────
        android.widget.Button btnClearChat = findViewById(R.id.btn_clear_chat);
        if (btnClearChat != null) {
            btnClearChat.setOnClickListener(v -> {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Clear Chat")
                        .setMessage("Delete all messages in this conversation?")
                        .setPositiveButton("Clear", (dialog, which) -> {
                            chatContainer.removeAllViews();
                            if (viewModel != null) {
                                viewModel.clearHistory();
                            }
                            android.widget.Toast.makeText(this, "Chat cleared", android.widget.Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        // ── Original AI logic (untouched) ────────────────────────────────────
        viewModel = new AgentViewModel(this);

        UserProfile profile = UserProfileContext.getProfile(this);

        String initialContext = getIntent().getStringExtra("initial_context");
        if (initialContext != null && !initialContext.trim().isEmpty()) {
            appendUserBubble(initialContext);
            showTypingIndicator();
            viewModel.sendUserMessage(initialContext, null, buildCallback());
        }
    }

    // =========================================================================
    // Animation layer 1 — Message bubble entrance
    // =========================================================================

    /**
     * Appends a user message bubble (right-aligned) with slide-up animation.
     */
    private void appendUserBubble(String text) {
        View bubble = buildBubble(text, true);
        chatContainer.addView(bubble);
        animateBubbleEntrance(bubble);
        scrollToBottom();
    }

    /**
     * Appends an AI (Whis) message bubble (left-aligned) with slide-up animation.
     */
    private void appendWhisBubble(String text) {
        View bubble = buildBubble(text, false);
        chatContainer.addView(bubble);
        animateBubbleEntrance(bubble);
        scrollToBottom();
    }

    /**
     * Builds a styled message bubble view.
     *
     * @param text   message content
     * @param isUser {@code true} = right-aligned user bubble, {@code false} = left-aligned Whis bubble
     */
    private View buildBubble(String text, boolean isUser) {
        // Outer frame for gravity alignment
        FrameLayout frame = new FrameLayout(this);
        LinearLayout.LayoutParams frameParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        frameParams.setMargins(0, 0, 0, dpToPx(8));
        frame.setLayoutParams(frameParams);

        // Bubble text view
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(14f);
        tv.setLineSpacing(dpToPx(2), 1.0f);

        int hPad = dpToPx(12);
        int vPad = dpToPx(10);
        tv.setPadding(hPad, vPad, hPad, vPad);

        int userBgColor = getResources().getColor(R.color.whis_trusted, getTheme());
        int whisBgColor = getResources().getColor(R.color.whis_surface, getTheme());
        int borderColor = getResources().getColor(R.color.whis_border, getTheme());

        if (isUser) {
            tv.setTextColor(Color.WHITE);
        } else {
            tv.setTextColor(getResources().getColor(R.color.whis_text_hi, getTheme()));
        }

        // Bubble background pill
        GradientDrawable bubbleBg = new GradientDrawable();
        bubbleBg.setShape(GradientDrawable.RECTANGLE);

        // User bubbles: top-right sharp, Whis bubbles: top-left sharp
        float r = dpToPx(14);
        if (isUser) {
            bubbleBg.setCornerRadii(new float[]{r, r, dpToPx(4), dpToPx(4), r, r, r, r});
        } else {
            bubbleBg.setCornerRadii(new float[]{dpToPx(4), dpToPx(4), r, r, r, r, r, r});
        }
        bubbleBg.setColor(isUser ? userBgColor : whisBgColor);
        bubbleBg.setStroke(1, borderColor);
        tv.setBackground(bubbleBg);

        // Max width: 80% of screen
        int maxW = (int) (getResources().getDisplayMetrics().widthPixels * 0.80f);
        FrameLayout.LayoutParams tvParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tvParams.gravity = isUser ? Gravity.END : Gravity.START;
        tv.setMaxWidth(maxW);
        tv.setLayoutParams(tvParams);

        frame.addView(tv);
        return frame;
    }

    /**
     * Slide-up entrance: translateY +20dp → 0, alpha 0 → 1, 250ms, FastOutSlowInInterpolator.
     */
    private void animateBubbleEntrance(View bubble) {
        float fromY = dpToPx(20);
        bubble.setAlpha(0f);
        bubble.setTranslationY(fromY);

        FastOutSlowInInterpolator interp = new FastOutSlowInInterpolator();

        ObjectAnimator slideUp = ObjectAnimator.ofFloat(bubble, View.TRANSLATION_Y, fromY, 0f);
        slideUp.setDuration(250);
        slideUp.setInterpolator(interp);

        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(bubble, View.ALPHA, 0f, 1f);
        fadeIn.setDuration(250);
        fadeIn.setInterpolator(interp);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(slideUp, fadeIn);
        set.start();
    }

    // =========================================================================
    // Animation layer 2 — Typing indicator
    // =========================================================================

    private void showTypingIndicator() {
        mainHandler.post(() -> {
            typingIndicatorRow.setVisibility(View.VISIBLE);
            typingIndicator.startAnimation();
            scrollToBottom();
        });
    }

    private void hideTypingIndicator() {
        mainHandler.post(() -> {
            typingIndicator.stopAnimation();
            typingIndicatorRow.setVisibility(View.GONE);
        });
    }

    // =========================================================================
    // Animation layer 3 — Input border focus animation
    // =========================================================================

    private void setupInputBorder() {
        int bgColor     = getResources().getColor(R.color.whis_surface, getTheme());
        int borderIdle  = getResources().getColor(R.color.whis_border, getTheme());
        int borderFocus = getResources().getColor(R.color.whis_trusted, getTheme());
        int textColor   = getResources().getColor(R.color.whis_text_hi, getTheme());
        int hintColor   = getResources().getColor(R.color.whis_text_mid, getTheme());

        etInput.setTextColor(textColor);
        etInput.setHintTextColor(hintColor);

        inputBorderDrawable = new GradientDrawable();
        inputBorderDrawable.setShape(GradientDrawable.RECTANGLE);
        inputBorderDrawable.setCornerRadius(dpToPx(10));
        inputBorderDrawable.setColor(bgColor);
        inputBorderDrawable.setStroke(dpToPx(1), borderIdle);
        etInput.setBackground(inputBorderDrawable);

        etInput.setOnFocusChangeListener((v, hasFocus) -> {
            int fromColor = hasFocus ? borderIdle : borderFocus;
            int toColor   = hasFocus ? borderFocus : borderIdle;
            animateBorderColor(fromColor, toColor);
        });
    }

    private void animateBorderColor(int fromColor, int toColor) {
        ValueAnimator anim = ValueAnimator.ofArgb(fromColor, toColor);
        anim.setDuration(200);
        anim.setInterpolator(new FastOutSlowInInterpolator());
        anim.addUpdateListener(animation -> {
            int color = (int) animation.getAnimatedValue();
            // mutate() ensures we don't affect other views sharing this drawable
            ((GradientDrawable) etInput.getBackground().mutate()).setStroke(dpToPx(1), color);
        });
        anim.start();
    }

    // =========================================================================
    // Animation layer 4 — Send button 360° spin
    // =========================================================================

    private void animateSendButton(View button) {
        ObjectAnimator spin = ObjectAnimator.ofFloat(button, View.ROTATION, 0f, 360f);
        spin.setDuration(300);
        spin.setInterpolator(new FastOutSlowInInterpolator());
        spin.start();
        // Reset rotation after animation so next click starts from 0
        spin.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                button.setRotation(0f);
            }
        });
    }

    // =========================================================================
    // Message dispatch + ViewModel callback
    // =========================================================================

    private void dispatchSendMessage() {
        String text = etInput.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        etInput.setText("");
        etInput.clearFocus();
        appendUserBubble(text);
        showTypingIndicator();

        viewModel.sendUserMessage(text, null, buildCallback());
    }

    private AgentViewModel.ViewModelCallback buildCallback() {
        return new AgentViewModel.ViewModelCallback() {
            @Override
            public void onMessageReceived(ChatMessage message) {
                hideTypingIndicator();
                mainHandler.post(() -> appendWhisBubble(message.content));
            }

            @Override
            public void onTriggerRedAlert() {
                hideTypingIndicator();
                startActivity(new Intent(AgentActivity.this, RedAlertActivity.class));
            }

            @Override
            public void onConsentRequired() {
                hideTypingIndicator();
                mainHandler.post(() ->
                        appendWhisBubble("Please complete onboarding to enable full AI protection."));
            }

            @Override
            public void onError(String errorMessage) {
                hideTypingIndicator();
                mainHandler.post(() -> appendWhisBubble("Error: " + (errorMessage != null ? errorMessage : "Failed to reach server.")));
            }
        };
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void scrollToBottom() {
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

    private int dpToPx(float dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }
}
