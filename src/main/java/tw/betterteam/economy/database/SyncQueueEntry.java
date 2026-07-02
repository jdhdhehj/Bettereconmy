package tw.betterteam.economy.database;

import java.math.BigDecimal;
import java.util.UUID;

public class SyncQueueEntry {
    private long id;
    private UUID uuid;
    private BigDecimal delta;
    private String sourceServer;
    private boolean processed;
    private long createdAt;

    public SyncQueueEntry(UUID uuid, BigDecimal delta, String sourceServer) {
        this.uuid = uuid;
        this.delta = delta;
        this.sourceServer = sourceServer;
        this.processed = false;
        this.createdAt = System.currentTimeMillis();
    }

    public SyncQueueEntry(long id, UUID uuid, BigDecimal delta, String sourceServer, boolean processed, long createdAt) {
        this.id = id;
        this.uuid = uuid;
        this.delta = delta;
        this.sourceServer = sourceServer;
        this.processed = processed;
        this.createdAt = createdAt;
    }

    // Getters
    public long getId() {
        return id;
    }

    public UUID getUuid() {
        return uuid;
    }

    public BigDecimal getDelta() {
        return delta;
    }

    public String getSourceServer() {
        return sourceServer;
    }

    public boolean isProcessed() {
        return processed;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    // Setters
    public void setId(long id) {
        this.id = id;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }
}
