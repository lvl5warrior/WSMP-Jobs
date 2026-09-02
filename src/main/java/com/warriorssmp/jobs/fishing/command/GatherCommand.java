package com.warriorssmp.jobs.fishing.command;

import com.warriorssmp.jobs.fishing.FishingPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class GatherCommand implements CommandExecutor {

    private final FishingPlugin plugin;

    public GatherCommand(FishingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        switch (command.getName().toLowerCase()) {
            case "fishmenu" -> plugin.menuManager().openMainMenu(player);
            case "fishtask" -> plugin.menuManager().openTaskDetails(player);
            case "fishleaderboards" -> plugin.menuManager().openLeaderboardHub(player);
            case "fishbuffs" -> plugin.menuManager().openBuffs(player);
            case "fishshop" -> {
                if (!player.hasPermission("fishing.admin") && !isNearAngler(player)) {
                    var loc = plugin.masterNpcService().masterLocation();
                    if (loc == null) {
                        player.sendMessage("§cThe Angler hasn't been placed yet — ask an admin.");
                    } else {
                        player.sendMessage("§cYou need to be near the Angler to open the shop.");
                    }
                    return true;
                }
                plugin.menuManager().openShop(player, 1);
            }
            default -> {return false;}
        }
        return true;
    }

    /** /fishshop is proximity-gated to the Angler NPC, per the design doc —
     *  same rule the NPC right-click path already satisfies just by existing.
     *  Admins (fishing.admin, default: op) bypass this so the shop can be
     *  tested/edited without needing an NPC placed first. */
    private boolean isNearAngler(Player player) {
        var loc = plugin.masterNpcService().masterLocation();
        if (loc == null) return false;
        if (loc.getWorld() == null || !loc.getWorld().equals(player.getWorld())) return false;
        double radius = plugin.getConfig().getDouble("settings.npc-shop-radius", 5.0);
        return loc.distanceSquared(player.getLocation()) <= radius * radius;
    }
}
