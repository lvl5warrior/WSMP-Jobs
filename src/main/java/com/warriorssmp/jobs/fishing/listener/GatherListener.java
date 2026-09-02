package com.warriorssmp.jobs.fishing.listener;

import com.warriorssmp.jobs.fishing.FishingPlugin;
import com.warriorssmp.jobs.fishing.data.PlayerGatherData;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/**
 * Fishing has no blocks to break — the whole loop runs off PlayerFishEvent
 * instead. There's no "cheesy afk farm" prevention needed either (unlike the
 * block-based skills), since you can't place-then-catch the way you can
 * place-then-break a block.
 */
public final class GatherListener implements Listener {

    private final FishingPlugin plugin;

    public GatherListener(FishingPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        if (!(event.getCaught() instanceof Item caughtItem)) return;

        Player player = event.getPlayer();
        ItemStack stack = caughtItem.getItemStack();
        Material material = stack.getType();
        int amount = stack.getAmount();

        PlayerGatherData data = plugin.dataStore().get(player.getUniqueId());

        // Legendary Requests (Tier 7) track independently of the normal tier pool.
        plugin.legendaryRequestService().addProgress(player, data, material, amount);

        int tier = plugin.gatherConfig().tierOfMaterial(material);
        if (tier == -1) return; // not part of the normal Tier 1-6 catch pool

        // Fishdex — every distinct catchable material discovered gets logged,
        // independent of tasks/tiers, for the collection page.
        data.fishdexDiscovered.add(material);
        data.fishdexCatchCounts.merge(material, amount, Integer::sum);

        // Lucky Strike rolls on every relevant catch, independent of the active task.
        plugin.luckyStrikeService().roll(player, data, material, tier);
        plugin.taskService().addProgress(player, data, material, amount);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Pre-warm the cache so menus open instantly.
        plugin.dataStore().get(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.dataStore().unload(event.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteractMasterNpc(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        NamespacedKey npcKey = new NamespacedKey(plugin.host(), "angler_npc");
        if (Boolean.TRUE.equals(entity.getPersistentDataContainer().get(npcKey, PersistentDataType.BOOLEAN))) {
            event.setCancelled(true);
            plugin.menuManager().openMasterMenu(event.getPlayer());
        }
    }
}
