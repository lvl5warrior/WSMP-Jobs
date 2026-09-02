package com.warriorssmp.jobs.jobbar;

import com.warriorssmp.jobs.WSMPJobsPlugin;
import com.warriorssmp.jobs.common.JobModule;
import com.warriorssmp.jobs.jobbar.command.JobsCommand;

public final class JobBarPlugin extends JobModule {

    private JobLookup jobLookup;
    private MenuManager menuManager;

    public JobBarPlugin(WSMPJobsPlugin host) {
        super(host, "jobbar");
    }

    @Override
    public void onEnable() {
        this.jobLookup = new JobLookup(host);
        this.menuManager = new MenuManager(this);

        registerEvents(menuManager);
        registerCommand("jobs", new JobsCommand(this));

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new JobBarPlaceholders(this).register();
            getLogger().info("Hooked into PlaceholderAPI.");
        }

        logInstalledJobs();
    }

    @Override
    public void onDisable() {
        // Nothing to persist — JobBar is a read-only view over the other
        // six jobs' own data, it doesn't keep any of its own.
    }

    private void logInstalledJobs() {
        var installed = jobLookup.installedJobs();
        if (installed.isEmpty()) {
            getLogger().warning("No job modules found — /jobs will show "
                    + "\"No WSMP job plugins found\" until at least one of Mining/Woodcutting/Farming/"
                    + "Fishing/Cooking/Hunter is enabled.");
        } else {
            getLogger().info("Tracking " + installed.size() + " job module(s): "
                    + installed.stream().map(JobLookup.JobInfo::displayName).reduce((a, b) -> a + ", " + b).orElse(""));
        }
    }

    public JobLookup jobLookup() {
        return jobLookup;
    }

    public MenuManager menuManager() {
        return menuManager;
    }
}
