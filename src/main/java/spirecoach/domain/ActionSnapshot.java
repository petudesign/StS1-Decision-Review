package spirecoach.domain;

/** One player card play, including the state observed before and after it resolved. */
public final class ActionSnapshot {
    private final int ordinal;
    private final int turnNumber;
    private final String cardId;
    private final String cardName;
    private final String cardType;
    private final int cost;
    private final int energyBefore;
    private final int energyAfter;
    private final String targetId;
    private final String targetName;
    private final int rawDamage;
    private final int damageDealt;
    private final TurnSnapshot before;
    private final TurnSnapshot after;

    public ActionSnapshot(int ordinal, int turnNumber, String cardId, String cardName,
                          String cardType, int cost, int energyBefore, int energyAfter,
                          String targetId, String targetName, int rawDamage, int damageDealt,
                          TurnSnapshot before, TurnSnapshot after) {
        this.ordinal = ordinal;
        this.turnNumber = turnNumber;
        this.cardId = cardId == null ? "" : cardId;
        this.cardName = cardName == null ? "" : cardName;
        this.cardType = cardType == null ? "" : cardType;
        this.cost = cost;
        this.energyBefore = energyBefore;
        this.energyAfter = energyAfter;
        this.targetId = targetId == null ? "" : targetId;
        this.targetName = targetName == null ? "" : targetName;
        this.rawDamage = rawDamage;
        this.damageDealt = damageDealt;
        this.before = before;
        this.after = after;
    }

    public int getOrdinal() { return ordinal; }
    public int getTurnNumber() { return turnNumber; }
    public String getCardId() { return cardId; }
    public String getCardName() { return cardName; }
    public String getCardType() { return cardType; }
    public int getCost() { return cost; }
    public int getEnergyBefore() { return energyBefore; }
    public int getEnergyAfter() { return energyAfter; }
    public String getTargetId() { return targetId; }
    public String getTargetName() { return targetName; }
    public int getRawDamage() { return rawDamage; }
    public int getDamageDealt() { return damageDealt; }
    public TurnSnapshot getBefore() { return before; }
    public TurnSnapshot getAfter() { return after; }

    public boolean isAttack() { return "ATTACK".equals(cardType); }
    public boolean isBash() { return "Bash".equals(cardId) || "Bash".equalsIgnoreCase(cardName); }
}
