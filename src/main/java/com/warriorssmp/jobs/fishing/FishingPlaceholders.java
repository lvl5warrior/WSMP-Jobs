package com.warriorssmp.jobs.fishing;

import com.warriorssmp.jobs.fishing.data.PlayerGatherData;
import com.warriorssmp.jobs.fishing.model.GatherTier;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Registers %fishing_level%, %fishing_xp%, %fishing_tier%, %fishing_streak%.
 * Useful for TAB / scoreboard integration alongside your other skill plugins.
 */
public final class FishingPlaceholders extends PlaceholderExpansion {

    private final FishingPlugin plugin;

    public FishingPlaceholders(FishingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "fishing";
    }

    @Override
    public @NotNull String getAuthor() {
        return "WarriorsSMP";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";
        PlayerGatherData data = plugin.dataStore().get(player.getUniqueId());
        int level = plugin.taskService().levelOf(data);

        return switch (params.toLowerCase()) {
            case "level" -> String.valueOf(level);
            case "xp" -> String.valueOf(data.totalXp);
            case "streak" -> String.valueOf(data.streak);
            case "tier" -> {
                GatherTier tier = plugin.gatherConfig().tierForLevel(level);
                yield tier == null ? "" : tier.rawName();
            }
            default -> null;
        };
    }
}
