package com.warriorssmp.jobs;

import com.warriorssmp.jobs.common.JobModule;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The one real plugin — every skill (Mining, Woodcutting, Farming, Fishing,
 * Cooking, Hunter) plus JobBar (the /jobs overview menu) is a JobModule
 * living inside this single jar, each with its own config file under
 * plugins/WSMP-Jobs/<skill>/config.yml. The point: a bug in shared
 * infrastructure (the per-player data cache, the Vault wrapper) now gets
 * fixed in ONE place instead of seven, and going forward there's one jar to
 * drop into plugins/ instead of seven.
 */
public final class WSMPJobsPlugin extends JavaPlugin {

    private final List<JobModule> modules = new ArrayList<>();
    private final Map<String, JobModule> modulesByName = new HashMap<>();

    @Override
    public void onEnable() {
        // Register jobbar last, after the six skills it reads from — not
        // strictly required for correctness (all six are already registered
        // into modulesByName before any module's onEnable() runs, and
        // JobLookup only reads from them lazily at query time, not at
        // construction time), but keeps the list's ordering intuitive.
        register("mining", new com.warriorssmp.jobs.mining.MiningPlugin(this));
        register("woodcutting", new com.warriorssmp.jobs.woodcutting.WoodcuttingPlugin(this));
        register("farming", new com.warriorssmp.jobs.farming.FarmingPlugin(this));
        register("fishing", new com.warriorssmp.jobs.fishing.FishingPlugin(this));
        register("cooking", new com.warriorssmp.jobs.cooking.CookingPlugin(this));
        register("hunter", new com.warriorssmp.jobs.hunter.HunterPlugin(this));
        register("jobbar", new com.warriorssmp.jobs.jobbar.JobBarPlugin(this));

        for (JobModule module : modules) {
            try {
                module.onEnable();
            } catch (Exception e) {
                getLogger().severe("Failed to enable a job module — see stack trace below. "
                        + "The rest of WSMP-Jobs will continue starting.");
                e.printStackTrace();
            }
        }

        getLogger().info("WSMP-Jobs enabled — " + modules.size() + " job module(s) loaded.");
    }

    @Override
    public void onDisable() {
        for (JobModule module : modules) {
            try {
                module.onDisable();
            } catch (Exception e) {
                getLogger().severe("Failed to disable a job module cleanly — see stack trace below.");
                e.printStackTrace();
            }
        }
    }

    private void register(String name, JobModule module) {
        modules.add(module);
        modulesByName.put(name, module);
    }

    /** Lets other plugins (WSMP-Classes, anything else that used to look up
     *  a separate "WSMP-Mining"-style plugin by name) reach an individual
     *  job module now that they all live inside this one plugin — e.g.
     *  getModule("mining") returns the same MiningPlugin instance that used
     *  to be its own registered plugin, with the same dataStore()/
     *  menuManager()/etc. methods on it. Valid keys: mining, woodcutting,
     *  farming, fishing, cooking, hunter, jobbar. */
    public JobModule getModule(String name) {
        return modulesByName.get(name);
    }
}
