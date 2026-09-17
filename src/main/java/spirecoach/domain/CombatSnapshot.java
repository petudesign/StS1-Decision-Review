package spirecoach.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The complete, bounded record for one combat. */
public final class CombatSnapshot {
    private final int floor;
    private final int act;
    private final String character;
    private final int ascension;
    private final TurnSnapshot start;
    private final TurnSnapshot end;
    private final List<TurnSnapshot> turnStarts;
    private final List<ActionSnapshot> actions;
    private final boolean won;

    public CombatSnapshot(int floor, int act, String character, int ascension,
                          TurnSnapshot start, TurnSnapshot end,
                          List<TurnSnapshot> turnStarts, List<ActionSnapshot> actions,
                          boolean won) {
        this.floor = floor;
        this.act = act;
        this.character = character == null ? "" : character;
        this.ascension = ascension;
        this.start = start;
        this.end = end;
        this.turnStarts = immutableCopy(turnStarts);
        this.actions = immutableCopy(actions);
        this.won = won;
    }

    public int getFloor() { return floor; }
    public int getAct() { return act; }
    public String getCharacter() { return character; }
    public int getAscension() { return ascension; }
    public TurnSnapshot getStart() { return start; }
    public TurnSnapshot getEnd() { return end; }
    public List<TurnSnapshot> getTurnStarts() { return turnStarts; }
    public List<ActionSnapshot> getActions() { return actions; }
    public boolean isWon() { return won; }

    private static <T> List<T> immutableCopy(List<T> values) {
        return values == null
                ? Collections.<T>emptyList()
                : Collections.unmodifiableList(new ArrayList<T>(values));
    }
}
