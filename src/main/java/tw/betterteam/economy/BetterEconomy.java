package tw.betterteam.economy;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import tw.betterteam.economy.command.impl.BetterEconomyStatusCommand;
import tw.betterteam.economy.config.ConfigManager;
import tw.betterteam.economy.database.DatabaseManager;
import tw.betterteam.economy.placeholder.EconomyExpansion;
import tw.betterteam.economy.service.EconomyService;
import tw.betterteam.economy.vault.VaultIntegration;

import java.util.logging.Level;

/**
 * BetterEconomy - A comprehensive economy plugin for Purpur servers
 * Supporting Vault Economy API and cross-server synchronization
 */
public class BetterEconomy extends JavaPlugin {

    private static BetterEconomy instance;
    
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private EconomyService economyService;
    private VaultIntegration vaultIntegration;
    private EconomyExpansion placeholderExpansion;
    private boolean vaultRegistered;
    private boolean placeholderAPIRegistered;
    
    private BukkitTask syncTask;

    @Override
    public void onEnable() {
        instance = this;
        
        getLogger().info("§6正在載入 BetterEconomy v" + getDescription().getVersion() + "...");
        
        try {
            // Initialize configuration
            this.configManager = new ConfigManager(this);
            configManager.loadConfig();
            
            // Initialize database
            this.databaseManager = new DatabaseManager(this);
            databaseManager.initialize();
            
            // Initialize economy service
            this.economyService = new EconomyService(this, databaseManager);
            
            // Register commands
            registerCommands();

            // Register Vault economy provider if Vault is installed and enabled
            if (configManager.isVaultEnabled() && Bukkit.getPluginManager().isPluginEnabled("Vault")) {
                this.vaultIntegration = new VaultIntegration(economyService);
                vaultIntegration.registerEconomyProvider();
                this.vaultRegistered = true;
            } else if (configManager.isVaultEnabled()) {
                getLogger().warning("Vault 插件未找到或已停用，已跳過 Vault 整合。");
            }

            // Register PlaceholderAPI expansion if PlaceholderAPI is installed and enabled
            if (configManager.isPlaceholderAPIEnabled() && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                this.placeholderExpansion = new EconomyExpansion(economyService);
                this.placeholderAPIRegistered = placeholderExpansion.register();
                if (placeholderAPIRegistered) {
                    getLogger().info("§aPlaceholderAPI 擴充功能已註冊！");
                } else {
                    getLogger().warning("PlaceholderAPI 擴充功能註冊失敗。");
                }
            } else if (configManager.isPlaceholderAPIEnabled()) {
                getLogger().warning("PlaceholderAPI 插件未找到或已停用，已跳過擴充功能註冊。");
            }
            
            // Register player join listener so caches refresh when players join
            Bukkit.getPluginManager().registerEvents(new tw.betterteam.economy.listener.PlayerCacheRefreshListener(this), this);

            // Start cross-server sync if enabled
            if (configManager.isCrossSyncEnabled()) {
                startCrossServerSync();
            }
            
            getLogger().info("§aBetterEconomy 載入完成！");
            
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "BetterEconomy 載入失敗", e);
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        try {
            if (syncTask != null) {
                syncTask.cancel();
            }
            
            if (vaultIntegration != null) {
                vaultIntegration.unregisterEconomyProvider();
            }

            if (placeholderExpansion != null && placeholderAPIRegistered) {
                placeholderExpansion.unregister();
            }

            if (databaseManager != null) {
                if (economyService != null) {
                    economyService.shutdown();
                }
                databaseManager.shutdown();
            }
            
            getLogger().info("§eBetterEconomy 已停用。");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "插件關閉時發生錯誤", e);
        }
    }

    private void registerCommands() {
        new tw.betterteam.economy.command.impl.EconomyAdminCommand(this, economyService, configManager).register();
        new tw.betterteam.economy.command.impl.BalanceCommand(this, economyService, configManager).register();
        new tw.betterteam.economy.command.impl.BaltopCommand(this, economyService, configManager).register();
        new tw.betterteam.economy.command.impl.PayCommand(this, economyService, configManager).register();
        new BetterEconomyStatusCommand(this).register();
    }

    private void startCrossServerSync() {
        syncTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
            this,
            () -> economyService.syncCrossServer(),
            configManager.getSyncPollingInterval(),
            configManager.getSyncPollingInterval()
        );
        getLogger().info("§a跨服同步已啟動。");
    }

    public static BetterEconomy getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public EconomyService getEconomyService() {
        return economyService;
    }

    public boolean isVaultRegistered() {
        return vaultRegistered;
    }

    public boolean isPlaceholderAPIRegistered() {
        return placeholderAPIRegistered;
    }
}
