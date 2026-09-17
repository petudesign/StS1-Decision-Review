package spirecoach.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Immutable combat state. The integration layer is responsible for populating it. */
public final class TurnSnapshot {
    private final int floor;
    private final int act;
    private final String character;
    private final int ascension;
    private final int turnNumber;
    private final int playerHp;
    private final int maxHp;
    private final int playerBlock;
    private final int energy;
    private final List<CardSnapshot> hand;
    private final List<CardSnapshot> drawPile;
    private final List<CardSnapshot> discardPile;
    private final List<CardSnapshot> exhaustPile;
    private final List<EnemySnapshot> enemies;
    private final Map<String, Integer> powers;
    private final List<String> relics;
    private final List<String> potions;

    public TurnSnapshot(int floor, int act, String character, int ascension, int turnNumber,
                        int playerHp, int maxHp, int playerBlock, int energy,
                        List<CardSnapshot> hand, List<CardSnapshot> drawPile,
                        List<CardSnapshot> discardPile, List<CardSnapshot> exhaustPile,
                        List<EnemySnapshot> enemies, Map<String, Integer> powers,
                        List<String> relics, List<String> potions) {
        this.floor = floor;
        this.act = act;
        this.character = character == null ? "" : character;
        this.ascension = ascension;
        this.turnNumber = turnNumber;
        this.playerHp = playerHp;
        this.maxHp = maxHp;
        this.playerBlock = playerBlock;
        this.energy = energy;
        this.hand = immutableCopy(hand);
        this.drawPile = immutableCopy(drawPile);
        this.discardPile = immutableCopy(discardPile);
        this.exhaustPile = immutableCopy(exhaustPile);
        this.enemies = immutableCopy(enemies);
        this.powers = powers == null
                ? Collections.<String, Integer>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<String, Integer>(powers));
        this.relics = immutableCopy(relics);
        this.potions = immutableCopy(potions);
    }

    public int getFloor() { return floor; }
    public int getAct() { return act; }
    public String getCharacter() { return character; }
    public int getAscension() { return ascension; }
    public int getTurnNumber() { return turnNumber; }
    public int getPlayerHp() { return playerHp; }
    public int getMaxHp() { return maxHp; }
    public int getPlayerBlock() { return playerBlock; }
    public int getEnergy() { return energy; }
    public List<CardSnapshot> getHand() { return hand; }
    public List<CardSnapshot> getDrawPile() { return drawPile; }
    public List<CardSnapshot> getDiscardPile() { return discardPile; }
    public List<CardSnapshot> getExhaustPile() { return exhaustPile; }
    public List<EnemySnapshot> getEnemies() { return enemies; }
    public Map<String, Integer> getPowers() { return powers; }
    public List<String> getRelics() { return relics; }
    public List<String> getPotions() { return potions; }

    public EnemySnapshot findEnemy(String id) {
        for (EnemySnapshot enemy : enemies) {
            if (enemy.getId().equals(id)) return enemy;
        }
        return null;
    }

    private static <T> List<T> immutableCopy(List<T> values) {
        return values == null
                ? Collections.<T>emptyList()
                : Collections.unmodifiableList(new ArrayList<T>(values));
    }
}
