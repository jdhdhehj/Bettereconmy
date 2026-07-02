package tw.betterteam.economy.database.impl;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.java.JavaPlugin;
import tw.betterteam.economy.config.ConfigManager;
import tw.betterteam.economy.database.Storage;
import tw.betterteam.economy.database.SyncQueueEntry;
import tw.betterteam.economy.model.PlayerBalance;
import tw.betterteam.economy.model.TransactionLog;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class MySQLStorage implements Storage {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private HikariDataSource dataSource;
    private final String tablePrefix;

    public MySQLStorage(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.tablePrefix = configManager.getTablePrefix();
    }

    @Override
    public void initialize() {
        try {
            // Setup HikariCP connection pool for MySQL
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s?useSSL=%s&serverTimezone=UTC&autoReconnect=true",
                    configManager.getMySQLHost(),
                    configManager.getMySQLPort(),
                    configManager.getMySQLDatabase(),
                    configManager.isMySQLSSLEnabled()
            ));
            config.setUsername(configManager.getMySQLUsername());
            config.setPassword(configManager.getMySQLPassword());
            config.setMaximumPoolSize(configManager.getMaxPoolSize());
            config.setMinimumIdle(configManager.getMinIdle());
            config.setConnectionTimeout(configManager.getConnectionTimeout());
            config.setIdleTimeout(configManager.getIdleTimeout());
            config.setMaxLifetime(configManager.getMaxLifetime());
            config.setAutoCommit(true);

            this.dataSource = new HikariDataSource(config);

            // Test connection
            try (Connection conn = dataSource.getConnection()) {
                if (conn.isValid(5)) {
                    plugin.getLogger().info("MySQL connection successful!");
                }
            }

            // Create tables
            createTables();

            plugin.getLogger().info("MySQL database initialized successfully!");

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize MySQL database", e);
            throw new RuntimeException("MySQL initialization failed", e);
        }
    }

    private void createTables() {
        try (Connection conn = dataSource.getConnection()) {
            // Create balances table
            String balancesTable = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "balances (" +
                    "uuid VARCHAR(36) PRIMARY KEY," +
                    "username VARCHAR(16)," +
                    "balance DECIMAL(20,4)," +
                    "version BIGINT DEFAULT 0," +
                    "updated_at BIGINT," +
                    "updated_by_server VARCHAR(64)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
            conn.createStatement().execute(balancesTable);

            // Create transaction logs table
            String logsTable = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "logs (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "uuid VARCHAR(36)," +
                    "type VARCHAR(20)," +
                    "amount DECIMAL(20,4)," +
                    "balance_after DECIMAL(20,4)," +
                    "operator VARCHAR(36)," +
                    "server_id VARCHAR(64)," +
                    "reason VARCHAR(255)," +
                    "created_at BIGINT," +
                    "KEY idx_uuid (uuid)," +
                    "KEY idx_created_at (created_at)," +
                    "CONSTRAINT fk_logs_uuid FOREIGN KEY (uuid) REFERENCES " + tablePrefix + "balances(uuid) ON DELETE CASCADE" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
            conn.createStatement().execute(logsTable);

            // Create sync queue table
            String syncQueueTable = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "sync_queue (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "uuid VARCHAR(36)," +
                    "delta DECIMAL(20,4)," +
                    "source_server VARCHAR(64)," +
                    "processed TINYINT DEFAULT 0," +
                    "created_at BIGINT," +
                    "KEY idx_source_server (source_server)," +
                    "KEY idx_processed (processed)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
            conn.createStatement().execute(syncQueueTable);

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create tables", e);
            throw new RuntimeException("Table creation failed", e);
        }
    }

    @Override
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    // ========== Player Balance Operations ==========

    @Override
    public Optional<PlayerBalance> getBalance(UUID uuid) {
        String sql = "SELECT uuid, username, balance, version, updated_at, updated_by_server FROM " + tablePrefix + "balances WHERE uuid = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new PlayerBalance(
                            UUID.fromString(rs.getString("uuid")),
                            rs.getString("username"),
                            new BigDecimal(rs.getString("balance")),
                            rs.getLong("version"),
                            rs.getLong("updated_at"),
                            rs.getString("updated_by_server")
                    ));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error getting balance for " + uuid, e);
        }
        return Optional.empty();
    }

    @Override
    public CompletableFuture<Optional<PlayerBalance>> getBalanceAsync(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> getBalance(uuid));
    }

    @Override
    public boolean setBalance(UUID uuid, String username, BigDecimal balance, String serverId) {
        String sql = "UPDATE " + tablePrefix + "balances SET balance = ?, version = version + 1, updated_at = ?, updated_by_server = ? WHERE uuid = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, balance);
            stmt.setLong(2, System.currentTimeMillis());
            stmt.setString(3, serverId);
            stmt.setString(4, uuid.toString());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error setting balance for " + uuid, e);
        }
        return false;
    }

    @Override
    public boolean addBalance(UUID uuid, String username, BigDecimal amount, String serverId) {
        String sql = "UPDATE " + tablePrefix + "balances SET balance = balance + ?, version = version + 1, updated_at = ?, updated_by_server = ? WHERE uuid = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, amount);
            stmt.setLong(2, System.currentTimeMillis());
            stmt.setString(3, serverId);
            stmt.setString(4, uuid.toString());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error adding balance for " + uuid, e);
        }
        return false;
    }

    @Override
    public boolean subtractBalance(UUID uuid, String username, BigDecimal amount, String serverId) {
        return addBalance(uuid, username, amount.negate(), serverId);
    }

    @Override
    public boolean createPlayerAccount(UUID uuid, String username, BigDecimal startingBalance, String serverId) {
        String sql = "INSERT INTO " + tablePrefix + "balances (uuid, username, balance, version, updated_at, updated_by_server) VALUES (?, ?, ?, 0, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, username);
            stmt.setBigDecimal(3, startingBalance);
            stmt.setLong(4, System.currentTimeMillis());
            stmt.setString(5, serverId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error creating account for " + username, e);
        }
        return false;
    }

    @Override
    public boolean playerExists(UUID uuid) {
        String sql = "SELECT 1 FROM " + tablePrefix + "balances WHERE uuid = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error checking player existence for " + uuid, e);
        }
        return false;
    }

    @Override
    public List<PlayerBalance> getTopBalances(int limit) {
        List<PlayerBalance> result = new ArrayList<>();
        String sql = "SELECT uuid, username, balance, version, updated_at, updated_by_server FROM " + tablePrefix + "balances ORDER BY balance DESC LIMIT ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(new PlayerBalance(
                            UUID.fromString(rs.getString("uuid")),
                            rs.getString("username"),
                            new BigDecimal(rs.getString("balance")),
                            rs.getLong("version"),
                            rs.getLong("updated_at"),
                            rs.getString("updated_by_server")
                    ));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error getting top balances", e);
        }
        return result;
    }

    @Override
    public BigDecimal getTotalBalance() {
        String sql = "SELECT COALESCE(SUM(balance), 0) as total FROM " + tablePrefix + "balances";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return new BigDecimal(rs.getString("total"));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error getting total balance", e);
        }
        return BigDecimal.ZERO;
    }

    @Override
    public int getPlayerRank(UUID uuid) {
        String sql = "SELECT COUNT(*) + 1 as rank FROM " + tablePrefix + "balances WHERE balance > (SELECT balance FROM " + tablePrefix + "balances WHERE uuid = ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("rank");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error getting player rank for " + uuid, e);
        }
        return -1;
    }

    // ========== Transaction Logging ==========

    @Override
    public void logTransaction(TransactionLog log) {
        String sql = "INSERT INTO " + tablePrefix + "logs (uuid, type, amount, balance_after, operator, server_id, reason, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, log.getUuid().toString());
            stmt.setString(2, log.getType().name());
            stmt.setBigDecimal(3, log.getAmount());
            stmt.setBigDecimal(4, log.getBalanceAfter());
            stmt.setString(5, log.getOperator());
            stmt.setString(6, log.getServerId());
            stmt.setString(7, log.getReason());
            stmt.setLong(8, log.getCreatedAt());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error logging transaction for " + log.getUuid(), e);
        }
    }

    @Override
    public List<TransactionLog> getTransactionHistory(UUID uuid, int page, int pageSize) {
        List<TransactionLog> result = new ArrayList<>();
        String sql = "SELECT id, uuid, type, amount, balance_after, operator, server_id, reason, created_at FROM " + tablePrefix + "logs " +
                "WHERE uuid = ? ORDER BY created_at DESC LIMIT ? OFFSET ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setInt(2, pageSize);
            stmt.setInt(3, (page - 1) * pageSize);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(new TransactionLog(
                            rs.getLong("id"),
                            UUID.fromString(rs.getString("uuid")),
                            TransactionLog.TransactionType.valueOf(rs.getString("type")),
                            new BigDecimal(rs.getString("amount")),
                            new BigDecimal(rs.getString("balance_after")),
                            rs.getString("operator"),
                            rs.getString("server_id"),
                            rs.getString("reason"),
                            rs.getLong("created_at")
                    ));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error getting transaction history for " + uuid, e);
        }
        return result;
    }

    @Override
    public int getTransactionCount(UUID uuid) {
        String sql = "SELECT COUNT(*) as count FROM " + tablePrefix + "logs WHERE uuid = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error getting transaction count for " + uuid, e);
        }
        return 0;
    }

    @Override
    public void clearOldTransactionLogs(int retentionDays) {
        if (retentionDays <= 0) return;

        String sql = "DELETE FROM " + tablePrefix + "logs WHERE created_at < ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            long cutoffTime = System.currentTimeMillis() - (retentionDays * 24L * 60 * 60 * 1000);
            stmt.setLong(1, cutoffTime);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error clearing old transaction logs", e);
        }
    }

    // ========== Wipe Operations ==========

    @Override
    public boolean wipeAllBalances(BigDecimal startingBalance, String serverId) {
        String deleteLogsSql = "DELETE FROM " + tablePrefix + "logs";
        String deleteBalancesSql = "DELETE FROM " + tablePrefix + "balances";

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(deleteLogsSql);
                stmt.executeUpdate(deleteBalancesSql);
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                plugin.getLogger().log(Level.WARNING, "Error wiping all balances", e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error wiping all balances", e);
        }
        return false;
    }

    @Override
    public boolean wipeAllTransactionLogs() {
        String sql = "DELETE FROM " + tablePrefix + "logs";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error wiping transaction logs", e);
        }
        return false;
    }

    @Override
    public boolean wipeAllSyncEntries() {
        String sql = "DELETE FROM " + tablePrefix + "sync_queue";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error wiping sync entries", e);
        }
        return false;
    }

    // ========== Cross-Server Sync ==========

    @Override
    public List<SyncQueueEntry> getUnprocessedSyncEntries(String thisServerId) {
        List<SyncQueueEntry> result = new ArrayList<>();
        String sql = "SELECT id, uuid, delta, source_server, processed, created_at FROM " + tablePrefix + "sync_queue WHERE source_server != ? AND processed = 0";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, thisServerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    SyncQueueEntry entry = new SyncQueueEntry(
                            rs.getLong("id"),
                            UUID.fromString(rs.getString("uuid")),
                            new BigDecimal(rs.getString("delta")),
                            rs.getString("source_server"),
                            rs.getInt("processed") == 1,
                            rs.getLong("created_at")
                    );
                    result.add(entry);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error getting unprocessed sync entries", e);
        }
        return result;
    }

    @Override
    public void markSyncEntryProcessed(long entryId) {
        String sql = "UPDATE " + tablePrefix + "sync_queue SET processed = 1 WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, entryId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error marking sync entry as processed", e);
        }
    }

    @Override
    public void addSyncQueueEntry(UUID uuid, BigDecimal delta, String sourceServer) {
        String sql = "INSERT INTO " + tablePrefix + "sync_queue (uuid, delta, source_server, processed, created_at) VALUES (?, ?, ?, 0, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setBigDecimal(2, delta);
            stmt.setString(3, sourceServer);
            stmt.setLong(4, System.currentTimeMillis());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error adding sync queue entry", e);
        }
    }

    @Override
    public void clearOldSyncEntries(int ageHours) {
        String sql = "DELETE FROM " + tablePrefix + "sync_queue WHERE created_at < ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            long cutoffTime = System.currentTimeMillis() - (ageHours * 60L * 60 * 1000);
            stmt.setLong(1, cutoffTime);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error clearing old sync entries", e);
        }
    }

    // ========== Batch Operations ==========

    @Override
    public List<PlayerBalance> getAllPlayers() {
        List<PlayerBalance> result = new ArrayList<>();
        String sql = "SELECT uuid, username, balance, version, updated_at, updated_by_server FROM " + tablePrefix + "balances";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result.add(new PlayerBalance(
                        UUID.fromString(rs.getString("uuid")),
                        rs.getString("username"),
                        new BigDecimal(rs.getString("balance")),
                        rs.getLong("version"),
                        rs.getLong("updated_at"),
                        rs.getString("updated_by_server")
                ));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error getting all players", e);
        }
        return result;
    }

    @Override
    public int getTotalPlayerCount() {
        String sql = "SELECT COUNT(*) as count FROM " + tablePrefix + "balances";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error getting total player count", e);
        }
        return 0;
    }
}
