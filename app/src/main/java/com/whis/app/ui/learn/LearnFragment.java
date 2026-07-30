package com.whis.app.ui.learn;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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
import com.whis.app.ui.components.StatusCard;

import java.util.List;

/**
 * LearnFragment — Whis Knowledge Book Awareness Module (LEARN_PLAN.md).
 * Features horizontal carousel with peek effect, shared element transitions,
 * search-by-problem, 1930 national helpline quick dial, and dynamic chapter list.
 */
public class LearnFragment extends Fragment {

    private LearnRepository repository;
    private RecyclerView rvCarousel;
    private LinearLayout containerList;
    private EditText etSearch;
    private TextView tvProgressCount;
    private ProgressBar pbOverallProgress;
    private TextView tvNoResults;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_learn, container, false);
        // Window enter transition: fade in root layout alpha 0->1, 200ms
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
        etSearch = view.findViewById(R.id.et_search_learn);
        tvProgressCount = view.findViewById(R.id.tv_progress_count);
        pbOverallProgress = view.findViewById(R.id.pb_overall_progress);
        tvNoResults = view.findViewById(R.id.tv_no_results);

        Button btnCall1930 = view.findViewById(R.id.btn_call_1930_banner);
        if (btnCall1930 != null) {
            btnCall1930.setOnClickListener(v -> dial1930());
        }

        // Setup Carousel RecyclerView
        setupCarousel();

        // Setup search input listener
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterChapters(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        renderCarousel(repository.getAllChapters());
        updateProgress();
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

                    // Interpolate scale 1.0 -> 0.92 and alpha 1.0 -> 0.7
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
            if (tvNoResults != null) tvNoResults.setVisibility(View.VISIBLE);
            rvCarousel.setVisibility(View.GONE);
            containerList.setVisibility(View.GONE);
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

        // Initial scroll position trigger for peek animation
        rvCarousel.post(() -> {
            rvCarousel.scrollBy(1, 0);
            rvCarousel.scrollBy(-1, 0);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (repository != null) {
            updateProgress();
            if (etSearch != null) {
                filterChapters(etSearch.getText().toString());
            } else {
                renderCarousel(repository.getAllChapters());
            }
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

    private void filterChapters(String query) {
        List<LearnChapter> filtered = repository.searchChapters(query);
        renderCarousel(filtered);
    }

    private void updateProgress() {
        int completed = repository.getCompletedCount();
        int total = repository.getTotalCount();

        if (tvProgressCount != null) {
            tvProgressCount.setText(completed + " of " + total + " Completed");
        }

        if (pbOverallProgress != null) {
            int percent = total > 0 ? (completed * 100) / total : 0;
            pbOverallProgress.setProgress(percent);
        }
    }
}
