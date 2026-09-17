package spirecoach.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Enemy state needed to explain a combat decision without exposing game internals to rules. */
public final class EnemySnapshot {
    private final String id;
    private final String name;
    private final int currentHp;
    private final int maxHp;
    private final int block;
    private final String intent;
    private final int intentDamage;
    private final int intentHits;
    private final Map<String, Integer> powers;

    public EnemySnapshot(String id, String name, int currentHp, int maxHp, int block,
                         String intent, int intentDamage, int intentHits,
                         Map<String, Integer> powers) {
        this.id = id == null ? "" : id;
        this.name = name == null ? "" : name;
        this.currentHp = currentHp;
        this.maxHp = maxHp;
        this.block = block;
        this.intent = intent == null ? "" : intent;
        this.intentDamage = intentDamage;
        this.intentHits = intentHits;
        this.powers = powers == null
                ? Collections.<String, Integer>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<String, Integer>(powers));
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getCurrentHp() { return currentHp; }
    public int getMaxHp() { return maxHp; }
    public int getBlock() { return block; }
    public String getIntent() { return intent; }
    public int getIntentDamage() { return intentDamage; }
    public int getIntentHits() { return intentHits; }
    public Map<String, Integer> getPowers() { return powers; }

    public int getPowerAmount(String powerId) {
        Integer amount = powers.get(powerId);
        return amount == null ? 0 : amount;
    }

    public boolean isAlive() { return currentHp > 0; }
}
