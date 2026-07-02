package tw.betterteam.economy.database;

import org.bukkit.plugin.java.JavaPlugin;
import tw.betterteam.economy.config.ConfigManager;
import tw.betterteam.economy.database.impl.MySQLStorage;
import tw.betterteam.economy.database.impl.SQLiteStorage;

import java.util.logging.Level;

public class DatabaseManager {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private Storage storage;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configManager = null; // Will be set during initialize()
    }

    public void initialize() {
        try {
            ConfigManager configManager = ((tw.betterteam.economy.BetterEconomy) plugin).getConfigManager();
            DatabaseType type = configManager.getDatabaseType();

            plugin.getLogger().info("Initializing " + type.getDisplayName() + " database...");

            switch (type) {
                case SQLITE:
                    storage = new SQLiteStorage(plugin, configManager);
                    break;
                case MYSQL:
                    storage = new MySQLStorage(plugin, configManager);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown database type: " + type);
            }

            storage.initialize();
            plugin.getLogger().info("§aDatabase initialized successfully!");

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize database", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    public void shutdown() {
        if (storage != null) {
            try {
                storage.shutdown();
                plugin.getLogger().info("Database shutdown successfully.");
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error during database shutdown", e);
            }
        }
    }

    public Storage getStorage() {
        if (storage == null) {
            throw new RuntimeException("Database not initialized");
        }
        return storage;
    }
}
