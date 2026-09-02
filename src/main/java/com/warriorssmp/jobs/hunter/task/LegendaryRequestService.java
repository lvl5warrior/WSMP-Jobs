package com.warriorssmp.jobs.hunter.task;

import com.warriorssmp.jobs.hunter.data.DataStore;
import com.warriorssmp.jobs.hunter.data.PlayerGatherData;
import com.warriorssmp.jobs.common.EconomyService;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.Random;

/**
 * Tier 7 has a real normal task pool for Hunter (Warden/Wither/Ender Dragon —
 * see config.yml), but there's still one Legendary Request on top of it for a
 * bigger, rarer undertaking, matching every other WSMP skill plugin's Tier 7
 * capstone pattern.
 */
public final class LegendaryRequestService {

    private final GatherConfig config;
    private final DataStore dataStore;
    private final EconomyService economy;
    private final Random random = new Random();

    public LegendaryRequestService(GatherConfig config, DataStore dataStore, EconomyService economy) {
        this.config = config;
        this.dataStore = dataStore;
        this.economy = economy;
    }

    public boolean isAvailable(PlayerGatherData data, String requestId) {
        return data.legendaryReadyAt.getOrDefault(requestId, 0L) <= System.currentTimeMillis();
    }

    /** Rolls a fresh target amount for a simple request if it doesn't have one yet. */
    public int ensureTarget(PlayerGatherData data, GatherConfig.LegendaryRequestDef def) {
        Integer existing = data.legendaryTarget.get(def.id());
        if (existing != null) return existing;
        int span = Math.max(0, def.maxAmount() - def.minAmount());
        int target = def.minAmount() + (span == 0 ? 0 : random.nextInt(span + 1));
        data.legendaryTarget.put(def.id(), target);
        data.legendaryProgress.put(def.id(), 0);
        return target;
    }

    /**
     * Called from the entity-death listener alongside normal task and Perfect
     * Strike progress — legendary requests track independently of the
     * player's active tier task, so the same mob can feed both at once.
     */
    public void addProgress(Player player, PlayerGatherData data, EntityType material, int amount) {
        for (GatherConfig.LegendaryRequestDef def : config.legendaryRequests().values()) {
            if (def.material() != material) continue;
            if (!isAvailable(data, def.id())) continue;

            int target = ensureTarget(data, def);
            int progress = data.legendaryProgress.getOrDefault(def.id(), 0) + amount;
            progress = Math.min(progress, target);
            data.legendaryProgress.put(def.id(), progress);

            if (progress >= target) {
                completeSimple(player, data, def);
            }
        }
    }

    private void completeSimple(Player player, PlayerGatherData data, GatherConfig.LegendaryRequestDef def) {
        data.points += def.yield();
        data.lifetimeLegendaryCompleted++;
        data.legendaryTarget.remove(def.id());
        data.legendaryProgress.remove(def.id());
        data.legendaryReadyAt.put(def.id(), System.currentTimeMillis() + def.cooldownMillis());

        player.sendMessage(Component.text("§6§lLEGENDARY REQUEST COMPLETE §7— §f" + def.display()
                + " §7(+" + com.warriorssmp.jobs.hunter.model.PointsUtil.format(def.yield()) + ")"));
    }
}
