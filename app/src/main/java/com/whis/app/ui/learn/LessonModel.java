package com.whis.app.ui.learn;

/**
 * Data model representing a learning module / lesson item (UI_PLAN.md §3.4).
 */
public class LessonModel {
    public final String id;
    public final String title;
    public final String summary;
    public final String content;
    public int progressPercent; // 0 - 100

    public LessonModel(String id, String title, String summary, String content, int progressPercent) {
        this.id = id;
        this.title = title;
        this.summary = summary;
        this.content = content;
        this.progressPercent = progressPercent;
    }
}
