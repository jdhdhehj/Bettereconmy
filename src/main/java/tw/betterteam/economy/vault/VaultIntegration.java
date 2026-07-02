package tw.betterteam.economy.vault;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import tw.betterteam.economy.BetterEconomy;
import tw.betterteam.economy.service.EconomyService;

/**
 * Vault integration handler
 * Registers the economy implementation with Vault
 */
public class VaultIntegration {

    private final EconomyService economyService;
    private VaultEconomyImpl vaultEconomy;

    public VaultIntegration(EconomyService economyService) {
        this.economyService = economyService;
    }

    /**
     * Register economy provider with Vault
     */
    public void registerEconomyProvider() {
        BetterEconomy plugin = BetterEconomy.getInstance();
        
        this.vaultEconomy = new VaultEconomyImpl(economyService, plugin.getConfigManager());
        
        Bukkit.getServicesManager().register(
            Economy.class,
            vaultEconomy,
            plugin,
            ServicePriority.High
        );

        plugin.getLogger().info("§aVault Economy provider registered!");
    }

    /**
     * Unregister economy provider from Vault
     */
    public void unregisterEconomyProvider() {
        if (vaultEconomy != null) {
            Bukkit.getServicesManager().unregister(vaultEconomy);
        }
    }

    public VaultEconomyImpl getVaultEconomy() {
        return vaultEconomy;
    }
}
