package com.warriorssmp.jobs.hunter;

import com.warriorssmp.jobs.WSMPJobsPlugin;
import com.warriorssmp.jobs.common.JobModule;
import com.warriorssmp.jobs.hunter.command.GatherAdminCommand;
import com.warriorssmp.jobs.hunter.command.GatherCommand;
import com.warriorssmp.jobs.hunter.data.DataStore;
import com.warriorssmp.jobs.common.EconomyService;
import com.warriorssmp.jobs.hunter.listener.GatherListener;
import com.warriorssmp.jobs.hunter.menu.MenuManager;
import com.warriorssmp.jobs.hunter.task.GatherConfig;
import com.warriorssmp.jobs.hunter.task.LeaderboardService;
import com.warriorssmp.jobs.hunter.task.LegendaryRequestService;
import com.warriorssmp.jobs.hunter.task.LuckyStrikeService;
import com.warriorssmp.jobs.hunter.task.MasterNpcService;
import com.warriorssmp.jobs.hunter.task.PremiumService;
import com.warriorssmp.jobs.hunter.task.ShopService;
import com.warriorssmp.jobs.hunter.task.TaskService;

public final class HunterPlugin extends JobModule {

    private GatherConfig gatherConfig;
    private DataStore dataStore;
    private EconomyService economyService;
    private TaskService taskService;
    private LuckyStrikeService luckyStrikeService;
    private MasterNpcService masterNpcService;
    private LegendaryRequestService legendaryRequestService;
    private LeaderboardService leaderboardService;
    private ShopService shopService;
    private PremiumService premiumService;
    private MenuManager menuManager;

    public HunterPlugin(WSMPJobsPlugin host) {
        super(host, "hunter");
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.gatherConfig = new GatherConfig(this);
        this.dataStore = new DataStore(this);
        this.economyService = new EconomyService(this);
        economyService.setupEconomy();

        this.premiumService = new PremiumService(this);
        this.taskService = new TaskService(this, gatherConfig, dataStore, economyService, premiumService);
        this.luckyStrikeService = new LuckyStrikeService(this, gatherConfig, dataStore, economyService);
        this.masterNpcService = new MasterNpcService(this, gatherConfig, dataStore, economyService);
        this.legendaryRequestService = new LegendaryRequestService(gatherConfig, dataStore, economyService);
        this.leaderboardService = new LeaderboardService(dataStore);
        this.shopService = new ShopService(gatherConfig, dataStore, economyService, premiumService);
        this.menuManager = new MenuManager(this);

        registerEvents(new GatherListener(this));
        registerEvents(menuManager);

        GatherCommand gatherCommand = new GatherCommand(this);
        registerCommand("huntermenu", gatherCommand);
        registerCommand("huntertask", gatherCommand);
        registerCommand("hunterleaderboards", gatherCommand);
        registerCommand("hunterbuffs", gatherCommand);
        registerCommand("huntershop", gatherCommand);

        GatherAdminCommand adminCommand = new GatherAdminCommand(this);
        registerCommand("huntershopadmin", adminCommand);
        registerCommand("huntermenuadmin", adminCommand);
        registerCommand("huntereditor", adminCommand);
        registerCommand("huntermaster", adminCommand);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new com.warriorssmp.jobs.hunter.HunterPlaceholders(this).register();
            getLogger().info("Hooked into PlaceholderAPI.");
        }

        runTaskTimerAsynchronously(dataStore::saveAll, 20L * 60 * 5, 20L * 60 * 5);

        logStartupSummary();
    }

    public void logStartupSummary() {
        java.util.logging.Logger log = getLogger();
        log.info("==================================================");
        log.info("  WSMP-Jobs / Hunter — Config Summary");
        log.info("==================================================");

        log.info("[Tiers]");
        for (var tier : gatherConfig.allTiers()) {
            log.info("  " + org.bukkit.ChatColor.stripColor(tier.display()) + " (Lv" + tier.minLevel() + "+): "
                    + tier.resources().size() + " resource(s), " + tier.baseCoins() + " points/task"
                    + (tier.premium() ? " [premium]" : " [free]"));
        }
        if (gatherConfig.allTiers().isEmpty()) {
            log.warning("  No tiers loaded at all — check the 'tiers:' section in hunter/config.yml!");
        }

        log.info("[Shop]");
        log.info("  " + gatherConfig.shopItems().size() + " item(s) loaded");

        log.info("[Legendary Requests]");
        log.info("  " + gatherConfig.legendaryRequests().size() + " request(s) loaded"
                + (gatherConfig.triadTrial() != null ? ", Triad Trial loaded" : " (no Triad Trial defined — expected in a single-skill plugin)"));

        log.info("[Economy]");
        log.info("  Points: in-plugin currency, not tied to Vault");
        log.info("  Vault:  " + (economyService.isHooked() ? "hooked (unused by core loop; reserved for future use)" : "not found (optional)"));

        log.info("[Premium]");
        log.info("  " + premiumService.grantedUuids().size() + " player(s) manually granted premium via the Admin Panel");
        log.info("  Server operators always count as premium automatically");

        log.info("==================================================");
    }

    @Override
    public void onDisable() {
        if (dataStore != null) {
            dataStore.saveAll();
        }
    }

    public GatherConfig gatherConfig() {
        return gatherConfig;
    }

    public DataStore dataStore() {
        return dataStore;
    }

    /** Direct, compile-time accessor for WSMP-JobBar (now a module of
     *  this same plugin) to read a player's level without reflection. */
    public long totalXpFor(java.util.UUID uuid) {
        return dataStore.get(uuid).totalXp;
    }

    public EconomyService economy() {
        return economyService;
    }

    public TaskService taskService() {
        return taskService;
    }

    public LuckyStrikeService luckyStrikeService() {
        return luckyStrikeService;
    }

    public MasterNpcService masterNpcService() {
        return masterNpcService;
    }

    public LegendaryRequestService legendaryRequestService() {
        return legendaryRequestService;
    }

    public LeaderboardService leaderboardService() {
        return leaderboardService;
    }

    public ShopService shopService() {
        return shopService;
    }

    public PremiumService premiumService() {
        return premiumService;
    }

    public MenuManager menuManager() {
        return menuManager;
    }
}
