package com.warriorssmp.jobs.common;

import com.warriorssmp.jobs.WSMPJobsPlugin;
import org.bukkit.Server;
import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Every skill (Mining, Woodcutting, Farming, Fishing, Cooking, Hunter) is a
 * JobModule instead of a JavaPlugin now that they all live in one real
 * plugin. The point of this class: every existing method call in the six
 * ported codebases — plugin.getConfig(), plugin.getDataFolder(),
 * plugin.getLogger(), plugin.getServer(), plugin.reloadConfig() — keeps
 * working completely unchanged, because JobModule provides the exact
 * same method shapes, just implemented via delegation to the one real
 * WSMPJobsPlugin instead of inherited from JavaPlugin. That's what let
 * almost all of each skill's actual logic (task services, menus, listeners,
 * commands) get ported with a package rename and nothing else.
 *
 * Three things genuinely required a real Plugin instance and could not be
 * faked this way — event registration, task scheduling, and NamespacedKey
 * construction — those three call sites in each skill were changed to go
 * through host() instead of `this`/`plugin`; see registerEvents()/
 * runTaskTimer()/etc below, which do that routing in one place.
 */
public abstract class JobModule {

    protected final WSMPJobsPlugin host;
    private final String skillName;
    private final File moduleFolder;
    private final File configFile;
    private FileConfiguration config;

    protected JobModule(WSMPJobsPlugin host, String skillName) {
        this.host = host;
        this.skillName = skillName;
        this.moduleFolder = new File(host.getDataFolder(), skillName);
        if (!moduleFolder.exists()) moduleFolder.mkdirs();
        this.configFile = new File(moduleFolder, "config.yml");
    }

    // ---------------------------------------------------------------- lifecycle

    public abstract void onEnable();

    public abstract void onDisable();

    // ---------------------------------------------------------------- config
    // Each skill gets its own config.yml under plugins/WSMP-Jobs/<skill>/,
    // not the single shared plugin config.yml — exactly the "separate config
    // files inside one plugin" the person asked for.

    public FileConfiguration getConfig() {
        if (config == null) reloadConfig();
        return config;
    }

    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
        // Layer in defaults from the jar-embedded default, same as Bukkit's
        // own getConfig() does for a normal single plugin.
        InputStream defaults = host.getResource(skillName + "/config.yml");
        if (defaults != null) {
            config.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defaults, StandardCharsets.UTF_8)));
        }
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            getLogger().log(Level.WARNING, "Could not save " + skillName + "/config.yml", e);
        }
    }

    /** Copies the jar-embedded default config to disk if the skill doesn't
     *  have one yet — same behavior as JavaPlugin#saveDefaultConfig(). */
    public void saveDefaultConfig() {
        if (configFile.exists()) return;
        try (InputStream in = host.getResource(skillName + "/config.yml")) {
            if (in == null) {
                getLogger().warning("No embedded default config found for " + skillName + " — starting with an empty config.");
                configFile.createNewFile();
                return;
            }
            java.nio.file.Files.copy(in, configFile.toPath());
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Could not create default config for " + skillName, e);
        }
    }

    // ---------------------------------------------------------------- delegation to the real plugin

    public File getDataFolder() {
        return moduleFolder;
    }

    public Logger getLogger() {
        return host.getLogger();
    }

    public Server getServer() {
        return host.getServer();
    }

    /** The one real Plugin instance — needed anywhere vanilla Bukkit API
     *  insists on a genuine JavaPlugin (event registration, scheduling,
     *  NamespacedKey), which a JobModule can never itself be, since only
     *  one real plugin can exist per plugin.yml. */
    public Plugin host() {
        return host;
    }

    /** Reach another module living in the same plugin — e.g. a skill's menu
     *  wanting a "Jobs Overview" button that opens JobBar's overview.
     *  Returns null if that module isn't registered (shouldn't normally
     *  happen since all modules register before any of them enable, but
     *  callers should still treat null as "not available" rather than
     *  assuming). Valid keys: mining, woodcutting, farming, fishing,
     *  cooking, hunter, jobbar. */
    public JobModule sibling(String name) {
        return host.getModule(name);
    }

    public InputStream getResource(String path) {
        return host.getResource(skillName + "/" + path);
    }

    // ---------------------------------------------------------------- registration helpers
    // Routes through host() so event registration/scheduling/commands work
    // against the one real plugin, the way vanilla Bukkit requires.

    public void registerEvents(Listener listener) {
        host.getServer().getPluginManager().registerEvents(listener, host);
    }

    public void registerCommand(String name, CommandExecutor executor) {
        var command = host.getCommand(name);
        if (command == null) {
            getLogger().warning("Command '" + name + "' isn't declared in plugin.yml — check the commands: section.");
            return;
        }
        command.setExecutor(executor);
    }

    public BukkitTask runTaskTimer(Runnable task, long delay, long period) {
        return host.getServer().getScheduler().runTaskTimer(host, task, delay, period);
    }

    public BukkitTask runTaskTimerAsynchronously(Runnable task, long delay, long period) {
        return host.getServer().getScheduler().runTaskTimerAsynchronously(host, task, delay, period);
    }

    public BukkitTask runTask(Runnable task) {
        return host.getServer().getScheduler().runTask(host, task);
    }
}
