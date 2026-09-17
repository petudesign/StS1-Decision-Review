package spirecoach.coaching;

import org.junit.Test;
import spirecoach.domain.ActionSnapshot;
import spirecoach.domain.CardSnapshot;
import spirecoach.domain.CombatSnapshot;
import spirecoach.domain.EnemySnapshot;
import spirecoach.domain.Lesson;
import spirecoach.domain.TurnSnapshot;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SequencingRuleTest {
    @Test
    public void reportsStrikeBeforeBashWithConcreteDifference() {
        TurnSnapshot before = state(1, 0, false, 46, 3);
        TurnSnapshot afterStrike = state(1, 0, false, 40, 2);
        TurnSnapshot afterBash = state(1, 0, true, 32, 0);

        ActionSnapshot strike = action(0, "Strike_R", "Strike", "ATTACK", 1, 6, 6, before, afterStrike);
        ActionSnapshot bash = action(1, "Bash", "Bash", "ATTACK", 2, 8, 8, afterStrike, afterBash);

        CombatSnapshot combat = combat(Arrays.asList(strike, bash));
        List<Lesson> lessons = new SequencingRule().evaluate(combat);

        assertEquals(1, lessons.size());
        assertEquals("Card order mattered here", lessons.get(0).getTitle());
        assertEquals(Integer.valueOf(6), lessons.get(0).getValues().get("actual_damage"));
        assertEquals(Integer.valueOf(9), lessons.get(0).getValues().get("alternative_damage"));
        assertEquals(Integer.valueOf(3), lessons.get(0).getValues().get("difference"));
        assertEquals("CERTAIN", lessons.get(0).getConfidence().name());
    }

    @Test
    public void staysSilentWhenBashWasAlreadyFirst() {
        TurnSnapshot start = state(1, 0, false, 46, 3);
        TurnSnapshot afterBash = state(1, 0, true, 38, 1);
        TurnSnapshot afterStrike = state(1, 0, true, 25, 0);

        ActionSnapshot bash = action(0, "Bash", "Bash", "ATTACK", 2, 8, 8, start, afterBash);
        ActionSnapshot strike = action(1, "Strike_R", "Strike", "ATTACK", 1, 6, 9, afterBash, afterStrike);

        assertTrue(new SequencingRule().evaluate(combat(Arrays.asList(bash, strike))).isEmpty());
    }

    @Test
    public void staysSilentForTrivialDifference() {
        TurnSnapshot before = state(1, 0, false, 46, 3);
        TurnSnapshot afterStrike = state(1, 0, false, 45, 2);
        TurnSnapshot afterBash = state(1, 0, true, 37, 0);

        ActionSnapshot strike = action(0, "Strike_R", "Strike", "ATTACK", 1, 1, 1, before, afterStrike);
        ActionSnapshot bash = action(1, "Bash", "Bash", "ATTACK", 2, 8, 8, afterStrike, afterBash);

        assertTrue(new SequencingRule().evaluate(combat(Arrays.asList(strike, bash))).isEmpty());
    }

    private static CombatSnapshot combat(List<ActionSnapshot> actions) {
        TurnSnapshot state = actions.get(0).getBefore();
        return new CombatSnapshot(1, 1, "IRONCLAD", 0, state,
                actions.get(actions.size() - 1).getAfter(),
                Collections.singletonList(state), actions, true);
    }

    private static ActionSnapshot action(int ordinal, String id, String name, String type,
                                         int cost, int rawDamage, int actualDamage,
                                         TurnSnapshot before, TurnSnapshot after) {
        return new ActionSnapshot(ordinal, 1, id, name, type, cost,
                before.getEnergy(), before.getEnergy() - cost,
                "jaw-worm", "Jaw Worm", rawDamage, actualDamage, before, after);
    }

    private static TurnSnapshot state(int turn, int vulnerable, boolean targetVulnerable,
                                      int enemyHp, int energy) {
        java.util.Map<String, Integer> powers = targetVulnerable
                ? Collections.singletonMap("Vulnerable", vulnerable == 0 ? 2 : vulnerable)
                : Collections.<String, Integer>emptyMap();
        EnemySnapshot enemy = new EnemySnapshot("jaw-worm", "Jaw Worm", enemyHp, 46, 0,
                "ATTACK", 5, 1, powers);
        CardSnapshot strike = new CardSnapshot("Strike_R", "Strike", "ATTACK", 1, 6, 0, true);
        CardSnapshot bash = new CardSnapshot("Bash", "Bash", "ATTACK", 2, 8, 0, true);
        return new TurnSnapshot(1, 1, "IRONCLAD", 0, turn, 75, 75, 0, energy,
                Arrays.asList(strike, bash), Collections.<CardSnapshot>emptyList(),
                Collections.<CardSnapshot>emptyList(), Collections.<CardSnapshot>emptyList(),
                Collections.singletonList(enemy), Collections.<String, Integer>emptyMap(),
                Collections.<String>emptyList(), Collections.<String>emptyList());
    }
}
