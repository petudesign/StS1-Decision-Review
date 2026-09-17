package spirecoach;

import basemod.BaseMod;
import basemod.interfaces.OnCardUseSubscriber;
import basemod.interfaces.OnStartBattleSubscriber;
import basemod.interfaces.PostEnergyRechargeSubscriber;
import basemod.interfaces.PostBattleSubscriber;
import basemod.interfaces.PostDeathSubscriber;
import basemod.interfaces.PostRenderSubscriber;
import basemod.interfaces.PostUpdateSubscriber;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.rooms.MonsterRoomBoss;
import spirecoach.coaching.CoachingEngine;
import spirecoach.coaching.CoachingRule;
import spirecoach.coaching.SequencingRule;
import spirecoach.domain.ActionSnapshot;
import spirecoach.domain.CombatSnapshot;
import spirecoach.domain.Lesson;
import spirecoach.domain.TurnSnapshot;
import spirecoach.game.CardRewardCoach;
import spirecoach.game.CombatRecorder;
import spirecoach.game.GameStateCapture;
import spirecoach.logging.NdjsonLogger;
import spirecoach.logging.RunLogger;
import spirecoach.ui.CombatReviewOverlay;

import java.util.Arrays;
import java.util.List;

/** Mod entry point. Game hooks stay here; coaching rules only see domain snapshots. */
@SpireInitializer
public final class SpireCoach implements OnStartBattleSubscriber, PostEnergyRechargeSubscriber,
        OnCardUseSubscriber, PostBattleSubscriber, PostDeathSubscriber,
        PostRenderSubscriber, PostUpdateSubscriber {
    private final CombatRecorder recorder = new CombatRecorder();
    private final CoachingEngine coaching = new CoachingEngine(
            Arrays.<CoachingRule>asList(new SequencingRule())
    );
    private final CombatReviewOverlay review = new CombatReviewOverlay();
    private final CardRewardCoach rewardCoach = new CardRewardCoach(review);
    private NdjsonLogger logger;
    private RunLogger runLogger;
    private int loggedActions;
    private boolean turnOpen;
    private int combatsAnalyzed;
    private int lessonsFound;
    private boolean runEnded;

    public static void initialize() {
        new SpireCoach();
    }

    private SpireCoach() {
        BaseMod.subscribe(this);
    }

    @Override
    public void receiveOnBattleStart(AbstractRoom room) {
        if (runEnded) {
            if (runLogger != null) runLogger.close();
            runLogger = null;
            rewardCoach.setRunLogger(null);
            combatsAnalyzed = 0;
            lessonsFound = 0;
            rewardCoach.resetRun();
            runEnded = false;
        }
        TurnSnapshot state = GameStateCapture.capture();
        if (state == null || !"IRONCLAD".equalsIgnoreCase(state.getCharacter())) return;
        recorder.start(state);
        loggedActions = 0;
        turnOpen = false;
        if (runLogger == null) {
            runLogger = RunLogger.open();
            runLogger.runStart(state);
            rewardCoach.setRunLogger(runLogger);
        }
        logger = NdjsonLogger.open();
        logger.combatStart(state);
        review.close();
    }

    @Override
    public void receivePostEnergyRecharge() {
        if (!recorder.isActive()) return;
        TurnSnapshot state = GameStateCapture.capture();
        if (state == null) return;
        if (turnOpen && logger != null) logger.turnEnd(state);
        turnOpen = true;
        recorder.recordTurnStart(state);
        if (logger != null) logger.turnStart(state);
    }

    @Override
    public void receiveCardUsed(AbstractCard card) {
        if (!recorder.isActive()) return;
        TurnSnapshot before = GameStateCapture.capture();
        recorder.recordCardUse(card, before);
        logNewActions();
    }

    @Override
    public void receivePostBattle(AbstractRoom battleRoom) {
        finishCombat(true);
        if (isRunEndingBoss(battleRoom)) {
            finishRun(true);
        }
    }

    @Override
    public void receivePostDeath() {
        finishCombat(false);
        finishRun(false);
    }

    @Override
    public void receivePostRender(SpriteBatch sb) {
        review.render(sb);
    }

    @Override
    public void receivePostUpdate() {
        review.update();
        rewardCoach.update();
    }

    private void finishCombat(boolean won) {
        if (!recorder.isActive()) return;
        TurnSnapshot state = GameStateCapture.capture();
        CombatSnapshot combat = recorder.finish(state, won);
        logNewActions();
        List<Lesson> lessons = coaching.evaluate(combat);
        if (runLogger != null) runLogger.combatEnd(combat, lessons);
        if (logger != null) {
            if (turnOpen && state != null) logger.turnEnd(state);
            turnOpen = false;
            logger.combatEnd(combat, lessons);
            logger.close();
            logger = null;
        }
        combatsAnalyzed++;
        lessonsFound += lessons.size();
        review.open(lessons, lessons.isEmpty()
                ? "Coach checked this fight but found no high-confidence lesson."
                : "Coach found a concrete lesson from this fight.");
    }

    private void finishRun(boolean won) {
        runEnded = true;
        TurnSnapshot state = GameStateCapture.capture();
        if (runLogger != null) {
            runLogger.runEnd(state == null ? 0 : state.getFloor(),
                    state == null ? 0 : state.getAct(),
                    state == null ? "" : state.getCharacter(), won,
                    combatsAnalyzed, lessonsFound,
                    rewardCoach.getRewardsReviewed(), rewardCoach.getRewardsSkipped(),
                    rewardCoach.getCardsAdded());
            runLogger.close();
            runLogger = null;
        }
        review.openRunSummary(combatsAnalyzed, lessonsFound,
                rewardCoach.getRewardsReviewed(), rewardCoach.getRewardsSkipped(),
                rewardCoach.getCardsAdded(), won);
    }

    private boolean isRunEndingBoss(AbstractRoom battleRoom) {
        return battleRoom instanceof MonsterRoomBoss && AbstractDungeon.actNum >= 3;
    }

    private void logNewActions() {
        if (logger == null) return;
        List<ActionSnapshot> actions = recorder.getActions();
        while (loggedActions < actions.size()) {
            logger.action(actions.get(loggedActions));
            loggedActions++;
        }
    }
}
