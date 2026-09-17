package spirecoach.game;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;
import spirecoach.domain.CardSnapshot;
import spirecoach.domain.EnemySnapshot;
import spirecoach.domain.TurnSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Translates the live game objects into the small domain model used by rules and logs. */
public final class GameStateCapture {
    private GameStateCapture() { }

    public static TurnSnapshot capture() {
        AbstractPlayer player = AbstractDungeon.player;
        if (player == null) return null;

        return new TurnSnapshot(
                AbstractDungeon.floorNum,
                AbstractDungeon.actNum,
                player.chosenClass == null ? "" : player.chosenClass.name(),
                AbstractDungeon.ascensionLevel,
                AbstractDungeon.actionManager == null ? 0 : AbstractDungeon.actionManager.turn,
                player.currentHealth,
                player.maxHealth,
                player.currentBlock,
                player.energy == null ? 0 : player.energy.energy,
                cards(player.hand),
                cards(player.drawPile),
                cards(player.discardPile),
                cards(player.exhaustPile),
                enemies(),
                powers(player),
                relics(player),
                potions(player)
        );
    }

    private static List<CardSnapshot> cards(CardGroup group) {
        if (group == null || group.group == null) return Collections.emptyList();
        List<CardSnapshot> result = new ArrayList<CardSnapshot>();
        for (AbstractCard card : group.group) {
            result.add(card(card));
        }
        return result;
    }

    public static CardSnapshot card(AbstractCard card) {
        if (card == null) return new CardSnapshot("", "", "", -1, 0, 0, false);
        return new CardSnapshot(
                card.cardID,
                card.name,
                card.type == null ? "" : card.type.name(),
                card.costForTurn,
                Math.max(0, card.damage),
                Math.max(0, card.block),
                card.target != AbstractCard.CardTarget.NONE
        );
    }

    private static List<EnemySnapshot> enemies() {
        if (AbstractDungeon.getMonsters() == null || AbstractDungeon.getMonsters().monsters == null) {
            return Collections.emptyList();
        }
        List<EnemySnapshot> result = new ArrayList<EnemySnapshot>();
        for (AbstractMonster monster : AbstractDungeon.getMonsters().monsters) {
            if (monster == null) continue;
            result.add(enemy(monster));
        }
        return result;
    }

    private static EnemySnapshot enemy(AbstractMonster monster) {
        Map<String, Integer> powers = new LinkedHashMap<String, Integer>();
        if (monster.powers != null) {
            for (AbstractPower power : monster.powers) {
                if (power != null) powers.put(power.ID, power.amount);
            }
        }
        return new EnemySnapshot(
                monster.id,
                monster.name,
                monster.currentHealth,
                monster.maxHealth,
                monster.currentBlock,
                monster.intent == null ? "" : monster.intent.name(),
                monster.getIntentDmg(),
                1,
                powers
        );
    }

    private static Map<String, Integer> powers(AbstractCreature creature) {
        Map<String, Integer> result = new LinkedHashMap<String, Integer>();
        if (creature.powers != null) {
            for (AbstractPower power : creature.powers) {
                if (power != null) result.put(power.ID, power.amount);
            }
        }
        return result;
    }

    private static List<String> relics(AbstractPlayer player) {
        List<String> result = new ArrayList<String>();
        if (player.relics != null) {
            for (com.megacrit.cardcrawl.relics.AbstractRelic relic : player.relics) {
                if (relic != null) result.add(relic.relicId);
            }
        }
        return result;
    }

    private static List<String> potions(AbstractPlayer player) {
        List<String> result = new ArrayList<String>();
        if (player.potions != null) {
            for (com.megacrit.cardcrawl.potions.AbstractPotion potion : player.potions) {
                if (potion != null) result.add(potion.ID);
            }
        }
        return result;
    }

    public static TargetResolution resolveTarget(TurnSnapshot before, TurnSnapshot after) {
        if (before == null || after == null) return TargetResolution.none();
        EnemySnapshot best = null;
        int bestScore = 0;
        boolean tie = false;
        for (EnemySnapshot beforeEnemy : before.getEnemies()) {
            EnemySnapshot afterEnemy = after.findEnemy(beforeEnemy.getId());
            if (afterEnemy == null) continue;
            int hpLoss = Math.max(0, beforeEnemy.getCurrentHp() - afterEnemy.getCurrentHp());
            int blockLoss = Math.max(0, beforeEnemy.getBlock() - afterEnemy.getBlock());
            int vulnerableGain = Math.max(0, afterEnemy.getPowerAmount("Vulnerable")
                    - beforeEnemy.getPowerAmount("Vulnerable"));
            int score = hpLoss + blockLoss + (vulnerableGain * 100);
            if (score > bestScore) {
                best = afterEnemy;
                bestScore = score;
                tie = false;
            } else if (score > 0 && score == bestScore) {
                tie = true;
            }
        }
        if (best == null || tie) return TargetResolution.none();
        EnemySnapshot beforeEnemy = before.findEnemy(best.getId());
        int hpLoss = Math.max(0, beforeEnemy.getCurrentHp() - best.getCurrentHp());
        int blockLoss = Math.max(0, beforeEnemy.getBlock() - best.getBlock());
        return new TargetResolution(best.getId(), best.getName(), hpLoss + blockLoss);
    }

    public static final class TargetResolution {
        private final String id;
        private final String name;
        private final int damage;

        private TargetResolution(String id, String name, int damage) {
            this.id = id;
            this.name = name;
            this.damage = damage;
        }

        public static TargetResolution none() { return new TargetResolution("", "", 0); }
        public String getId() { return id; }
        public String getName() { return name; }
        public int getDamage() { return damage; }
    }
}
