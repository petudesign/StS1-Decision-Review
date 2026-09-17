package spirecoach.coaching;

import spirecoach.domain.ActionSnapshot;
import spirecoach.domain.CombatSnapshot;
import spirecoach.domain.Confidence;
import spirecoach.domain.EnemySnapshot;
import spirecoach.domain.Lesson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Conservative first rule: identify an attack played before Bash when the same
 * target was still alive and vulnerable had not yet been applied.
 *
 * This rule intentionally compares only the directly affected attack cards. Bash
 * itself is present in both lines, and attacks already played after Bash are not
 * counted as missed value.
 */
public final class SequencingRule implements CoachingRule {
    public static final String ID = "ironclad.card_order.vulnerable_before_attack";
    private static final int MIN_MEANINGFUL_GAIN = 3;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public List<Lesson> evaluate(CombatSnapshot combat) {
        if (combat == null || !"IRONCLAD".equalsIgnoreCase(combat.getCharacter())) {
            return Collections.emptyList();
        }

        List<ActionSnapshot> actions = combat.getActions();
        for (int bashIndex = 0; bashIndex < actions.size(); bashIndex++) {
            ActionSnapshot bash = actions.get(bashIndex);
            if (!bash.isBash()) continue;

            List<ActionSnapshot> missedAttacks = new ArrayList<ActionSnapshot>();
            for (int i = 0; i < bashIndex; i++) {
                ActionSnapshot attack = actions.get(i);
                if (!isReliableSingleHitAttack(attack)) continue;
                if (attack.getTurnNumber() != bash.getTurnNumber()) continue;
                if (!attack.getTargetId().equals(bash.getTargetId())) continue;
                if (attack.getRawDamage() <= 0) continue;

                EnemySnapshot targetBeforeAttack = attack.getBefore().findEnemy(attack.getTargetId());
                EnemySnapshot targetBeforeBash = bash.getBefore().findEnemy(bash.getTargetId());
                if (targetBeforeAttack == null || targetBeforeBash == null) continue;
                if (!targetBeforeAttack.isAlive() || !targetBeforeBash.isAlive()) continue;
                if (targetBeforeAttack.getPowerAmount("Vulnerable") > 0) continue;

                missedAttacks.add(attack);
            }

            if (missedAttacks.isEmpty()) continue;

            int actualDamage = 0;
            int alternativeDamage = 0;
            List<String> actualNames = new ArrayList<String>();
            List<String> alternativeNames = new ArrayList<String>();
            for (ActionSnapshot attack : missedAttacks) {
                actualDamage += Math.max(0, attack.getDamageDealt());
                alternativeDamage += vulnerableDamage(attack.getRawDamage());
                actualNames.add(attack.getCardName());
                alternativeNames.add(attack.getCardName());
            }
            actualNames.add(bash.getCardName());
            alternativeNames.add(0, bash.getCardName());

            int gain = alternativeDamage - actualDamage;
            if (gain < MIN_MEANINGFUL_GAIN) continue;

            Map<String, Integer> values = new LinkedHashMap<String, Integer>();
            values.put("actual_damage", actualDamage);
            values.put("alternative_damage", alternativeDamage);
            values.put("difference", gain);

            String attackWord = missedAttacks.size() == 1 ? "attack" : "attacks";
            String explanation = "You played " + joinNames(missedAttacks)
                    + " before applying Vulnerable. Applying Vulnerable first would have increased "
                    + "the damage of the following " + attackWord + ".";

            Lesson lesson = new Lesson(
                    ID,
                    "Card order mattered here",
                    explanation,
                    "Apply multipliers and enabling effects before the actions that benefit from them.",
                    Confidence.CERTAIN,
                    gain,
                    actualNames,
                    alternativeNames,
                    values
            );
            return Collections.singletonList(lesson);
        }

        return Collections.emptyList();
    }

    private static int vulnerableDamage(int rawDamage) {
        return (int) Math.floor(rawDamage * 1.5d);
    }

    private static boolean isReliableSingleHitAttack(ActionSnapshot attack) {
        // The first rule is intentionally narrow. Strike is a direct, single-hit
        // attack whose damage can be reconstructed without simulating card text.
        // More attack families can be added once their hit/effect metadata is captured.
        return attack.isAttack()
                && !attack.isBash()
                && "Strike_R".equals(attack.getCardId());
    }

    private static String joinNames(List<ActionSnapshot> attacks) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < attacks.size(); i++) {
            if (i > 0) result.append(" and ");
            result.append(attacks.get(i).getCardName());
        }
        return result.toString();
    }
}
