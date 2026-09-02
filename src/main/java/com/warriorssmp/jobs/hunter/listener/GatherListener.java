package com.warriorssmp.jobs.hunter.listener;

import com.warriorssmp.jobs.hunter.HunterPlugin;
import com.warriorssmp.jobs.hunter.data.PlayerGatherData;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.Set;

/**
 * Hunter's whole loop runs off EntityDeathEvent — no blocks, no crafting, no
 * fishing rod. There's no "cheesy afk farm" prevention needed at the
 * placement level (you can't place-then-kill the way you can place-then-break
 * a block), but AFK mob farms are absolutely a thing in vanilla Minecraft, so
 * the real anti-cheese mechanism here is the equipment-tier requirement
 * (EquipmentConditions) — a task's kills only count if your gear meets that
 * tier's minimum, which most AFK farm setups (fists, no armor) won't clear.
 */
public final class GatherListener implements Listener {

    private final HunterPlugin plugin;

    private static final Set<EntityType> BOSSES = Set.of(EntityType.WARDEN, EntityType.WITHER, EntityType.ENDER_DRAGON);

    public GatherListener(HunterPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        EntityType type = event.getEntityType();
        PlayerGatherData data = plugin.dataStore().get(killer.getUniqueId());

        // Legendary Requests (Tier 7) track independently of the normal tier pool.
        plugin.legendaryRequestService().addProgress(killer, data, type, 1);

        if (BOSSES.contains(type)) {
            data.lifetimeBossKills++;
        }

        int tier = plugin.gatherConfig().tierOfMaterial(type);
        if (tier == -1) return; // not part of the normal Tier 1-7 monster pool

        // Perfect Strike rolls on every relevant kill, independent of the active task.
        plugin.luckyStrikeService().roll(killer, data, type, tier);
        plugin.taskService().addProgress(killer, data, type, 1);
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
        NamespacedKey npcKey = new NamespacedKey(plugin.host(), "combatmaster_npc");
        if (Boolean.TRUE.equals(entity.getPersistentDataContainer().get(npcKey, PersistentDataType.BOOLEAN))) {
            event.setCancelled(true);
            plugin.menuManager().openMasterMenu(event.getPlayer());
        }
    }
}
