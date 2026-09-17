package spirecoach.logging;

import spirecoach.domain.ActionSnapshot;
import spirecoach.domain.CardSnapshot;
import spirecoach.domain.CombatSnapshot;
import spirecoach.domain.EnemySnapshot;
import spirecoach.domain.Lesson;
import spirecoach.domain.TurnSnapshot;

import java.util.List;
import java.util.Map;

/** Dependency-free JSON serialization for the local log format. */
final class JsonLog {
    private JsonLog() { }

    static String combatStart(TurnSnapshot state) {
        StringBuilder json = new StringBuilder("{\"event\":\"combat_start\",\"state\":");
        json.append(turn(state)).append('}');
        return json.toString();
    }

    static String turnStart(TurnSnapshot state) {
        StringBuilder json = new StringBuilder("{\"event\":\"turn_start\",\"turn\":");
        json.append(state == null ? 0 : state.getTurnNumber()).append(",\"state\":");
        json.append(turn(state)).append('}');
        return json.toString();
    }

    static String turnEnd(TurnSnapshot state) {
        StringBuilder json = new StringBuilder("{\"event\":\"turn_end\",\"turn\":");
        json.append(state == null ? 0 : state.getTurnNumber()).append(",\"state\":");
        json.append(turn(state)).append('}');
        return json.toString();
    }

    static String action(ActionSnapshot action) {
        StringBuilder json = new StringBuilder("{\"event\":\"card_play\"");
        field(json, "ordinal", action.getOrdinal());
        field(json, "turn", action.getTurnNumber());
        field(json, "cardId", action.getCardId());
        field(json, "card", action.getCardName());
        field(json, "cardType", action.getCardType());
        field(json, "cost", action.getCost());
        field(json, "energyBefore", action.getEnergyBefore());
        field(json, "energyAfter", action.getEnergyAfter());
        field(json, "targetId", action.getTargetId());
        field(json, "target", action.getTargetName());
        field(json, "rawDamage", action.getRawDamage());
        field(json, "damageDealt", action.getDamageDealt());
        json.append(",\"before\":").append(turn(action.getBefore()));
        json.append(",\"after\":").append(turn(action.getAfter()));
        return json.append('}').toString();
    }

    static String combatEnd(CombatSnapshot combat, List<Lesson> lessons) {
        StringBuilder json = new StringBuilder("{\"event\":\"combat_end\"");
        field(json, "floor", combat.getFloor());
        field(json, "act", combat.getAct());
        field(json, "character", combat.getCharacter());
        field(json, "won", combat.isWon());
        json.append(",\"lessons\":[");
        if (lessons != null) {
            for (int i = 0; i < lessons.size(); i++) {
                if (i > 0) json.append(',');
                json.append(lesson(lessons.get(i)));
            }
        }
        json.append("]}");
        return json.toString();
    }

    private static String turn(TurnSnapshot state) {
        if (state == null) return "null";
        StringBuilder json = new StringBuilder("{");
        field(json, "floor", state.getFloor());
        field(json, "act", state.getAct());
        field(json, "character", state.getCharacter());
        field(json, "ascension", state.getAscension());
        field(json, "turn", state.getTurnNumber());
        field(json, "playerHp", state.getPlayerHp());
        field(json, "maxHp", state.getMaxHp());
        field(json, "playerBlock", state.getPlayerBlock());
        field(json, "energy", state.getEnergy());
        json.append(",\"hand\":").append(cards(state.getHand()));
        json.append(",\"drawPile\":").append(cards(state.getDrawPile()));
        json.append(",\"discardPile\":").append(cards(state.getDiscardPile()));
        json.append(",\"exhaustPile\":").append(cards(state.getExhaustPile()));
        json.append(",\"enemies\":[");
        for (int i = 0; i < state.getEnemies().size(); i++) {
            if (i > 0) json.append(',');
            json.append(enemy(state.getEnemies().get(i)));
        }
        json.append("]");
        json.append(",\"powers\":").append(map(state.getPowers()));
        json.append(",\"relics\":").append(strings(state.getRelics()));
        json.append(",\"potions\":").append(strings(state.getPotions()));
        return json.append('}').toString();
    }

    private static String cards(List<CardSnapshot> cards) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < cards.size(); i++) {
            if (i > 0) json.append(',');
            CardSnapshot card = cards.get(i);
            json.append('{');
            field(json, "id", card.getId());
            field(json, "name", card.getName());
            field(json, "type", card.getType());
            field(json, "cost", card.getCost());
            field(json, "damage", card.getDamage());
            field(json, "block", card.getBlock());
            field(json, "targeted", card.isTargeted());
            json.append('}');
        }
        return json.append(']').toString();
    }

    private static String enemy(EnemySnapshot enemy) {
        StringBuilder json = new StringBuilder("{");
        field(json, "id", enemy.getId());
        field(json, "name", enemy.getName());
        field(json, "currentHp", enemy.getCurrentHp());
        field(json, "maxHp", enemy.getMaxHp());
        field(json, "block", enemy.getBlock());
        field(json, "intent", enemy.getIntent());
        field(json, "intentDamage", enemy.getIntentDamage());
        field(json, "intentHits", enemy.getIntentHits());
        json.append(",\"powers\":").append(map(enemy.getPowers()));
        return json.append('}').toString();
    }

    private static String lesson(Lesson lesson) {
        StringBuilder json = new StringBuilder("{");
        field(json, "ruleId", lesson.getRuleId());
        field(json, "title", lesson.getTitle());
        field(json, "explanation", lesson.getExplanation());
        field(json, "principle", lesson.getPrinciple());
        field(json, "confidence", lesson.getConfidence().name());
        field(json, "impact", lesson.getImpact());
        json.append(",\"relevantActions\":").append(strings(lesson.getRelevantActions()));
        json.append(",\"alternativeActions\":").append(strings(lesson.getAlternativeActions()));
        json.append(",\"values\":").append(map(lesson.getValues()));
        return json.append('}').toString();
    }

    private static String strings(List<String> values) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) json.append(',');
            json.append(quote(values.get(i)));
        }
        return json.append(']').toString();
    }

    private static String map(Map<String, Integer> values) {
        StringBuilder json = new StringBuilder("{");
        int index = 0;
        for (Map.Entry<String, Integer> entry : values.entrySet()) {
            if (index++ > 0) json.append(',');
            json.append(quote(entry.getKey())).append(':').append(entry.getValue());
        }
        return json.append('}').toString();
    }

    private static void field(StringBuilder json, String key, String value) {
        if (json.length() > 1 && json.charAt(json.length() - 1) != '{') json.append(',');
        json.append(quote(key)).append(':').append(quote(value));
    }

    private static void field(StringBuilder json, String key, int value) {
        if (json.length() > 1 && json.charAt(json.length() - 1) != '{') json.append(',');
        json.append(quote(key)).append(':').append(value);
    }

    private static void field(StringBuilder json, String key, boolean value) {
        if (json.length() > 1 && json.charAt(json.length() - 1) != '{') json.append(',');
        json.append(quote(key)).append(':').append(value);
    }

    private static String quote(String value) {
        if (value == null) return "null";
        StringBuilder escaped = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\': escaped.append("\\\\"); break;
                case '"': escaped.append("\\\""); break;
                case '\n': escaped.append("\\n"); break;
                case '\r': escaped.append("\\r"); break;
                case '\t': escaped.append("\\t"); break;
                default: escaped.append(c);
            }
        }
        return escaped.append('"').toString();
    }
}
