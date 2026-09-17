package spirecoach.game;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.screens.CardRewardScreen;
import spirecoach.logging.RunLogger;
import spirecoach.ui.CombatReviewOverlay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Tracks normal post-combat card rewards and gives feedback after the choice. */
public final class CardRewardCoach {
    private static final int FRAMES_TO_WAIT_FOR_CARD_EFFECT = 20;

    private final CombatReviewOverlay review;
    private final List<RewardCard> offeredCards = new ArrayList<RewardCard>();
    private Map<String, Integer> deckCountsAtOpen = Collections.emptyMap();
    private DeckShape deckShapeAtOpen = DeckShape.empty();
    private RunLogger runLogger;
    private int deckSizeAtOpen;
    private boolean watching;
    private int framesSinceClosed;
    private int rewardsReviewed;
    private int rewardsSkipped;
    private int cardsAdded;

    public CardRewardCoach(CombatReviewOverlay review) {
        this.review = review;
    }

    public void setRunLogger(RunLogger runLogger) {
        this.runLogger = runLogger;
    }

    public void update() {
        CardRewardScreen screen = AbstractDungeon.cardRewardScreen;
        if (screen == null) return;

        boolean rewardOpen = AbstractDungeon.screen == AbstractDungeon.CurrentScreen.CARD_REWARD;
        if (rewardOpen) {
            if (!watching && screen.rItem != null && screen.rewardGroup != null
                    && !screen.rewardGroup.isEmpty()) {
                begin(screen.rewardGroup);
            }
            return;
        }

        if (!watching) return;
        framesSinceClosed++;
        RewardCard selected = findAddedCard();
        if (selected != null || framesSinceClosed >= FRAMES_TO_WAIT_FOR_CARD_EFFECT) {
            finish(selected);
        }
    }

    public void resetRun() {
        watching = false;
        offeredCards.clear();
        deckCountsAtOpen = Collections.emptyMap();
        deckShapeAtOpen = DeckShape.empty();
        deckSizeAtOpen = 0;
        framesSinceClosed = 0;
        rewardsReviewed = 0;
        rewardsSkipped = 0;
        cardsAdded = 0;
    }

    public int getRewardsReviewed() { return rewardsReviewed; }
    public int getRewardsSkipped() { return rewardsSkipped; }
    public int getCardsAdded() { return cardsAdded; }

    private void begin(List<AbstractCard> cards) {
        offeredCards.clear();
        for (AbstractCard card : cards) {
            if (card != null) offeredCards.add(new RewardCard(card));
        }
        if (offeredCards.isEmpty()) return;
        deckCountsAtOpen = deckCounts();
        deckShapeAtOpen = deckShape();
        deckSizeAtOpen = currentDeckSize();
        framesSinceClosed = 0;
        watching = true;
    }

    private void finish(RewardCard selected) {
        watching = false;
        rewardsReviewed++;
        if (selected == null) {
            rewardsSkipped++;
        } else {
            cardsAdded++;
        }

        int deckSize = currentDeckSize();
        String advice = guide(selected, deckShapeAtOpen);
        if (runLogger != null) {
            List<String> offeredIds = new ArrayList<String>();
            for (RewardCard offered : offeredCards) offeredIds.add(offered.id);
            String character = "";
            if (AbstractDungeon.player != null && AbstractDungeon.player.chosenClass != null) {
                character = AbstractDungeon.player.chosenClass.name();
            }
            runLogger.cardReward(AbstractDungeon.floorNum, AbstractDungeon.actNum, character,
                    offeredIds,
                    selected == null ? null : selected.id,
                    selected == null ? null : selected.name,
                    selected == null,
                    deckSizeAtOpen, deckSize,
                    deckShapeAtOpen.attacks, deckShapeAtOpen.skills, deckShapeAtOpen.powers,
                    advice);
        }
        StringBuilder message = new StringBuilder();
        if (selected == null) {
            message.append("You skipped this card reward. Your deck stayed at ")
                    .append(deckSize)
                    .append(" cards. Skipping is valid when none of the offers solves a clear problem.");
        } else {
            message.append("You added ")
                    .append(selected.name)
                    .append(" (")
                    .append(role(selected.type))
                    .append("). Your deck is now ")
                    .append(deckSize)
                    .append(" cards.");
        }
        message.append(" Before the choice it had ")
                .append(deckShapeAtOpen.attacks)
                .append(" attacks, ")
                .append(deckShapeAtOpen.skills)
                .append(" skills, and ")
                .append(deckShapeAtOpen.powers)
                .append(" powers.");
        message.append(" Coach's read: ")
                .append(advice);
        message.append(" Ask yourself: does that match the problem you are trying to solve?");

        review.openNotice("CARD REWARD", message.toString());
        offeredCards.clear();
        deckCountsAtOpen = Collections.emptyMap();
        deckShapeAtOpen = DeckShape.empty();
        deckSizeAtOpen = 0;
    }

    private RewardCard findAddedCard() {
        Map<String, Integer> now = deckCounts();
        for (RewardCard offered : offeredCards) {
            int before = count(deckCountsAtOpen, offered.id);
            int after = count(now, offered.id);
            if (after > before) return offered;
        }
        return null;
    }

    private Map<String, Integer> deckCounts() {
        if (AbstractDungeon.player == null || AbstractDungeon.player.masterDeck == null
                || AbstractDungeon.player.masterDeck.group == null) {
            return Collections.emptyMap();
        }
        Map<String, Integer> result = new LinkedHashMap<String, Integer>();
        for (AbstractCard card : AbstractDungeon.player.masterDeck.group) {
            if (card == null) continue;
            Integer old = result.get(card.cardID);
            result.put(card.cardID, old == null ? 1 : old + 1);
        }
        return result;
    }

    private int currentDeckSize() {
        if (AbstractDungeon.player == null || AbstractDungeon.player.masterDeck == null
                || AbstractDungeon.player.masterDeck.group == null) return 0;
        return AbstractDungeon.player.masterDeck.group.size();
    }

    private DeckShape deckShape() {
        if (AbstractDungeon.player == null || AbstractDungeon.player.masterDeck == null
                || AbstractDungeon.player.masterDeck.group == null) return DeckShape.empty();
        int attacks = 0;
        int skills = 0;
        int powers = 0;
        int nonStarterAttacks = 0;
        int nonStarterSkills = 0;
        boolean hasStrengthScaling = false;
        boolean hasExhaustPayoff = false;
        for (AbstractCard card : AbstractDungeon.player.masterDeck.group) {
            if (card == null || card.type == null) continue;
            if (card.type == AbstractCard.CardType.ATTACK) attacks++;
            if (card.type == AbstractCard.CardType.SKILL) skills++;
            if (card.type == AbstractCard.CardType.POWER) powers++;
            if (card.type == AbstractCard.CardType.ATTACK && !isStarter(card.cardID)) {
                nonStarterAttacks++;
            }
            if (card.type == AbstractCard.CardType.SKILL && !isStarter(card.cardID)) {
                nonStarterSkills++;
            }
            if (isStrengthScaling(card.cardID)) hasStrengthScaling = true;
            if (isExhaustPayoff(card.cardID)) hasExhaustPayoff = true;
        }
        return new DeckShape(attacks, skills, powers, nonStarterAttacks, nonStarterSkills,
                hasStrengthScaling, hasExhaustPayoff);
    }

    private static boolean isStarter(String id) {
        return "Strike_R".equals(id) || "Defend_R".equals(id);
    }

    private static boolean isStrengthScaling(String id) {
        return "Inflame".equals(id) || "Spot Weakness".equals(id)
                || "Demon Form".equals(id) || "Limit Break".equals(id)
                || "Rupture".equals(id);
    }

    private static boolean isExhaustPayoff(String id) {
        return "Feel No Pain".equals(id) || "Dark Embrace".equals(id)
                || "Corruption".equals(id) || "Second Wind".equals(id);
    }

    private static int count(Map<String, Integer> counts, String id) {
        Integer value = counts.get(id);
        return value == null ? 0 : value;
    }

    private static String role(String type) {
        return type == null || type.length() == 0 ? "card" : type.toLowerCase();
    }

    private static String guide(RewardCard selected, DeckShape deck) {
        if (selected == null) {
            return "Skipping is reasonable when none of the offers fixes a clear weakness. You keep a smaller, more consistent deck, but you also give up any immediate answer the cards offered.";
        }
        if ("Anger".equals(selected.id)) {
            if (deck.hasStrengthScaling || deck.hasExhaustPayoff) {
                return "Anger is free front-loaded damage, and your deck already shows a synergy that can make repeated cheap attacks useful. The tradeoff is that every play adds another Anger to the discard pile, so it can still bloat the deck.";
            }
            return "Anger is free front-loaded damage, but every play adds another Anger to the discard pile. Take it for an immediate damage need; without strength, exhaust, or other 0-cost synergies, it can make later draws worse.";
        }
        if ("ATTACK".equals(selected.type)) {
            if (selected.cost == 0) {
                return "This is cheap immediate damage, useful when you need to spend less energy on attacks. Check that it has enough impact to justify adding another card.";
            }
            if (deck.nonStarterAttacks >= 5) {
                return "Your deck already has several non-starter attacks. Add this only if it improves a specific matchup, damage plan, or scaling package.";
            }
            return "This adds damage, which is usually valuable early. The next question is whether it is better than the attacks you already draw.";
        }
        if ("SKILL".equals(selected.type)) {
            if (selected.block > 0 && deck.nonStarterSkills < 4) {
                return "This adds a defensive tool to a deck with relatively few non-starter skills. That can help you survive while your damage develops.";
            }
            return "This is a utility or defensive card. Take it when its effect answers a real problem; otherwise it may make your attack turns less consistent.";
        }
        if ("POWER".equals(selected.type)) {
            return "Powers are long-term investments: they can improve later turns, but they cost a draw and often do nothing immediately. Take one when the fight is long enough for its effect to matter.";
        }
        return "There is no universal best pick here. Compare this card's specific effect with the next threat and the plan your current deck is already building toward.";
    }

    private static final class RewardCard {
        private final String id;
        private final String name;
        private final String type;
        private final int cost;
        private final int damage;
        private final int block;

        private RewardCard(AbstractCard card) {
            id = card.cardID == null ? "" : card.cardID;
            name = card.name == null ? id : card.name;
            type = card.type == null ? "card" : card.type.name();
            cost = card.costForTurn;
            damage = Math.max(0, card.damage);
            block = Math.max(0, card.block);
        }
    }

    private static final class DeckShape {
        private final int attacks;
        private final int skills;
        private final int powers;
        private final int nonStarterAttacks;
        private final int nonStarterSkills;
        private final boolean hasStrengthScaling;
        private final boolean hasExhaustPayoff;

        private DeckShape(int attacks, int skills, int powers, int nonStarterAttacks,
                          int nonStarterSkills, boolean hasStrengthScaling,
                          boolean hasExhaustPayoff) {
            this.attacks = attacks;
            this.skills = skills;
            this.powers = powers;
            this.nonStarterAttacks = nonStarterAttacks;
            this.nonStarterSkills = nonStarterSkills;
            this.hasStrengthScaling = hasStrengthScaling;
            this.hasExhaustPayoff = hasExhaustPayoff;
        }

        private static DeckShape empty() {
            return new DeckShape(0, 0, 0, 0, 0, false, false);
        }
    }
}
