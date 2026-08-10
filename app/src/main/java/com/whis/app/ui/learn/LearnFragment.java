package com.whis.app.ui.learn;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.FragmentNavigator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.whis.app.R;

import java.util.List;

/**
 * LearnFragment — Whis Scam Story Library (LEARN_PLAN.md).
 * Displays real scam stories with practical solutions.
 */
public class LearnFragment extends Fragment {

    private LearnRepository repository;
    private RecyclerView rvCarousel;
    private LinearLayout containerList;
    private TextView tvNoResults;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_learn, container, false);
        view.setAlpha(0f);
        view.animate().alpha(1f).setDuration(200).start();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = LearnRepository.getInstance(requireContext());

        rvCarousel = view.findViewById(R.id.rv_learn_carousel);
        containerList = view.findViewById(R.id.container_lessons_list);
        tvNoResults = view.findViewById(R.id.tv_no_results);

        Button btnCall1930 = view.findViewById(R.id.btn_call_1930_banner);
        if (btnCall1930 != null) {
            btnCall1930.setOnClickListener(v -> dial1930());
        }

        // Setup Carousel RecyclerView
        setupCarousel();

        Button btnGenerateMain = view.findViewById(R.id.btn_generate_ai_story_main);
        if (btnGenerateMain != null) {
            btnGenerateMain.setOnClickListener(v -> triggerMainAiStoryGeneration());
        }

        renderCarousel(repository.getAllChapters());

        // Add scam education lesson cards below the carousel
        setupScamLessons(view);
    }

    private void triggerMainAiStoryGeneration() {
        Toast.makeText(requireContext(), "✨ Generating fresh AI Scam Story...", Toast.LENGTH_SHORT).show();
        LearnStoryGenerator.generateNewStory(requireContext(), new LearnStoryGenerator.StoryCallback() {
            @Override
            public void onStoryGenerated(LearnChapter newChapter) {
                if (repository != null) {
                    repository.addDynamicChapter(requireContext(), newChapter);
                    List<LearnChapter> all = repository.getAllChapters();
                    renderCarousel(all);
                    rvCarousel.smoothScrollToPosition(all.size() - 1);
                }
                Toast.makeText(requireContext(), "🎉 New Story Unlocked: " + newChapter.title, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(requireContext(), "Failed to generate story", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupCarousel() {
        LinearLayoutManager lm = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        rvCarousel.setLayoutManager(lm);

        PagerSnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(rvCarousel);

        // Scroll listener to animate peek cards: center item = scale 1.0/alpha 1.0, adjacent = scale 0.92/alpha 0.7
        rvCarousel.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                int centerX = recyclerView.getWidth() / 2;
                for (int i = 0; i < recyclerView.getChildCount(); i++) {
                    View child = recyclerView.getChildAt(i);
                    if (child == null) continue;

                    int childCenterX = (child.getLeft() + child.getRight()) / 2;
                    float d = Math.abs(centerX - childCenterX);
                    float maxD = recyclerView.getWidth() / 2f;

                    float fraction = Math.min(1.0f, d / maxD);

                    float scale = 1.0f - (0.08f * fraction);
                    float alpha = 1.0f - (0.30f * fraction);

                    child.setScaleX(scale);
                    child.setScaleY(scale);
                    child.setAlpha(alpha);
                }
            }
        });
    }

    private void renderCarousel(List<LearnChapter> chapters) {
        if (chapters.isEmpty()) {
            if (tvNoResults != null) {
                tvNoResults.setText("📭 No stories available\n\nTap ✨ Generate Story below to create a new AI scam story.");
                tvNoResults.setVisibility(View.VISIBLE);
            }
            rvCarousel.setVisibility(View.GONE);
            if (containerList != null) containerList.setVisibility(View.GONE);
            // Show the generate button prominently when list is empty
            View btnGenerateMain = getView() != null ? getView().findViewById(R.id.btn_generate_ai_story_main) : null;
            if (btnGenerateMain != null) btnGenerateMain.setVisibility(View.VISIBLE);
            return;
        }

        if (tvNoResults != null) tvNoResults.setVisibility(View.GONE);
        rvCarousel.setVisibility(View.VISIBLE);

        LearnCarouselAdapter adapter = new LearnCarouselAdapter(chapters, repository, (chapter, sharedView) -> {
            Bundle args = new Bundle();
            args.putString(LessonDetailFragment.ARG_LESSON_ID, chapter.chapterId);

            String transitionName = ViewCompat.getTransitionName(sharedView);
            FragmentNavigator.Extras extras = null;
            if (transitionName != null) {
                extras = new FragmentNavigator.Extras.Builder()
                        .addSharedElement(sharedView, transitionName)
                        .build();
            }

            if (extras != null) {
                Navigation.findNavController(sharedView).navigate(R.id.action_learn_to_lesson_detail, args, null, extras);
            } else {
                Navigation.findNavController(sharedView).navigate(R.id.action_learn_to_lesson_detail, args);
            }
        });

        rvCarousel.setAdapter(adapter);

        rvCarousel.post(() -> {
            rvCarousel.scrollBy(1, 0);
            rvCarousel.scrollBy(-1, 0);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (repository != null) {
            renderCarousel(repository.getAllChapters());
        }
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

    private void setupScamLessons(View rootView) {
        LinearLayout container = rootView.findViewById(R.id.container_lessons_list);
        if (container == null) return;

        container.removeAllViews();

        // Section header
        TextView header = new TextView(requireContext());
        header.setText("\uD83D\uDCDA Scam Awareness Guide — जानें, बचें");
        header.setTextSize(16f);
        header.setTextColor(0xFFEEEEEE);
        header.setPadding(0, 48, 0, 16);
        android.graphics.Typeface bold = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        header.setTypeface(bold);
        container.addView(header);

        java.util.List<ScamLessonData.Lesson> lessons = ScamLessonData.getAllLessons();
        for (ScamLessonData.Lesson lesson : lessons) {
            addLessonCard(container, lesson);
        }
    }

    private void addLessonCard(LinearLayout container, ScamLessonData.Lesson lesson) {
        // Card wrapper
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(40, 36, 40, 36);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(0xFF1A1A2E);
        bg.setCornerRadius(24f);
        bg.setStroke(2, 0xFF2A2A4A);
        card.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 24);
        card.setLayoutParams(lp);

        // Title row
        TextView title = new TextView(requireContext());
        title.setText(lesson.emoji + "  " + lesson.titleHindi + "  |  " + lesson.titleEnglish);
        title.setTextSize(15f);
        title.setTextColor(0xFFFFFFFF);
        android.graphics.Typeface bold = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        title.setTypeface(bold);
        card.addView(title);

        // Example script
        TextView example = new TextView(requireContext());
        example.setText("\uD83D\uDDE3\uFE0F " + lesson.exampleScript);
        example.setTextSize(13f);
        example.setTextColor(0xFFAAAAAA);
        example.setPadding(0, 16, 0, 8);
        card.addView(example);

        // Red flags
        for (String flag : lesson.redFlags) {
            TextView flagView = new TextView(requireContext());
            flagView.setText(flag);
            flagView.setTextSize(13f);
            flagView.setTextColor(0xFFFF6B6B);
            flagView.setPadding(0, 4, 0, 4);
            card.addView(flagView);
        }

        // What to do
        TextView whatToDo = new TextView(requireContext());
        whatToDo.setText(lesson.whatToDo);
        whatToDo.setTextSize(13f);
        whatToDo.setTextColor(0xFF4CAF50);
        whatToDo.setPadding(0, 12, 0, 4);
        card.addView(whatToDo);

        // Helpline
        TextView helpline = new TextView(requireContext());
        helpline.setText("\uD83D\uDCDE " + lesson.helpline);
        helpline.setTextSize(12f);
        helpline.setTextColor(0xFF888888);
        helpline.setPadding(0, 4, 0, 0);
        card.addView(helpline);

        container.addView(card);
    }
}
