package com.warriorssmp.jobs.jobbar;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Registers %wsmpjobbar_highest_job%, %wsmpjobbar_highest_level%, and
 * %wsmpjobbar_highest_job_level% — whichever installed job the player
 * has the highest level in, computed the same way as the /jobs menu (via
 * JobLookup's direct calls into each job module). If two jobs tie for
 * highest, whichever comes first in JobLookup's fixed order wins.
 */
public final class JobBarPlaceholders extends PlaceholderExpansion {

    private final JobBarPlugin plugin;

    public JobBarPlaceholders(JobBarPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "wsmpjobbar";
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

        List<JobLookup.JobLevel> levels = plugin.jobLookup().getAllLevels(player.getUniqueId());
        Optional<JobLookup.JobLevel> highest = levels.stream()
                .max(Comparator.comparingInt(JobLookup.JobLevel::level));

        return switch (params.toLowerCase()) {
            case "highest_job" -> highest.map(JobLookup.JobLevel::displayName).orElse("");
            case "highest_level" -> highest.map(l -> String.valueOf(l.level())).orElse("");
            case "highest_job_level" -> highest.map(l -> "§7" + l.displayName() + " - §6Lv." + l.level()).orElse("");
            default -> null;
        };
    }
}
