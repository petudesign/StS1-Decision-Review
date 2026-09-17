package spirecoach.domain;

import java.util.Objects;

/** A small, game-independent representation of a card at the moment it was observed. */
public final class CardSnapshot {
    private final String id;
    private final String name;
    private final String type;
    private final int cost;
    private final int damage;
    private final int block;
    private final boolean targeted;

    public CardSnapshot(String id, String name, String type, int cost, int damage, int block, boolean targeted) {
        this.id = valueOrEmpty(id);
        this.name = valueOrEmpty(name);
        this.type = valueOrEmpty(type);
        this.cost = cost;
        this.damage = damage;
        this.block = block;
        this.targeted = targeted;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public int getCost() { return cost; }
    public int getDamage() { return damage; }
    public int getBlock() { return block; }
    public boolean isTargeted() { return targeted; }

    public boolean isAttack() { return "ATTACK".equals(type); }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof CardSnapshot)) return false;
        CardSnapshot that = (CardSnapshot) other;
        return cost == that.cost && damage == that.damage && block == that.block
                && targeted == that.targeted && id.equals(that.id) && name.equals(that.name)
                && type.equals(that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, type, cost, damage, block, targeted);
    }
}
