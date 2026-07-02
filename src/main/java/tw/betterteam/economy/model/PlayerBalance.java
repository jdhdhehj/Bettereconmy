package tw.betterteam.economy.model;

import java.math.BigDecimal;
import java.util.UUID;

public class PlayerBalance {
    private final UUID uuid;
    private final String username;
    private BigDecimal balance;
    private long version;
    private long updatedAt;
    private String updatedByServer;

    public PlayerBalance(UUID uuid, String username, BigDecimal balance) {
        this.uuid = uuid;
        this.username = username;
        this.balance = balance;
        this.version = 0;
        this.updatedAt = System.currentTimeMillis();
        this.updatedByServer = "";
    }

    public PlayerBalance(UUID uuid, String username, BigDecimal balance, long version, long updatedAt, String updatedByServer) {
        this.uuid = uuid;
        this.username = username;
        this.balance = balance;
        this.version = version;
        this.updatedAt = updatedAt;
        this.updatedByServer = updatedByServer;
    }

    // Getters and Setters
    public UUID getUuid() {
        return uuid;
    }

    public String getUsername() {
        return username;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUpdatedByServer() {
        return updatedByServer;
    }

    public void setUpdatedByServer(String updatedByServer) {
        this.updatedByServer = updatedByServer;
    }
}
