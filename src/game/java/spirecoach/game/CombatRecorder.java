package spirecoach.game;

import com.megacrit.cardcrawl.cards.AbstractCard;
import spirecoach.domain.ActionSnapshot;
import spirecoach.domain.CardSnapshot;
import spirecoach.domain.CombatSnapshot;
import spirecoach.domain.TurnSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Owns the mutable capture session. A card play is finalized on the next player
 * input or at combat end, after the game's action queue has resolved it.
 */
public final class CombatRecorder {
    private TurnSnapshot start;
    private TurnSnapshot end;
    private final List<TurnSnapshot> turnStarts = new ArrayList<TurnSnapshot>();
    private final List<ActionSnapshot> actions = new ArrayList<ActionSnapshot>();
    private PendingAction pending;
    private int nextOrdinal;
    private boolean active;

    public void start(TurnSnapshot initialState) {
        start = initialState;
        end = null;
        turnStarts.clear();
        actions.clear();
        pending = null;
        nextOrdinal = 0;
        active = initialState != null;
    }

    public boolean isActive() { return active; }

    public void recordTurnStart(TurnSnapshot state) {
        if (active && state != null) turnStarts.add(state);
    }

    public void recordCardUse(AbstractCard card, TurnSnapshot before) {
        if (!active || card == null || before == null) return;
        finishPending(before);
        pending = new PendingAction(nextOrdinal++, GameStateCapture.card(card), before);
    }

    public CombatSnapshot finish(TurnSnapshot finalState, boolean won) {
        if (!active) return null;
        end = finalState == null ? start : finalState;
        finishPending(end);
        active = false;
        return new CombatSnapshot(
                start.getFloor(), start.getAct(), start.getCharacter(), start.getAscension(),
                start, end, turnStarts, actions, won
        );
    }

    public List<ActionSnapshot> getActions() {
        return Collections.unmodifiableList(actions);
    }

    private void finishPending(TurnSnapshot after) {
        if (pending == null || after == null) return;
        GameStateCapture.TargetResolution target = GameStateCapture.resolveTarget(pending.before, after);
        ActionSnapshot action = new ActionSnapshot(
                pending.ordinal,
                pending.before.getTurnNumber(),
                pending.card.getId(),
                pending.card.getName(),
                pending.card.getType(),
                pending.card.getCost(),
                pending.before.getEnergy(),
                after.getEnergy(),
                target.getId(),
                target.getName(),
                pending.card.getDamage(),
                target.getDamage(),
                pending.before,
                after
        );
        actions.add(action);
        pending = null;
    }

    private static final class PendingAction {
        private final int ordinal;
        private final CardSnapshot card;
        private final TurnSnapshot before;

        private PendingAction(int ordinal, CardSnapshot card, TurnSnapshot before) {
            this.ordinal = ordinal;
            this.card = card;
            this.before = before;
        }
    }
}
