package com.warriorssmp.jobs.hunter.command;

import com.warriorssmp.jobs.hunter.HunterPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class GatherCommand implements CommandExecutor {

    private final HunterPlugin plugin;

    public GatherCommand(HunterPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        switch (command.getName().toLowerCase()) {
            case "huntermenu" -> plugin.menuManager().openMainMenu(player);
            case "huntertask" -> plugin.menuManager().openTaskDetails(player);
            case "hunterleaderboards" -> plugin.menuManager().openLeaderboardHub(player);
            case "hunterbuffs" -> plugin.menuManager().openBuffs(player);
            case "huntershop" -> {
                if (!player.hasPermission("hunter.admin") && !isNearCombatMaster(player)) {
                    var loc = plugin.masterNpcService().masterLocation();
                    if (loc == null) {
                        player.sendMessage("§cThe Combat Master hasn't been placed yet — ask an admin.");
                    } else {
                        player.sendMessage("§cYou need to be near the Combat Master to open the shop.");
                    }
                    return true;
                }
                plugin.menuManager().openShop(player, 1);
            }
            default -> {return false;}
        }
        return true;
    }

    /** /huntershop is proximity-gated to the Combat Master NPC, per the design doc —
     *  same rule the NPC right-click path already satisfies just by existing.
     *  Admins (hunter.admin, default: op) bypass this so the shop can be
     *  tested/edited without needing an NPC placed first. */
    private boolean isNearCombatMaster(Player player) {
        var loc = plugin.masterNpcService().masterLocation();
        if (loc == null) return false;
        if (loc.getWorld() == null || !loc.getWorld().equals(player.getWorld())) return false;
        double radius = plugin.getConfig().getDouble("settings.npc-shop-radius", 5.0);
        return loc.distanceSquared(player.getLocation()) <= radius * radius;
    }
}
