package spirecoach.coaching;

import spirecoach.domain.CombatSnapshot;
import spirecoach.domain.Lesson;

import java.util.List;

public interface CoachingRule {
    String getId();

    /** Return zero or one lesson. Rules should stay conservative and silent when uncertain. */
    List<Lesson> evaluate(CombatSnapshot combat);
}
