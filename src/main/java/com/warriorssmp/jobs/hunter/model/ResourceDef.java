package com.warriorssmp.jobs.hunter.model;

import org.bukkit.entity.EntityType;

/** Despite the name (kept for consistency with the other WSMP skill plugins'
 *  internals), this tracks a monster type, not a block/item material — Hunter
 *  progress comes from EntityDeathEvent, not BlockBreakEvent/CraftItemEvent. */
public final class ResourceDef {

    public enum GatherType { MINING, WOODCUTTING, FARMING, FISHING, COOKING, HUNTING, PROCESSED }

    private final EntityType material;
    private final int tier;
    private final GatherType type;
    private final int requiredLevel;
    private final int minAmount;
    private final int maxAmount;

    public ResourceDef(EntityType material, int tier, GatherType type, int requiredLevel, int minAmount, int maxAmount) {
        this.material = material;
        this.tier = tier;
        this.type = type;
        this.requiredLevel = requiredLevel;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
    }

    public EntityType material() {
        return material;
    }

    public int tier() {
        return tier;
    }

    public GatherType type() {
        return type;
    }

    public int requiredLevel() {
        return requiredLevel;
    }

    public int minAmount() {
        return minAmount;
    }

    public int maxAmount() {
        return maxAmount;
    }

    public int rollAmount(java.util.random.RandomGenerator rng) {
        if (maxAmount <= minAmount) return minAmount;
        return minAmount + rng.nextInt(maxAmount - minAmount + 1);
    }

    public String displayName() {
        String raw = material.name().toLowerCase().replace('_', ' ');
        StringBuilder sb = new StringBuilder();
        for (String word : raw.split(" ")) {
            if (word.isEmpty()) continue;
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }
}
