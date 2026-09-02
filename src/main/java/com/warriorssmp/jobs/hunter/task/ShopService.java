package com.warriorssmp.jobs.hunter.task;

import com.warriorssmp.jobs.hunter.data.DataStore;
import com.warriorssmp.jobs.hunter.data.PlayerGatherData;
import com.warriorssmp.jobs.common.EconomyService;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class ShopService {

    private final GatherConfig config;
    private final DataStore dataStore;
    private final EconomyService economy;
    private final PremiumService premium;

    public ShopService(GatherConfig config, DataStore dataStore, EconomyService economy, PremiumService premium) {
        this.config = config;
        this.dataStore = dataStore;
        this.economy = economy;
        this.premium = premium;
    }

    public enum Result {SUCCESS, NO_PERMISSION, ALREADY_OWNED, INSUFFICIENT_FUNDS, UNKNOWN_ITEM}

    public Result purchase(Player player, PlayerGatherData data, String itemId) {
        GatherConfig.ShopItem item = config.shopItem(itemId);
        if (item == null) return Result.UNKNOWN_ITEM;

        if (item.premium() && !premium.isPremium(player)) {
            return Result.NO_PERMISSION;
        }

        int key = itemId.hashCode();
        if (item.limit() > 0 && data.purchasedOneTimeItems.contains(key)) {
            return Result.ALREADY_OWNED;
        }

        long cost = Math.round(item.cost());
        if (data.points < cost) {
            return Result.INSUFFICIENT_FUNDS;
        }
        data.points -= cost;

        applyEffect(player, data, itemId);

        if (item.limit() > 0) {
            data.purchasedOneTimeItems.add(key);
        }

        return Result.SUCCESS;
    }

    private void applyEffect(Player player, PlayerGatherData data, String itemId) {
        long now = System.currentTimeMillis();
        switch (itemId) {
            case "task_skip" -> {} // handled by TaskService.skipTask directly, not routed through here
            case "xp_boost" -> data.xpBoostExpiry = Math.max(data.xpBoostExpiry, now) + 3_600_000L;
            case "point_boost" -> data.pointBoostExpiry = Math.max(data.pointBoostExpiry, now) + 3_600_000L;
            case "better_tasks" -> data.betterTasksExpiry = Math.max(data.betterTasksExpiry, now) + 3_600_000L;
            case "master_teleport" -> data.masterTeleportUnlocked = true;
            case "strength_potion" -> givePotion(player, PotionEffectType.STRENGTH, 3 * 60 * 20, 0);
            case "strength_ii_potion" -> givePotion(player, PotionEffectType.STRENGTH, 90 * 20, 1);
            case "speed_potion" -> givePotion(player, PotionEffectType.SPEED, 3 * 60 * 20, 0);
            case "fire_resistance_potion" -> givePotion(player, PotionEffectType.FIRE_RESISTANCE, 3 * 60 * 20, 0);
            case "night_vision_potion" -> givePotion(player, PotionEffectType.NIGHT_VISION, 3 * 60 * 20, 0);
            case "regeneration_potion" -> givePotion(player, PotionEffectType.REGENERATION, 45 * 20, 0);
            case "protection_iv_book" -> giveBook(player, Enchantment.PROTECTION, 4);
            case "sharpness_v_book" -> giveBook(player, Enchantment.SHARPNESS, 5);
            case "power_v_book" -> giveBook(player, Enchantment.POWER, 5);
            case "efficiency_v_book" -> giveBook(player, Enchantment.EFFICIENCY, 5);
            case "unbreaking_iii_book" -> giveBook(player, Enchantment.UNBREAKING, 3);
            case "feather_falling_iv_book" -> giveBook(player, Enchantment.FEATHER_FALLING, 4);
            case "fortune_iii_book" -> giveBook(player, Enchantment.FORTUNE, 3);
            case "looting_iii_book" -> giveBook(player, Enchantment.LOOTING, 3);
            case "mending_book" -> giveBook(player, Enchantment.MENDING, 1);
            default -> {
                // Anything else (fallback) — give one of the configured item.
                GatherConfig.ShopItem item = config.shopItem(itemId);
                if (item != null) {
                    player.getInventory().addItem(new ItemStack(
                            com.warriorssmp.jobs.hunter.model.IconUtil.safeIcon(item.material())));
                }
            }
        }
    }

    private void givePotion(Player player, PotionEffectType type, int durationTicks, int amplifier) {
        ItemStack potion = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        meta.addCustomEffect(new PotionEffect(type, durationTicks, amplifier), true);
        potion.setItemMeta(meta);
        player.getInventory().addItem(potion);
    }

    private void giveBook(Player player, Enchantment enchantment, int level) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
        meta.addStoredEnchant(enchantment, level, true);
        book.setItemMeta(meta);
        player.getInventory().addItem(book);
    }
}
