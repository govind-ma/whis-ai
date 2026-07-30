package com.whis.app.agent;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.whis.app.R;
import com.whis.app.agent.context.UserProfileContext;
import com.whis.app.agent.emergency.EmergencyContactNotifier;
import com.whis.app.agent.model.UserProfile;
import com.whis.app.agent.offline.OfflineKnowledgeBase;

/**
 * Full-screen Emergency Red Alert Activity (AI_AGENT_PLAN.md Section 4.7 Day 9).
 * <p>
 * Entry sequence on onCreate (chained with AnimatorSet):
 * <ul>
 *   <li>Step 1: Full screen red flash — overlay view alpha 0.3→0, 100ms</li>
 *   <li>Step 2: Title "GHABRAO MAT" — reveal letter by letter using Handler with 30ms delay</li>
 *   <li>Step 3: 3 action buttons slide up from bottom with 100ms stagger (translateY +60dp → 0, alpha 0→1)</li>
 *   <li>Vignette pulse: alpha 0.2→0.4→0.2 on 3000ms loop</li>
 *   <li>Reduced motion check via Settings.Global animator duration scale</li>
 * </ul>
 */
public class RedAlertActivity extends AppCompatActivity {

    private View flashOverlay;
    private View vignetteOverlay;
    private TextView tvTitle;
    private Button btn1930;
    private Button btnBank;
    private Button btnFamily;
    private Button btnDismiss;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final String titleText = "⚠ GHABRAO MAT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_red_alert);

        flashOverlay = findViewById(R.id.flash_overlay);
        vignetteOverlay = findViewById(R.id.vignette_overlay);
        tvTitle = findViewById(R.id.tv_red_alert_title);
        btn1930 = findViewById(R.id.btn_call_1930);
        btnBank = findViewById(R.id.btn_call_bank);
        btnFamily = findViewById(R.id.btn_alert_family);
        btnDismiss = findViewById(R.id.btn_dismiss_red_alert);

        UserProfile profile = UserProfileContext.getProfile(this);
        String bankHelpline = OfflineKnowledgeBase.getBankHelpline(this, profile.bankName);

        if (bankHelpline != null) {
            btnBank.setText("2. CALL " + (profile.bankName != null ? profile.bankName : "BANK") + " (" + bankHelpline + ")");
        }

        btn1930.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:1930"));
            startActivity(intent);
        });

        btnBank.setOnClickListener(v -> {
            String number = bankHelpline != null ? bankHelpline.replaceAll("[^0-9]", "") : "1930";
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number));
            startActivity(intent);
        });

        btnFamily.setOnClickListener(v -> {
            EmergencyContactNotifier.sendEmergencyAlert(RedAlertActivity.this, profile);
        });

        btnDismiss.setOnClickListener(v -> finish());

        // Check system reduced motion setting
        if (isReducedMotionEnabled()) {
            tvTitle.setText(titleText);
            flashOverlay.setVisibility(View.GONE);
            return;
        }

        startEntrySequence();
        startVignettePulse();
    }

    private boolean isReducedMotionEnabled() {
        try {
            float durationScale = Settings.Global.getFloat(
                    getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f);
            return durationScale == 0f;
        } catch (Exception e) {
            return false;
        }
    }

    private void startEntrySequence() {
        // Prepare initial button states for slide-up
        float fromY = dpToPx(60);
        Button[] buttons = new Button[]{btn1930, btnBank, btnFamily};
        for (Button btn : buttons) {
            btn.setAlpha(0f);
            btn.setTranslationY(fromY);
        }

        // Step 1: Full screen red flash — overlay view alpha 0.3 -> 0 in 100ms
        ObjectAnimator flashAnim = ObjectAnimator.ofFloat(flashOverlay, View.ALPHA, 0.3f, 0f);
        flashAnim.setDuration(100);

        flashAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                flashOverlay.setVisibility(View.GONE);
                // Step 2: Title typewriter reveal (30ms per char)
                typewriterTitle(() -> {
                    // Step 3: 3 action buttons slide up from bottom with 100ms stagger
                    animateButtonsIn(buttons);
                });
            }
        });

        flashAnim.start();
    }

    private void typewriterTitle(Runnable onComplete) {
        tvTitle.setText("");
        final int length = titleText.length();
        final int[] index = {0};

        Runnable charRunnable = new Runnable() {
            @Override
            public void run() {
                if (index[0] < length) {
                    tvTitle.setText(titleText.substring(0, index[0] + 1));
                    index[0]++;
                    mainHandler.postDelayed(this, 30);
                } else if (onComplete != null) {
                    onComplete.run();
                }
            }
        };

        mainHandler.postDelayed(charRunnable, 30);
    }

    private void animateButtonsIn(Button[] buttons) {
        FastOutSlowInInterpolator interpolator = new FastOutSlowInInterpolator();
        float fromY = dpToPx(60);

        for (int i = 0; i < buttons.length; i++) {
            Button btn = buttons[i];
            long delay = i * 100L;

            ObjectAnimator slideUp = ObjectAnimator.ofFloat(btn, View.TRANSLATION_Y, fromY, 0f);
            slideUp.setDuration(300);
            slideUp.setStartDelay(delay);
            slideUp.setInterpolator(interpolator);

            ObjectAnimator fadeIn = ObjectAnimator.ofFloat(btn, View.ALPHA, 0f, 1f);
            fadeIn.setDuration(300);
            fadeIn.setStartDelay(delay);
            fadeIn.setInterpolator(interpolator);

            AnimatorSet set = new AnimatorSet();
            set.playTogether(slideUp, fadeIn);
            set.start();
        }
    }

    private void startVignettePulse() {
        ObjectAnimator pulse = ObjectAnimator.ofFloat(vignetteOverlay, View.ALPHA, 0.2f, 0.4f, 0.2f);
        pulse.setDuration(3000);
        pulse.setRepeatCount(ObjectAnimator.INFINITE);
        pulse.setRepeatMode(ObjectAnimator.RESTART);
        pulse.setInterpolator(new FastOutSlowInInterpolator());
        pulse.start();
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
