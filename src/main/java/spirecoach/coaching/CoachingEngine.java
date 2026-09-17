package spirecoach.coaching;

import spirecoach.domain.CombatSnapshot;
import spirecoach.domain.Lesson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Runs modular rules and applies the product limit of at most two lessons per combat. */
public final class CoachingEngine {
    private static final int MAX_LESSONS_PER_COMBAT = 2;
    private final List<CoachingRule> rules;

    public CoachingEngine(List<CoachingRule> rules) {
        this.rules = rules == null
                ? Collections.<CoachingRule>emptyList()
                : Collections.unmodifiableList(new ArrayList<CoachingRule>(rules));
    }

    public List<Lesson> evaluate(CombatSnapshot combat) {
        List<Lesson> lessons = new ArrayList<Lesson>();
        for (CoachingRule rule : rules) {
            if (lessons.size() >= MAX_LESSONS_PER_COMBAT) break;
            List<Lesson> candidates = rule.evaluate(combat);
            if (candidates == null) continue;
            for (Lesson lesson : candidates) {
                if (lesson != null) {
                    lessons.add(lesson);
                    if (lessons.size() >= MAX_LESSONS_PER_COMBAT) break;
                }
            }
        }
        return Collections.unmodifiableList(lessons);
    }
}
