package com.warriorssmp.jobs.fishing.task;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Fishing has no natural "tier" gating the way mining/woodcutting/farming
 * tiers do (vanilla loot doesn't care about player level) — instead, Tier 3+
 * contracts only count a catch if the water conditions match, using
 * server-detectable signals only (weather, time, Y-level, biome, rod
 * enchants). No custom items or client-side content involved anywhere here.
 */
public final class TierConditions {

    private TierConditions() {}

    public static boolean meetsConditions(Player player, int tier) {
        return switch (tier) {
            case 1, 2 -> true;
            case 3 -> player.getWorld().hasStorm() || isDeepOcean(player);
            case 4 -> isOcean(player);
            case 5 -> isNight(player) || player.getLocation().getY() < 0;
            case 6 -> player.getWorld().isThundering() || (isSubmerged(player) && isNight(player));
            case 7 -> player.getWorld().isThundering() && isNight(player) && isDeepOcean(player) && hasLuckOfTheSeaIII(player);
            default -> true;
        };
    }

    /** A short human-readable hint for what's missing, shown when a catch doesn't
     *  count toward a gated contract — so it reads as "almost" rather than silent. */
    public static String hintFor(int tier) {
        return switch (tier) {
            case 3 -> "§7Needs: rain, or a deep ocean biome";
            case 4 -> "§7Needs: an ocean biome variant";
            case 5 -> "§7Needs: night, or below Y=0";
            case 6 -> "§7Needs: a thunderstorm, or submerged at night";
            case 7 -> "§7Needs: thunderstorm + night + deep ocean + Luck of the Sea III";
            default -> "";
        };
    }

    private static boolean isNight(Player player) {
        long time = player.getWorld().getTime();
        return time >= 13000 && time <= 23000;
    }

    private static boolean isDeepOcean(Player player) {
        String biome = player.getLocation().getBlock().getBiome().name();
        return biome.contains("DEEP") && biome.contains("OCEAN");
    }

    private static boolean isOcean(Player player) {
        return player.getLocation().getBlock().getBiome().name().contains("OCEAN");
    }

    private static boolean isSubmerged(Player player) {
        return player.getEyeLocation().getBlock().isLiquid();
    }

    private static boolean hasLuckOfTheSeaIII(Player player) {
        ItemStack rod = player.getInventory().getItemInMainHand();
        if (rod.getType() != Material.FISHING_ROD) {
            rod = player.getInventory().getItemInOffHand();
        }
        if (rod.getType() != Material.FISHING_ROD) return false;
        return rod.getEnchantmentLevel(Enchantment.LUCK_OF_THE_SEA) >= 3;
    }
}
