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

            if (isCompleted) {
                btnComplete.setText("Completed ✓ (Tap to generate next)");
            }

            btnComplete.setOnClickListener(v -> {
                btnComplete.setEnabled(false);
                btnComplete.setText("✨ Generating New AI Scam Story...");
                Toast.makeText(requireContext(), "🎉 Story Completed! Deleting this story & generating a new AI story...", Toast.LENGTH_LONG).show();

                repository.deleteChapterAndReplaceWithAi(requireContext(), chapter.chapterId, new LearnStoryGenerator.StoryCallback() {
                    @Override
                    public void onStoryGenerated(LearnChapter newChapter) {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), "✨ New AI Story Unlocked: " + newChapter.title, Toast.LENGTH_LONG).show();
                        // Navigate back to updated Scam Stories library
                        Navigation.findNavController(requireView()).navigateUp();
                    }

                    @Override
                    public void onError(String error) {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), "Story completed!", Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(requireView()).navigateUp();
                    }
                });
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
        Toast.makeText(requireContext(), "✨ Generating fresh AI Scam Story...", Toast.LENGTH_SHORT).show();
        LearnStoryGenerator.generateNewStory(requireContext(), new LearnStoryGenerator.StoryCallback() {
            @Override
            public void onStoryGenerated(LearnChapter newChapter) {
                if (repository != null) {
                    repository.addDynamicChapter(requireContext(), newChapter);
                }
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("🎉 New Scam Story Unlocked!")
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
}
