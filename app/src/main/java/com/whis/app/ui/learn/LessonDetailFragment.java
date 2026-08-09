package com.whis.app.ui.learn;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.whis.app.R;
import com.whis.app.ui.components.PrimaryButton;
import com.whis.app.ui.components.StatusCard;

import java.util.List;

/**
 * LessonDetailFragment — Renders the 4-question scam awareness framework for a specific chapter (LEARN_PLAN.md).
 */
public class LessonDetailFragment extends Fragment {

    public static final String ARG_LESSON_ID = "lesson_id";

    private LearnRepository repository;
    private LearnChapter chapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lesson_detail, container, false);
        view.setAlpha(0f);
        view.animate().alpha(1f).setDuration(200).start();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = LearnRepository.getInstance(requireContext());

        String chapterId = getArguments() != null ? getArguments().getString(ARG_LESSON_ID, "digital_arrest") : "digital_arrest";
        chapter = repository.getChapterById(chapterId);

        ProgressBar pbProgress = view.findViewById(R.id.pb_lesson_progress);
        TextView tvTag = view.findViewById(R.id.tv_chapter_tag);
        TextView tvTitle = view.findViewById(R.id.tv_lesson_title);

        androidx.core.view.ViewCompat.setTransitionName(tvTitle, "chapter_hero_" + chapterId);

        TextView tvWhatHappens = view.findViewById(R.id.tv_what_happens);
        TextView tvWhyItWorks = view.findViewById(R.id.tv_why_it_works);
        LinearLayout containerDoRightNow = view.findViewById(R.id.container_do_right_now);
        TextView tvHowWhisHelps = view.findViewById(R.id.tv_how_whis_helps);
        PrimaryButton btnComplete = view.findViewById(R.id.btn_complete_lesson);
        Button btnBack = view.findViewById(R.id.btn_lesson_back);
        Button btnCall1930 = view.findViewById(R.id.btn_detail_call_1930);

        Button btnAudio = view.findViewById(R.id.btn_audio_summary);
        LinearLayout containerQuiz = view.findViewById(R.id.container_quiz);

        LinearLayout containerWhatNotToDo = view.findViewById(R.id.container_what_not_to_do);
        Button btnGenerateAiStory = view.findViewById(R.id.btn_generate_ai_story);

        if (btnCall1930 != null) {
            btnCall1930.setOnClickListener(v -> dial1930());
        }

        if (chapter != null) {
            boolean isCompleted = repository.isChapterCompleted(chapter.chapterId);
            pbProgress.setProgress(isCompleted ? 100 : 35);

            tvTag.setText(chapter.shortName.toUpperCase());
            tvTitle.setText(chapter.title);

            tvWhatHappens.setText(chapter.whatHappens);
            tvWhyItWorks.setText(chapter.whyItWorks);
            tvHowWhisHelps.setText(chapter.howWhisHelps);

            // Render 3 action steps for "What do I do right now?" (Green)
            renderActionSteps(containerDoRightNow, chapter.doRightNow, true);

            // Render 3 mistake steps for "What NOT to do" (Red)
            if (containerWhatNotToDo != null) {
                renderActionSteps(containerWhatNotToDo, chapter.whatNotToDo, false);
            }

            // ── AI Story Generator Button Handler ──────────────────────────────
            if (btnGenerateAiStory != null) {
                btnGenerateAiStory.setOnClickListener(v -> triggerAiStoryGeneration());
            }

            // ── Feature 5 Upgrade 1: Text-to-Speech Audio Summary ───────────────
            if (btnAudio != null) {
                btnAudio.setOnClickListener(v -> speakAudioSummary(chapter));
            }

            // ── Feature 5 Upgrade 2: Interactive Scam Quiz ───────────────────────
            if (containerQuiz != null) {
                renderQuiz(containerQuiz, chapter.chapterId);
            }

            if (isCompleted) {
                btnComplete.setText("Completed ✓");
            }

            btnComplete.setOnClickListener(v -> {
                repository.setChapterCompleted(chapter.chapterId, true);
                pbProgress.setProgress(100);
                btnComplete.setText("Completed ✓");
                Toast.makeText(requireContext(), "Chapter marked as completed! (+50 XP)", Toast.LENGTH_SHORT).show();
            });
        }

        btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
    }

    private void dial1930() {
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:1930"));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Dialing 1930 Helpline...", Toast.LENGTH_SHORT).show();
        }
    }

    private void triggerAiStoryGeneration() {
        Toast.makeText(requireContext(), "✨ Generating AI Scam Story...", Toast.LENGTH_SHORT).show();
        LearnStoryGenerator.generateNewStory(requireContext(), new LearnStoryGenerator.StoryCallback() {
            @Override
            public void onStoryGenerated(LearnChapter newChapter) {
                if (repository != null) {
                    repository.addDynamicChapter(requireContext(), newChapter);
                }
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("🎉 New Story Unlocked!")
                        .setMessage("Unlocked: " + newChapter.title + "\n\nStory: " + newChapter.whatHappens.substring(0, Math.min(120, newChapter.whatHappens.length())) + "...")
                        .setPositiveButton("Read Story 📖", (dialog, which) -> {
                            Bundle args = new Bundle();
                            args.putString(ARG_LESSON_ID, newChapter.chapterId);
                            Navigation.findNavController(requireView()).navigate(R.id.action_learn_to_lesson_detail, args);
                        })
                        .setNegativeButton("Dismiss", null)
                        .show();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(requireContext(), "Failed to generate story", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderActionSteps(LinearLayout container, List<String> steps, boolean isPositive) {
        container.removeAllViews();
        if (steps == null || steps.isEmpty()) return;

        for (int i = 0; i < steps.size(); i++) {
            String stepText = steps.get(i);

            StatusCard card = new StatusCard(requireContext());
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            );
            cardParams.setMargins(0, 0, 0, (int) (8 * getResources().getDisplayMetrics().density));
            card.setLayoutParams(cardParams);

            LinearLayout layout = new LinearLayout(requireContext());
            layout.setOrientation(LinearLayout.HORIZONTAL);
            layout.setGravity(android.view.Gravity.TOP);

            // Step number badge (Green if Sahi Kadam, Red if Galti Mat Karna)
            TextView tvStepNum = new TextView(requireContext());
            tvStepNum.setText(isPositive ? "✅ Step " + (i + 1) : "❌ Avoid " + (i + 1));
            tvStepNum.setTextSize(12);
            tvStepNum.setTypeface(null, android.graphics.Typeface.BOLD);
            tvStepNum.setTextColor(getResources().getColor(R.color.whis_bg, requireContext().getTheme()));
            tvStepNum.setBackgroundResource(isPositive ? R.drawable.bg_icon_circle_trusted : R.drawable.bg_hero_glass_glow);
            if (!isPositive) {
                tvStepNum.setBackgroundColor(0xFFFF4D4D);
            }
            tvStepNum.setPadding(
                    (int) (8 * getResources().getDisplayMetrics().density),
                    (int) (4 * getResources().getDisplayMetrics().density),
                    (int) (8 * getResources().getDisplayMetrics().density),
                    (int) (4 * getResources().getDisplayMetrics().density)
            );
            LinearLayout.LayoutParams numParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            );
            numParams.setMargins(0, 0, (int) (12 * getResources().getDisplayMetrics().density), 0);
            tvStepNum.setLayoutParams(numParams);

            // Step text
            TextView tvText = new TextView(requireContext());
            tvText.setText(stepText);
            tvText.setTextSize(15);
            tvText.setTextColor(getResources().getColor(R.color.whis_text_hi, requireContext().getTheme()));
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
            );
            tvText.setLayoutParams(textParams);

            layout.addView(tvStepNum);
            layout.addView(tvText);

            card.addView(layout);
            container.addView(card);
        }
    }

    private android.speech.tts.TextToSpeech tts;

    private void speakAudioSummary(LearnChapter chapter) {
        if (chapter == null) return;
        String summaryToSpeak = "Chapter: " + chapter.title + ". "
                + "What happens: " + chapter.whatHappens + " "
                + "What to do right now: " + (chapter.doRightNow != null && !chapter.doRightNow.isEmpty() ? chapter.doRightNow.get(0) : "");

        if (tts == null) {
            tts = new android.speech.tts.TextToSpeech(requireContext(), status -> {
                if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                    tts.setLanguage(new java.util.Locale("en", "IN"));
                    tts.speak(summaryToSpeak, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "whis_tts");
                }
            });
        } else {
            tts.speak(summaryToSpeak, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "whis_tts");
        }
        Toast.makeText(requireContext(), "🔊 Playing audio walkthrough...", Toast.LENGTH_SHORT).show();
    }

    private void renderQuiz(LinearLayout container, String chapterId) {
        container.removeAllViews();

        String qText = "Q: Can Indian police or CBI perform a 'digital arrest' over video call and demand money?";
        boolean correctAnswer = false; // False — digital arrest is 100% fake

        TextView tvQuestion = new TextView(requireContext());
        tvQuestion.setText(qText);
        tvQuestion.setTextSize(15);
        tvQuestion.setTypeface(null, android.graphics.Typeface.BOLD);
        tvQuestion.setTextColor(getResources().getColor(R.color.whis_text_hi, requireContext().getTheme()));

        LinearLayout btnRow = new LinearLayout(requireContext());
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        btnRow.setPadding(0, 12, 0, 0);

        Button btnTrue = new Button(requireContext());
        btnTrue.setText("YES (True)");
        btnTrue.setTextSize(13);
        btnTrue.setAllCaps(false);
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        p1.setMarginEnd(8);
        btnTrue.setLayoutParams(p1);

        Button btnFalse = new Button(requireContext());
        btnFalse.setText("NO (False)");
        btnFalse.setTextSize(13);
        btnFalse.setAllCaps(false);
        LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        btnFalse.setLayoutParams(p2);

        TextView tvFeedback = new TextView(requireContext());
        tvFeedback.setTextSize(14);
        tvFeedback.setPadding(0, 12, 0, 0);

        btnTrue.setOnClickListener(v -> {
            tvFeedback.setText("❌ Incorrect! Indian law has NO digital arrest over video calls. It is always a scam.");
            tvFeedback.setTextColor(0xFFFF4D4D);
        });

        btnFalse.setOnClickListener(v -> {
            tvFeedback.setText("✅ Correct! (+50 XP) No agency can arrest over video or ask money to avoid arrest.");
            tvFeedback.setTextColor(0xFF4CAF50);
        });

        btnRow.addView(btnTrue);
        btnRow.addView(btnFalse);

        container.addView(tvQuestion);
        container.addView(btnRow);
        container.addView(tvFeedback);
    }

    @Override
    public void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
