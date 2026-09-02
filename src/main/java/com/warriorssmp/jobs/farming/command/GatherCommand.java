package com.warriorssmp.jobs.farming.command;

import com.warriorssmp.jobs.farming.FarmingPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class GatherCommand implements CommandExecutor {

    private final FarmingPlugin plugin;

    public GatherCommand(FarmingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        switch (command.getName().toLowerCase()) {
            case "farmmenu" -> plugin.menuManager().openMainMenu(player);
            case "farmtask" -> plugin.menuManager().openTaskDetails(player);
            case "farmleaderboards" -> plugin.menuManager().openLeaderboardHub(player);
            case "farmbuffs" -> plugin.menuManager().openBuffs(player);
            case "farmshop" -> {
                if (!player.hasPermission("farming.admin") && !isNearFarmer(player)) {
                    var loc = plugin.masterNpcService().masterLocation();
                    if (loc == null) {
                        player.sendMessage("§cThe Master Farmer hasn't been placed yet — ask an admin.");
                    } else {
                        player.sendMessage("§cYou need to be near the Master Farmer to open the shop.");
                    }
                    return true;
                }
                plugin.menuManager().openShop(player, 1);
            }
            default -> {return false;}
        }
        return true;
    }

    /** /farmshop is proximity-gated to the Master Farmer NPC, matching the
     *  Fishing/Cooking plugins. Admins (farming.admin, default: op) bypass this
     *  so the shop can be tested/edited without needing an NPC placed first. */
    private boolean isNearFarmer(Player player) {
        var loc = plugin.masterNpcService().masterLocation();
        if (loc == null) return false;
        if (loc.getWorld() == null || !loc.getWorld().equals(player.getWorld())) return false;
        double radius = plugin.getConfig().getDouble("settings.npc-shop-radius", 5.0);
        return loc.distanceSquared(player.getLocation()) <= radius * radius;
    }
}
