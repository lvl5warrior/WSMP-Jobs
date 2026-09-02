package com.warriorssmp.jobs.jobbar;

import com.warriorssmp.jobs.WSMPJobsPlugin;
import com.warriorssmp.jobs.common.XpTable;
import com.warriorssmp.jobs.cooking.CookingPlugin;
import com.warriorssmp.jobs.farming.FarmingPlugin;
import com.warriorssmp.jobs.fishing.FishingPlugin;
import com.warriorssmp.jobs.hunter.HunterPlugin;
import com.warriorssmp.jobs.mining.MiningPlugin;
import com.warriorssmp.jobs.woodcutting.WoodcuttingPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Reads each player's level out of each of the other six skill modules.
 * This used to be reflection-based (JobBar was a standalone plugin with no
 * compile-time relationship to Mining/Woodcutting/etc.) — now that it's a
 * module of the same WSMP-Jobs plugin as the six it reads from, that
 * reason is gone, so this is just real, checked method calls instead:
 * `((MiningPlugin) host.getModule("mining")).totalXpFor(uuid)` instead of
 * five reflective hops per lookup. No more "its API may have changed"
 * fallback warnings — a real compile error would catch that instead.
 */
public final class JobLookup {

    public record JobInfo(String moduleKey, String displayName, Material icon) {}

    private static final List<JobInfo> SKILLS = List.of(
            new JobInfo("mining", "Mining", Material.DIAMOND_PICKAXE),
            new JobInfo("woodcutting", "Woodcutting", Material.DIAMOND_AXE),
            new JobInfo("farming", "Farming", Material.DIAMOND_HOE),
            new JobInfo("fishing", "Fishing", Material.FISHING_ROD),
            new JobInfo("cooking", "Cooking", Material.CAKE),
            new JobInfo("hunter", "Hunter", Material.DIAMOND_SWORD)
    );

    public record JobLevel(String displayName, Material icon, int level, long totalXp,
                              long xpIntoLevel, long xpForNextLevel, boolean maxLevel) {

        public double progressFraction() {
            if (maxLevel || xpForNextLevel <= 0) return 1.0;
            double frac = (double) xpIntoLevel / xpForNextLevel;
            return Math.max(0.0, Math.min(1.0, frac));
        }
    }

    private final WSMPJobsPlugin host;

    public JobLookup(WSMPJobsPlugin host) {
        this.host = host;
    }

    /** Which of the six skills actually enabled successfully — normally all
     *  six, but a skill that threw during its own onEnable() (see
     *  WSMPJobsPlugin's try/catch around each module) would be missing,
     *  so this still checks rather than assuming. */
    public List<JobInfo> installedJobs() {
        List<JobInfo> present = new ArrayList<>();
        for (JobInfo info : SKILLS) {
            if (host.getModule(info.moduleKey()) != null) present.add(info);
        }
        return present;
    }

    public List<JobLevel> getAllLevels(UUID uuid) {
        List<JobLevel> results = new ArrayList<>();
        for (JobInfo info : SKILLS) {
            Long totalXp = totalXpFor(info.moduleKey(), uuid);
            if (totalXp == null) continue;

            int level = XpTable.levelForXp(totalXp);
            long xpAtLevel = XpTable.xpForLevel(level);
            long xpAtNext = XpTable.xpForNextLevel(level);
            boolean maxLevel = level >= XpTable.MAX_LEVEL;

            results.add(new JobLevel(info.displayName(), info.icon(), level, totalXp,
                    totalXp - xpAtLevel, Math.max(1, xpAtNext - xpAtLevel), maxLevel));
        }
        return results;
    }

    private Long totalXpFor(String moduleKey, UUID uuid) {
        var module = host.getModule(moduleKey);
        if (module == null) return null;
        return switch (moduleKey) {
            case "mining" -> ((MiningPlugin) module).totalXpFor(uuid);
            case "woodcutting" -> ((WoodcuttingPlugin) module).totalXpFor(uuid);
            case "farming" -> ((FarmingPlugin) module).totalXpFor(uuid);
            case "fishing" -> ((FishingPlugin) module).totalXpFor(uuid);
            case "cooking" -> ((CookingPlugin) module).totalXpFor(uuid);
            case "hunter" -> ((HunterPlugin) module).totalXpFor(uuid);
            default -> null;
        };
    }

    /** Opens that skill's own main menu for the player — direct call to
     *  `menuManager().openMainMenu(player)`, no reflection. */
    public boolean openJobMenu(JobInfo info, Player player) {
        var module = host.getModule(info.moduleKey());
        if (module == null) return false;
        switch (info.moduleKey()) {
            case "mining" -> ((MiningPlugin) module).menuManager().openMainMenu(player);
            case "woodcutting" -> ((WoodcuttingPlugin) module).menuManager().openMainMenu(player);
            case "farming" -> ((FarmingPlugin) module).menuManager().openMainMenu(player);
            case "fishing" -> ((FishingPlugin) module).menuManager().openMainMenu(player);
            case "cooking" -> ((CookingPlugin) module).menuManager().openMainMenu(player);
            case "hunter" -> ((HunterPlugin) module).menuManager().openMainMenu(player);
            default -> {
                return false;
            }
        }
        return true;
    }
}
