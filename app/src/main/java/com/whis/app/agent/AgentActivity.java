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

        // Allow sending message via Keyboard Enter / IME Send action
        etInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                animateSendButton(btnSend);
                dispatchSendMessage();
                return true;
            }
            return false;
        });

        // ── Top Bar 3-Dot Overflow Menu Handler ──────────────────────────────
        View btnAiMenu = findViewById(R.id.btn_ai_menu);
        if (btnAiMenu != null) {
            btnAiMenu.setOnClickListener(v -> {
                androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(this, v);
                popup.getMenu().add(0, 1, 0, "🧹 Clear Chat History");
                popup.getMenu().add(0, 2, 1, "📋 Copy Conversation");
                popup.getMenu().add(0, 3, 2, "🛡️ Reset AI Security Profile");
                popup.setOnMenuItemClickListener(item -> {
                    switch (item.getItemId()) {
                        case 1:
                            // Clear Chat History
                            new androidx.appcompat.app.AlertDialog.Builder(this)
                                    .setTitle("Clear Chat History")
                                    .setMessage("Are you sure you want to delete all messages in this conversation? This cannot be undone.")
                                    .setPositiveButton("Clear", (dialog, which) -> {
                                        chatContainer.removeAllViews();
                                        if (viewModel != null) {
                                            viewModel.clearHistory();
                                        }
                                        android.widget.Toast.makeText(this, "Chat history cleared", android.widget.Toast.LENGTH_SHORT).show();
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .show();
                            return true;

                        case 2:
                            // Copy Conversation to Clipboard
                            StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < chatContainer.getChildCount(); i++) {
                                View child = chatContainer.getChildAt(i);
                                if (child instanceof ViewGroup) {
                                    ViewGroup vg = (ViewGroup) child;
                                    for (int j = 0; j < vg.getChildCount(); j++) {
                                        View inner = vg.getChildAt(j);
                                        if (inner instanceof TextView) {
                                            sb.append(((TextView) inner).getText()).append("\n\n");
                                        }
                                    }
                                }
                            }
                            if (sb.length() > 0) {
                                android.content.ClipboardManager cm =
                                        (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                                if (cm != null) {
                                    cm.setPrimaryClip(android.content.ClipData.newPlainText("Whis AI Chat", sb.toString().trim()));
                                    android.widget.Toast.makeText(this, "📋 Conversation copied to clipboard", android.widget.Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                android.widget.Toast.makeText(this, "No messages to copy", android.widget.Toast.LENGTH_SHORT).show();
                            }
                            return true;

                        case 3:
                            // Reset AI Security Profile
                            chatContainer.removeAllViews();
                            if (viewModel != null) {
                                viewModel.clearHistory();
                            }
                            appendWhisBubble("Hello! I am your Whis AI Cyber Security Case Officer. How can I help protect you today?");
                            android.widget.Toast.makeText(this, "🛡️ AI Security Profile Reset", android.widget.Toast.LENGTH_SHORT).show();
                            return true;
                    }
                    return false;
                });
                popup.show();
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

        // Handle text shared from other apps (Share to Whis)
        handleSharedIntent(getIntent());
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
     * Appends an AI (Whis) message bubble (left-aligned) with optional quick reply option buttons.
     */
    private void appendWhisBubble(String text) {
        appendWhisBubble(text, null);
    }

    private void appendWhisBubble(String text, java.util.List<String> optionButtons) {
        View bubble = buildBubble(text, false);
        chatContainer.addView(bubble);
        animateBubbleEntrance(bubble);

        // Render option buttons if provided by AI
        if (optionButtons != null && !optionButtons.isEmpty()) {
            LinearLayout optionsContainer = new LinearLayout(this);
            optionsContainer.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, dpToPx(4), 0, dpToPx(8));
            optionsContainer.setLayoutParams(lp);

            for (String optionText : optionButtons) {
                if (optionText == null || optionText.trim().isEmpty()) continue;
                TextView btnOption = new TextView(this);
                btnOption.setText(optionText.trim());
                btnOption.setTextSize(13f);
                btnOption.setTextColor(Color.WHITE);
                btnOption.setPadding(dpToPx(14), dpToPx(10), dpToPx(14), dpToPx(10));

                GradientDrawable optionBg = new GradientDrawable();
                optionBg.setShape(GradientDrawable.RECTANGLE);
                optionBg.setCornerRadius(dpToPx(20));
                optionBg.setColor(0xFF2A2A4A);
                optionBg.setStroke(dpToPx(1), getResources().getColor(R.color.whis_trusted, getTheme()));
                btnOption.setBackground(optionBg);

                LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                btnLp.setMargins(0, dpToPx(4), 0, dpToPx(4));
                btnOption.setLayoutParams(btnLp);

                btnOption.setOnClickListener(v -> {
                    // Send selected option text as user input
                    etInput.setText(optionText.trim());
                    dispatchSendMessage();
                    optionsContainer.setVisibility(View.GONE);
                });

                optionsContainer.addView(btnOption);
            }
            chatContainer.addView(optionsContainer);
            animateBubbleEntrance(optionsContainer);
        }

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
        }
        bubbleBg.setColor(isUser ? userBgColor : whisBgColor);
        bubbleBg.setStroke(1, borderColor);
        tv.setBackground(bubbleBg);

        tv.setTextIsSelectable(true);

        // Long-press bubble to copy full message
        tv.setOnLongClickListener(v -> {
            android.content.ClipboardManager cm =
                    (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(android.content.ClipData.newPlainText("Whis AI Message", text));
                android.widget.Toast.makeText(this, "📋 Message copied to clipboard", android.widget.Toast.LENGTH_SHORT).show();
            }
            return true;
        });

        // Max width: 80% of screen
        int maxW = (int) (getResources().getDisplayMetrics().widthPixels * 0.80f);
        FrameLayout.LayoutParams tvParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tvParams.gravity = isUser ? Gravity.END : Gravity.START;
        tv.setMaxWidth(maxW);
        tv.setLayoutParams(tvParams);

        if (!isUser) {
            // For AI Whis bubbles, wrap in a vertical container with a 1-tap Copy action button
            LinearLayout aiGroup = new LinearLayout(this);
            aiGroup.setOrientation(LinearLayout.VERTICAL);
            FrameLayout.LayoutParams groupParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            groupParams.gravity = Gravity.START;
            aiGroup.setLayoutParams(groupParams);

            aiGroup.addView(tv);

            // 1-tap Copy button under AI response
            TextView btnCopy = new TextView(this);
            btnCopy.setText("📋 Copy text");
            btnCopy.setTextSize(11f);
            btnCopy.setTextColor(getResources().getColor(R.color.whis_trusted, getTheme()));
            btnCopy.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));
            btnCopy.setOnClickListener(v -> {
                android.content.ClipboardManager cm =
                        (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(android.content.ClipData.newPlainText("Whis AI Response", text));
                    android.widget.Toast.makeText(this, "📋 Text copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show();
                }
            });
            aiGroup.addView(btnCopy);
            frame.addView(aiGroup);
        } else {
            frame.addView(tv);
        }

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
        etInput.setEnabled(true);
        appendUserBubble(text);
        showTypingIndicator();

        viewModel.sendUserMessage(text, null, buildCallback());
    }

    private AgentViewModel.ViewModelCallback buildCallback() {
        return new AgentViewModel.ViewModelCallback() {
            @Override
            public void onMessageReceived(ChatMessage message) {
                hideTypingIndicator();
                mainHandler.post(() -> {
                    appendWhisBubble(message.content, message.optionButtons);
                    if (etInput != null) {
                        etInput.setEnabled(true);
                        etInput.setFocusable(true);
                        etInput.setFocusableInTouchMode(true);
                    }
                });
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

    /**
     * Handles text shared to Whis from other apps (WhatsApp messages, SMS screenshots etc.).
     * Pre-fills the input with the shared text and shows a prompt.
     */
    private void handleSharedIntent(Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        String type = intent.getType();
        if (android.content.Intent.ACTION_SEND.equals(action) && "text/plain".equals(type)) {
            String sharedText = intent.getStringExtra(android.content.Intent.EXTRA_TEXT);
            if (sharedText != null && !sharedText.trim().isEmpty()) {
                // Pre-fill input and show a helper prompt bubble
                if (etInput != null) {
                    etInput.setText("क्या यह message safe है?\n\n" + sharedText.trim());
                    etInput.setSelection(etInput.getText().length());
                }
                // Small delay to let UI settle, then show info toast
                mainHandler.postDelayed(() -> {
                    if (!isFinishing()) {
                        android.widget.Toast.makeText(this,
                                "\uD83D\uDD0D Whis को message share किया — Send दबाएं analysis के लिए",
                                android.widget.Toast.LENGTH_LONG).show();
                    }
                }, 600);
            }
        }
    }
}
