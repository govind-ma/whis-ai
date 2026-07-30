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

            // Render 3 action steps for "What do I do right now?"
            renderActionSteps(containerDoRightNow, chapter.doRightNow);

            if (isCompleted) {
                btnComplete.setText("Completed ✓");
            }

            btnComplete.setOnClickListener(v -> {
                repository.setChapterCompleted(chapter.chapterId, true);
                pbProgress.setProgress(100);
                btnComplete.setText("Completed ✓");
                Toast.makeText(requireContext(), "Chapter marked as completed!", Toast.LENGTH_SHORT).show();
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

    private void renderActionSteps(LinearLayout container, List<String> steps) {
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

            // Step number badge
            TextView tvStepNum = new TextView(requireContext());
            tvStepNum.setText("Step " + (i + 1));
            tvStepNum.setTextSize(12);
            tvStepNum.setTypeface(null, android.graphics.Typeface.BOLD);
            tvStepNum.setTextColor(getResources().getColor(R.color.whis_bg, requireContext().getTheme()));
            tvStepNum.setBackgroundResource(R.drawable.bg_icon_circle_trusted);
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
