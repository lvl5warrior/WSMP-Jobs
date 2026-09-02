package com.warriorssmp.jobs.hunter.command;

import com.warriorssmp.jobs.hunter.HunterPlugin;
import com.warriorssmp.jobs.hunter.data.PlayerGatherData;
import com.warriorssmp.jobs.hunter.model.GatherTask;
import com.warriorssmp.jobs.common.XpTable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataType;

/**
 * Backs /huntershopadmin, /huntermenuadmin, /huntereditor, and /huntermaster.
 * /huntereditor with no arguments opens the in-game Admin Panel GUI (reload,
 * global buffs, and browsing/editing online players' level, points, and
 * active task). The reload/giveglobalbuff subcommands still work directly
 * from the console or chat for scripting.
 */
public final class GatherAdminCommand implements CommandExecutor {

    private final HunterPlugin plugin;

    public GatherAdminCommand(HunterPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("hunter.admin")) {
            sender.sendMessage("§cYou don't have permission for that.");
            return true;
        }

        switch (command.getName().toLowerCase()) {
            case "huntermaster" -> handleMaster(sender, args);
            case "huntermenuadmin" -> handleMenuAdmin(sender, args);
            case "huntereditor" -> handleEditor(sender, args);
            case "huntershopadmin" -> handleShopAdmin(sender, args);
            default -> {return false;}
        }
        return true;
    }

    // /huntermaster set|remove
    private void handleMaster(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cMust be run in-game.");
            return;
        }
        if (args.length == 0) {
            sender.sendMessage("§eUsage: /huntermaster <set|remove>");
            return;
        }
        if (args[0].equalsIgnoreCase("set")) {
            Location loc = player.getLocation();
            Villager villager = (Villager) loc.getWorld().spawnEntity(loc, EntityType.VILLAGER);
            villager.setAI(false);
            villager.setInvulnerable(true);
            villager.setSilent(true);
            villager.setCustomName("§6§lCombat Master");
            villager.setCustomNameVisible(true);
            villager.getPersistentDataContainer().set(
                    new NamespacedKey(plugin.host(), "combatmaster_npc"), PersistentDataType.BOOLEAN, true);
            plugin.masterNpcService().setMasterLocation(loc);
            sender.sendMessage("§aCombat Master NPC placed. Right-click it to open the Combat Master menu.");
        } else if (args[0].equalsIgnoreCase("remove")) {
            int removed = 0;
            NamespacedKey key = new NamespacedKey(plugin.host(), "combatmaster_npc");
            for (var e : player.getWorld().getEntitiesByClass(Villager.class)) {
                if (Boolean.TRUE.equals(e.getPersistentDataContainer().get(key, PersistentDataType.BOOLEAN))) {
                    e.remove();
                    removed++;
                }
            }
            sender.sendMessage("§aRemoved " + removed + " Combat Master NPC(s) in this world.");
        } else {
            sender.sendMessage("§eUsage: /huntermaster <set|remove>");
        }
    }

    // /huntermenuadmin view <player>
    // /huntermenuadmin setlevel <player> <level>
    // /huntermenuadmin settask <player> <material> <amount>
    private void handleMenuAdmin(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§eUsage: /huntermenuadmin <view|setlevel|settask|setpoints|reset> <player> [args]");
            return;
        }
        switch (args[0].toLowerCase()) {
            case "view" -> {
                if (args.length < 2) {
                    sender.sendMessage("§eUsage: /huntermenuadmin view <player>");
                    return;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage("§cPlayer not found.");
                    return;
                }
                PlayerGatherData data = plugin.dataStore().get(target.getUniqueId());
                int level = plugin.taskService().levelOf(data);
                GatherTask task = data.activeTask;
                sender.sendMessage("§6--- Hunter data: " + target.getName() + " ---");
                sender.sendMessage("§7Level: §f" + level + " §7(XP: " + data.totalXp + ")");
                sender.sendMessage("§7Points: §f" + data.points);
                sender.sendMessage("§7Streak: §f" + data.streak);
                sender.sendMessage("§7Active Task: §f" + (task == null ? "none" : task.displayName()
                        + " (" + task.progress() + "/" + task.required() + ")"));
                sender.sendMessage("§7Lifetime Tasks Completed: §f" + data.lifetimeTasksCompleted);
                sender.sendMessage("§7Blocked Monsters: §f" + data.blockedMonsters.size());
            }
            case "setlevel" -> {
                if (args.length < 3) {
                    sender.sendMessage("§eUsage: /huntermenuadmin setlevel <player> <level>");
                    return;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage("§cPlayer not found.");
                    return;
                }
                int level;
                try {
                    level = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cLevel must be a number.");
                    return;
                }
                PlayerGatherData data = plugin.dataStore().get(target.getUniqueId());
                data.totalXp = XpTable.xpForLevel(level);
                sender.sendMessage("§aSet " + target.getName() + "'s Hunter level to " + level + ".");
            }
            case "settask" -> {
                if (args.length < 4) {
                    sender.sendMessage("§eUsage: /huntermenuadmin settask <player> <mob_type> <amount>");
                    return;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                EntityType type;
                try {
                    type = EntityType.valueOf(args[2].toUpperCase());
                } catch (IllegalArgumentException e) {
                    sender.sendMessage("§cUnknown mob type.");
                    return;
                }
                if (target == null) {
                    sender.sendMessage("§cPlayer not found.");
                    return;
                }
                int amount;
                try {
                    amount = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cAmount must be a number.");
                    return;
                }
                PlayerGatherData data = plugin.dataStore().get(target.getUniqueId());
                int tier = plugin.gatherConfig().tierOfMaterial(type);
                data.activeTask = new GatherTask(type, Math.max(tier, 1), amount, 0);
                sender.sendMessage("§aForced " + target.getName() + "'s task to " + type.name() + " x" + amount + ".");
            }
            case "setpoints" -> {
                if (args.length < 3) {
                    sender.sendMessage("§eUsage: /huntermenuadmin setpoints <player> <amount>");
                    return;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage("§cPlayer not found.");
                    return;
                }
                long amount;
                try {
                    amount = Long.parseLong(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cAmount must be a number.");
                    return;
                }
                PlayerGatherData data = plugin.dataStore().get(target.getUniqueId());
                data.points = amount;
                sender.sendMessage("§aSet " + target.getName() + "'s Points to " + amount + ".");
            }
            case "reset" -> {
                if (args.length < 2) {
                    sender.sendMessage("§eUsage: /huntermenuadmin reset <player>");
                    return;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage("§cPlayer not found.");
                    return;
                }
                PlayerGatherData data = plugin.dataStore().get(target.getUniqueId());
                data.resetAll();
                sender.sendMessage("§cReset all Hunter progress for " + target.getName() + ".");
            }
            default -> sender.sendMessage("§eUsage: /huntermenuadmin <view|setlevel|settask|setpoints|reset> <player> [args]");
        }
    }

    // /huntereditor  (no args)          -> opens the in-game Admin Panel GUI
    // /huntereditor reload
    // /huntereditor giveglobalbuff <xp|pointboost|bettertasks> <minutes>
    private void handleEditor(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                plugin.menuManager().openAdminPanel(player);
            } else {
                sender.sendMessage("§eUsage: /huntereditor <reload|giveglobalbuff>");
            }
            return;
        }
        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.reloadConfig();
                plugin.gatherConfig().load();
                plugin.logStartupSummary();
                sender.sendMessage("§aCombat Master config reloaded — check console for a full summary.");
            }
            case "giveglobalbuff" -> {
                if (args.length < 3) {
                    sender.sendMessage("§eUsage: /huntereditor giveglobalbuff <xp|pointboost|bettertasks> <minutes>");
                    return;
                }
                long minutes;
                try {
                    minutes = Long.parseLong(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cMinutes must be a number.");
                    return;
                }
                long expiry = System.currentTimeMillis() + minutes * 60_000L;
                int affected = 0;
                for (Player online : Bukkit.getOnlinePlayers()) {
                    PlayerGatherData data = plugin.dataStore().get(online.getUniqueId());
                    switch (args[1].toLowerCase()) {
                        case "xp" -> data.xpBoostExpiry = Math.max(data.xpBoostExpiry, expiry);
                        case "pointboost" -> data.pointBoostExpiry = Math.max(data.pointBoostExpiry, expiry);
                        case "bettertasks" -> data.betterTasksExpiry = Math.max(data.betterTasksExpiry, expiry);
                        default -> {
                            sender.sendMessage("§cUnknown buff type. Use xp, pointboost, or bettertasks.");
                            return;
                        }
                    }
                    affected++;
                }
                sender.sendMessage("§aGave " + args[1] + " buff (" + minutes + "m) to " + affected + " online player(s).");
            }
            default -> sender.sendMessage("§eUsage: /huntereditor <reload|giveglobalbuff>");
        }
    }

    // /huntershopadmin list
    private void handleShopAdmin(CommandSender sender, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("list")) {
            sender.sendMessage("§6--- Combat Master Shop Items ---");
            plugin.gatherConfig().shopItems().forEach((id, item) ->
                    sender.sendMessage("§7" + id + " §f- " + item.display() + " §7(" + item.cost() + " points"
                            + (item.premium() ? ", premium" : "") + ")"));
            sender.sendMessage("§7Edit prices/items directly in config.yml, then run /huntereditor reload.");
        } else {
            sender.sendMessage("§eUsage: /huntershopadmin list");
            sender.sendMessage("§7Shop items are defined in config.yml — edit there, then /huntereditor reload.");
        }
    }
}
