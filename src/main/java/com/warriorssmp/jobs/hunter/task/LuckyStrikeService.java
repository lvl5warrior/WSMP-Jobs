package com.warriorssmp.jobs.hunter.task;

import com.warriorssmp.jobs.hunter.HunterPlugin;
import com.warriorssmp.jobs.hunter.data.DataStore;
import com.warriorssmp.jobs.hunter.data.PlayerGatherData;
import com.warriorssmp.jobs.common.EconomyService;
import com.warriorssmp.jobs.hunter.model.PointsUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Random;

/**
 * Perfect Strike — Hunter's version of the bonus-roll mechanic every WSMP
 * skill plugin has. Unlike the others, there's nothing to "duplicate" about
 * a kill, so a trigger pays bonus Points instead (scaled by tier) rather than
 * doubling any item.
 */
public final class LuckyStrikeService {

    private final HunterPlugin plugin;
    private final GatherConfig config;
    private final DataStore dataStore;
    private final EconomyService economy;
    private final Random random = new Random();

    public LuckyStrikeService(HunterPlugin plugin, GatherConfig config, DataStore dataStore, EconomyService economy) {
        this.plugin = plugin;
        this.config = config;
        this.dataStore = dataStore;
        this.economy = economy;
    }

    public boolean roll(Player player, PlayerGatherData data, EntityType type, int tierOfMonster) {
        double chance = config.luckyStrikeChance(tierOfMonster);

        if (random.nextDouble() >= chance) {
            return false;
        }

        data.lifetimeLuckyStrikes++;
        long bonusPoints = tierOfMonster * 5L;
        data.points += bonusPoints;

        String mobName = type.name().toLowerCase().replace('_', ' ');

        player.sendActionBar(Component.text("§e⚔ PERFECT STRIKE! §7+" + PointsUtil.format(bonusPoints) + " (" + mobName + ")"));
        player.showTitle(Title.title(
                Component.text("§e⚔ PERFECT STRIKE!"),
                Component.text("§7+" + PointsUtil.format(bonusPoints) + " Points"),
                Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(1200), Duration.ofMillis(300))
        ));
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.6f);

        return true;
    }
}
