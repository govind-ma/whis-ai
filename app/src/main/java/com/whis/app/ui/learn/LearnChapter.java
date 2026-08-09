package com.whis.app.ui.learn;

import java.util.ArrayList;
import java.util.List;

/**
 * Data model for a chapter in the Whis Knowledge Book (LEARN_PLAN.md).
 * Implements the 4-question scam awareness framework.
 */
public class LearnChapter {
    public final String chapterId;
    public final String title;
    public final String shortName;
    public final List<String> searchTriggers;
    public final String whatHappens;
    public final String whyItWorks;
    public final List<String> doRightNow;
    public final String howWhisHelps;
    public final List<String> crossReference;
    public final String sourceConfidence;

    public static class QuizQuestion {
        public final String question;
        public final boolean isTrueCorrect;
        public final String explanation;

        public QuizQuestion(String question, boolean isTrueCorrect, String explanation) {
            this.question = question;
            this.isTrueCorrect = isTrueCorrect;
            this.explanation = explanation;
        }
    }

    public List<QuizQuestion> quizQuestions = new ArrayList<>();
    public List<String> whatNotToDo = new ArrayList<>();

    public LearnChapter(String chapterId, String title, String shortName,
                        String sourceConfidence, String whatHappens, String whyItWorks,
                        List<String> doRightNow, String howWhisHelps) {
        this(chapterId, title, shortName, new ArrayList<>(), whatHappens, whyItWorks, doRightNow, howWhisHelps, new ArrayList<>(), sourceConfidence);
    }

    public LearnChapter(String chapterId, String title, String shortName,
                        List<String> searchTriggers, String whatHappens, String whyItWorks,
                        List<String> doRightNow, String howWhisHelps,
                        List<String> crossReference, String sourceConfidence) {
        this.chapterId = chapterId;
        this.title = title;
        this.shortName = shortName;
        this.searchTriggers = searchTriggers != null ? searchTriggers : new ArrayList<>();
        this.whatHappens = whatHappens;
        this.whyItWorks = whyItWorks;
        this.doRightNow = doRightNow != null ? doRightNow : new ArrayList<>();
        this.howWhisHelps = howWhisHelps;
        this.crossReference = crossReference != null ? crossReference : new ArrayList<>();
        this.sourceConfidence = sourceConfidence;
    }

    /**
     * Search matching helper. Checks if query matches title, shortName, whatHappens, or searchTriggers.
     */
    public boolean matchesSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            return true;
        }
        String q = query.toLowerCase().trim();
        if (title != null && title.toLowerCase().contains(q)) return true;
        if (shortName != null && shortName.toLowerCase().contains(q)) return true;
        if (whatHappens != null && whatHappens.toLowerCase().contains(q)) return true;

        for (String trigger : searchTriggers) {
            if (trigger != null && trigger.toLowerCase().contains(q)) {
                return true;
            }
        }
        return false;
    }
}
