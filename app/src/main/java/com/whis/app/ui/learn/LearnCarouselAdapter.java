package com.whis.app.ui.learn;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.FragmentNavigator;
import androidx.recyclerview.widget.RecyclerView;

import com.whis.app.R;

import java.util.List;

/**
 * Adapter for Learn chapter horizontal carousel with peek scaling and shared element navigation.
 */
public class LearnCarouselAdapter extends RecyclerView.Adapter<LearnCarouselAdapter.ViewHolder> {

    public interface OnChapterClickListener {
        void onChapterClick(LearnChapter chapter, View sharedView);
    }

    private final List<LearnChapter> chapters;
    private final LearnRepository repository;
    private final OnChapterClickListener clickListener;

    public LearnCarouselAdapter(@NonNull List<LearnChapter> chapters,
                                @NonNull LearnRepository repository,
                                @NonNull OnChapterClickListener clickListener) {
        this.chapters = chapters;
        this.repository = repository;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_learn_carousel, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LearnChapter chapter = chapters.get(position);
        boolean isCompleted = repository.isChapterCompleted(chapter.chapterId);

        holder.tvTag.setText(chapter.shortName.toUpperCase());
        holder.tvTitle.setText(chapter.title);

        String snippet = chapter.whatHappens;
        if (snippet.length() > 100) snippet = snippet.substring(0, 97) + "...";
        holder.tvSummary.setText(snippet);

        if (isCompleted) {
            holder.tvBadge.setText("Completed ✓");
            holder.tvBadge.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.whis_trusted, holder.itemView.getContext().getTheme()));
        } else {
            holder.tvBadge.setText("Read Story 📖");
            holder.tvBadge.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.whis_trusted, holder.itemView.getContext().getTheme()));
        }

        // Shared element transition name setup
        String transitionName = "chapter_hero_" + chapter.chapterId;
        ViewCompat.setTransitionName(holder.heroContainer, transitionName);

        // Thin green progress bar animation (0-100% fill)
        int targetProgress = isCompleted ? 100 : 0;
        holder.pbProgress.setProgress(0);

        if (targetProgress > 0) {
            ValueAnimator anim = ValueAnimator.ofInt(0, targetProgress);
            anim.setDuration(500);
            anim.setInterpolator(new FastOutSlowInInterpolator());
            anim.addUpdateListener(animation -> holder.pbProgress.setProgress((int) animation.getAnimatedValue()));
            anim.start();
        }

        holder.itemView.setOnClickListener(v -> clickListener.onChapterClick(chapter, holder.heroContainer));
    }

    @Override
    public int getItemCount() {
        return chapters.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final View heroContainer;
        final TextView tvTag;
        final TextView tvBadge;
        final TextView tvTitle;
        final TextView tvSummary;
        final ProgressBar pbProgress;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            heroContainer = itemView.findViewById(R.id.hero_thumbnail_container);
            tvTag = itemView.findViewById(R.id.tv_carousel_tag);
            tvBadge = itemView.findViewById(R.id.tv_carousel_badge);
            tvTitle = itemView.findViewById(R.id.tv_carousel_title);
            tvSummary = itemView.findViewById(R.id.tv_carousel_summary);
            pbProgress = itemView.findViewById(R.id.pb_card_progress);
        }
    }
}
