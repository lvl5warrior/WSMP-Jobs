package com.warriorssmp.jobs.common;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/** Thin wrapper around Vault, shared by every skill (each used to have an
 *  identical copy of this same class). */
public final class EconomyService {

    private final JobModule module;
    private Economy economy;

    public EconomyService(JobModule module) {
        this.module = module;
    }

    public boolean setupEconomy() {
        if (module.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> provider = module.getServer()
                .getServicesManager().getRegistration(Economy.class);
        if (provider == null) {
            return false;
        }
        economy = provider.getProvider();
        return economy != null;
    }

    public boolean isHooked() {
        return economy != null;
    }

    public boolean has(OfflinePlayer player, double amount) {
        return economy.has(player, amount);
    }

    public void deposit(OfflinePlayer player, double amount) {
        if (amount <= 0) return;
        economy.depositPlayer(player, amount);
    }

    /** Returns true if the withdrawal succeeded (i.e. the player could afford it). */
    public boolean withdraw(Player player, double amount) {
        if (amount <= 0) return true;
        if (!economy.has(player, amount)) return false;
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public String format(double amount) {
        return economy.format(amount);
    }
}
