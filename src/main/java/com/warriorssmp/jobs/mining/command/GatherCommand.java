package com.warriorssmp.jobs.mining.command;

import com.warriorssmp.jobs.mining.MiningPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class GatherCommand implements CommandExecutor {

    private final MiningPlugin plugin;

    public GatherCommand(MiningPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        switch (command.getName().toLowerCase()) {
            case "minemenu" -> plugin.menuManager().openMainMenu(player);
            case "minetask" -> plugin.menuManager().openTaskDetails(player);
            case "mineleaderboards" -> plugin.menuManager().openLeaderboardHub(player);
            case "minebuffs" -> plugin.menuManager().openBuffs(player);
            case "mineshop" -> {
                if (!player.hasPermission("mining.admin") && !isNearMiner(player)) {
                    var loc = plugin.masterNpcService().masterLocation();
                    if (loc == null) {
                        player.sendMessage("§cThe Master Miner hasn't been placed yet — ask an admin.");
                    } else {
                        player.sendMessage("§cYou need to be near the Master Miner to open the shop.");
                    }
                    return true;
                }
                plugin.menuManager().openShop(player, 1);
            }
            default -> {return false;}
        }
        return true;
    }

    /** /mineshop is proximity-gated to the Master Miner NPC, matching the
     *  Fishing/Cooking plugins. Admins (mining.admin, default: op) bypass this
     *  so the shop can be tested/edited without needing an NPC placed first. */
    private boolean isNearMiner(Player player) {
        var loc = plugin.masterNpcService().masterLocation();
        if (loc == null) return false;
        if (loc.getWorld() == null || !loc.getWorld().equals(player.getWorld())) return false;
        double radius = plugin.getConfig().getDouble("settings.npc-shop-radius", 5.0);
        return loc.distanceSquared(player.getLocation()) <= radius * radius;
    }
}
