package com.warriorssmp.jobs.hunter.task;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * The design doc wants gear requirements to scale with task tier so players
 * can't cheese high-tier kill counts with trivial gear (or, just as much the
 * point, farm easy mobs for real progress while overgeared). Checks the
 * player's held weapon and worn chestplate against a minimum material rank
 * per tier. This is a simplified version of the doc's full per-slot,
 * per-enchant, per-potion requirement system (bow/crossbow/shield/potions/
 * specific enchantments) — just weapon + chestplate — but it's the same
 * anti-cheese mechanism doing real work.
 */
public final class EquipmentConditions {

    private EquipmentConditions() {}

    /** Minimum material rank required per tier. 0 = no requirement. */
    private static final int[] REQUIRED_RANK = {0, 0, 0, 1, 2, 3, 4, 4};

    public static boolean meetsRequirement(Player player, int tier) {
        int required = tier >= 0 && tier < REQUIRED_RANK.length ? REQUIRED_RANK[tier] : 0;
        if (required == 0) return true;

        int weaponRank = rankOf(player.getInventory().getItemInMainHand());
        int armorRank = rankOf(player.getInventory().getChestplate());
        return weaponRank >= required && armorRank >= required;
    }

    /** Human-readable minimum tier name, for the Item Requirements page and
     *  the "doesn't count yet" action bar hint. */
    public static String requirementLabel(int tier) {
        int required = tier >= 0 && tier < REQUIRED_RANK.length ? REQUIRED_RANK[tier] : 0;
        return switch (required) {
            case 0 -> "No requirement";
            case 1 -> "Stone/Leather+";
            case 2 -> "Iron+";
            case 3 -> "Diamond+";
            case 4 -> "Netherite";
            default -> "No requirement";
        };
    }

    private static int rankOf(ItemStack item) {
        if (item == null) return 0;
        String name = item.getType().name();
        if (name.contains("NETHERITE")) return 4;
        if (name.contains("DIAMOND")) return 3;
        if (name.contains("IRON")) return 2;
        if (name.contains("STONE") || name.contains("LEATHER") || name.contains("GOLDEN") || name.contains("GOLD")) return 1;
        return 0;
    }
}
