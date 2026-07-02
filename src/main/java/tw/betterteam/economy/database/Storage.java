package tw.betterteam.economy.database;

import tw.betterteam.economy.model.PlayerBalance;
import tw.betterteam.economy.model.TransactionLog;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Storage interface for economy database operations
 * Implementations should handle async operations to avoid blocking the main thread
 */
public interface Storage {

    /**
     * Initialize database and create tables
     */
    void initialize();

    /**
     * Shutdown database connection
     */
    void shutdown();

    // ========== Player Balance Operations ==========

    /**
     * Get player balance by UUID
     */
    Optional<PlayerBalance> getBalance(UUID uuid);

    /**
     * Get player balance asynchronously
     */
    CompletableFuture<Optional<PlayerBalance>> getBalanceAsync(UUID uuid);

    /**
     * Set player balance
     * @return true if successful, false if player not found
     */
    boolean setBalance(UUID uuid, String username, BigDecimal balance, String serverId);

    /**
     * Add amount to player balance
     */
    boolean addBalance(UUID uuid, String username, BigDecimal amount, String serverId);

    /**
     * Subtract amount from player balance
     */
    boolean subtractBalance(UUID uuid, String username, BigDecimal amount, String serverId);

    /**
     * Create new player account with starting balance
     */
    boolean createPlayerAccount(UUID uuid, String username, BigDecimal startingBalance, String serverId);

    /**
     * Check if player account exists
     */
    boolean playerExists(UUID uuid);

    /**
     * Get all players sorted by balance (for baltop)
     */
    List<PlayerBalance> getTopBalances(int limit);

    /**
     * Get total balance across all players
     */
    BigDecimal getTotalBalance();

    /**
     * Get player rank by balance
     */
    int getPlayerRank(UUID uuid);

    // ========== Transaction Logging ==========

    /**
     * Log a transaction
     */
    void logTransaction(TransactionLog log);

    /**
     * Get transaction history for a player with pagination
     */
    List<TransactionLog> getTransactionHistory(UUID uuid, int page, int pageSize);

    /**
     * Get total transaction count for a player
     */
    int getTransactionCount(UUID uuid);

    /**
     * Clear old transaction logs based on retention days
     */
    void clearOldTransactionLogs(int retentionDays);

    // ========== Wipe Operations ==========

    /**
     * Wipe all player balances and account records
     */
    boolean wipeAllBalances(BigDecimal startingBalance, String serverId);

    /**
     * Wipe all transaction logs
     */
    boolean wipeAllTransactionLogs();

    /**
     * Wipe all pending cross-server sync entries
     */
    boolean wipeAllSyncEntries();

    // ========== Cross-Server Sync ==========

    /**
     * Get unprocessed sync entries from other servers
     */
    List<SyncQueueEntry> getUnprocessedSyncEntries(String thisServerId);

    /**
     * Mark sync entry as processed
     */
    void markSyncEntryProcessed(long entryId);

    /**
     * Add entry to sync queue
     */
    void addSyncQueueEntry(UUID uuid, BigDecimal delta, String sourceServer);

    /**
     * Clear old sync queue entries
     */
    void clearOldSyncEntries(int ageHours);

    // ========== Batch Operations ==========

    /**
     * Get all players
     */
    List<PlayerBalance> getAllPlayers();

    /**
     * Get count of all players with balances
     */
    int getTotalPlayerCount();
}
