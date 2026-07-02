package tw.betterteam.economy.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import tw.betterteam.economy.database.DatabaseType;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ConfigManager {

    private final JavaPlugin plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    
    private File configFile;
    private File messagesFile;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() throws IOException {
        // Create data folder if it doesn't exist
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        configFile = new File(plugin.getDataFolder(), "config.yml");
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");

        // Save default config if doesn't exist
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        // Load configs
        this.config = YamlConfiguration.loadConfiguration(configFile);
        this.messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public void reloadConfig() throws IOException {
        loadConfig();
    }

    // ========== Database Configuration ==========

    public DatabaseType getDatabaseType() {
        return DatabaseType.valueOf(config.getString("database.type", "SQLITE").toUpperCase());
    }

    public String getSQLiteFile() {
        return config.getString("database.sqlite.file", "economy.db");
    }

    public String getMySQLHost() {
        return config.getString("database.mysql.host", "127.0.0.1");
    }

    public int getMySQLPort() {
        return config.getInt("database.mysql.port", 3306);
    }

    public String getMySQLDatabase() {
        return config.getString("database.mysql.database", "economy");
    }

    public String getMySQLUsername() {
        return config.getString("database.mysql.username", "root");
    }

    public String getMySQLPassword() {
        return config.getString("database.mysql.password", "");
    }

    public boolean isMySQLSSLEnabled() {
        return config.getBoolean("database.mysql.useSSL", false);
    }

    public int getMaxPoolSize() {
        return config.getInt("database.mysql.pool.maximum-pool-size", 10);
    }

    public int getMinIdle() {
        return config.getInt("database.mysql.pool.minimum-idle", 2);
    }

    public long getConnectionTimeout() {
        return config.getLong("database.mysql.pool.connection-timeout", 30000);
    }

    public long getIdleTimeout() {
        return config.getLong("database.mysql.pool.idle-timeout", 600000);
    }

    public long getMaxLifetime() {
        return config.getLong("database.mysql.pool.max-lifetime", 1800000);
    }

    public String getTablePrefix() {
        return config.getString("database.mysql.table-prefix", "eco_");
    }

    // ========== Cross-Server Sync Configuration ==========

    public boolean isCrossSyncEnabled() {
        return config.getBoolean("cross-server-sync.enabled", false);
    }

    public String getServerId() {
        return config.getString("cross-server-sync.server-id", "server-1");
    }

    public long getSyncPollingInterval() {
        return config.getLong("cross-server-sync.polling-interval-ticks", 100);
    }

    public String getConflictResolution() {
        return config.getString("cross-server-sync.conflict-resolution", "DELTA_MERGE");
    }

    public boolean isOptimisticLockingEnabled() {
        return config.getBoolean("cross-server-sync.optimistic-locking", true);
    }

    // ========== Economy Configuration ==========

    public int getDecimalPlaces() {
        return config.getInt("economy.decimal-places", 2);
    }

    public boolean isNegativeBalanceAllowed() {
        return config.getBoolean("economy.allow-negative-balance", false);
    }

    public boolean isStartingBalanceEnabled() {
        return config.getBoolean("economy.starting-balance.enabled", true);
    }

    public double getStartingBalance() {
        return config.getDouble("economy.starting-balance.amount", 1000.0);
    }

    public boolean isBalanceCapEnabled() {
        return config.getBoolean("economy.balance-cap.enabled", false);
    }

    public double getBalanceCap() {
        return config.getDouble("economy.balance-cap.amount", 1000000.0);
    }

    public double getMinimumPayAmount() {
        return config.getDouble("economy.pay.minimum-amount", 1.0);
    }

    public boolean isAllowSelfPay() {
        return config.getBoolean("economy.pay.allow-self-pay", false);
    }

    public boolean isAllowOfflineTarget() {
        return config.getBoolean("economy.pay.allow-offline-target", true);
    }

    public boolean isAbbreviateEnabled() {
        return config.getBoolean("economy.format.abbreviate", false);
    }

    public String getCurrencySymbol() {
        return config.getString("economy.format.currency-symbol", "DOLLAR");
    }

    public String getCustomSymbol() {
        return config.getString("economy.format.custom-symbol", "coin");
    }

    public String getSymbolPosition() {
        return config.getString("economy.format.symbol-position", "SUFFIX");
    }

    public String getThousandsSeparator() {
        return config.getString("economy.format.thousands-separator", ",");
    }

    public int getBaltopEntriesPerPage() {
        return config.getInt("economy.baltop.entries-per-page", 10);
    }

    public int getBaltopCacheSeconds() {
        return config.getInt("economy.baltop.cache-seconds", 60);
    }

    public java.util.List<String> getBaltopExcludedPlayers() {
        return config.getStringList("economy.baltop.excluded-players");
    }

    public int getHistoryEntriesPerPage() {
        return config.getInt("economy.history.entries-per-page", 10);
    }

    public int getHistoryRetentionDays() {
        return config.getInt("economy.history.retention-days", 90);
    }

    // ========== Vault Configuration ==========

    public boolean isVaultEnabled() {
        return config.getBoolean("vault.enabled", true);
    }

    public String getCurrencyNameSingular() {
        return config.getString("vault.currency-name-singular", "Coin");
    }

    public String getCurrencyNamePlural() {
        return config.getString("vault.currency-name-plural", "Coins");
    }

    // ========== PlaceholderAPI Configuration ==========

    public boolean isPlaceholderAPIEnabled() {
        return config.getBoolean("placeholderapi.enabled", true);
    }

    // ========== Debug Configuration ==========

    public boolean isDebugEnabled() {
        return config.getBoolean("debug", false);
    }

    // ========== Messages ==========

    public String getMessage(String path) {
        String message = messages.getString(path, "");
        return translateColorCodes(message).replace("{prefix}", getPrefix());
    }

    public String getMessage(String path, String... replacements) {
        String message = getMessage(path);
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace(replacements[i], replacements[i + 1]);
            }
        }
        return message;
    }

    public String getPrefix() {
        return translateColorCodes(messages.getString("prefix", "&8[&6Economy&8] &r"));
    }

    private String translateColorCodes(String message) {
        if (message == null) return "";
        return message.replace("&", "§");
    }
}
