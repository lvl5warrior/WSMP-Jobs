package com.warriorssmp.jobs.jobbar.command;

import com.warriorssmp.jobs.jobbar.JobBarPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class JobsCommand implements CommandExecutor {

    private final JobBarPlugin plugin;

    public JobsCommand(JobBarPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        plugin.menuManager().openJobsMenu(player);
        return true;
    }
}
