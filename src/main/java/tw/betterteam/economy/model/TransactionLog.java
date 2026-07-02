package tw.betterteam.economy.model;

import java.math.BigDecimal;
import java.util.UUID;

public class TransactionLog {
    private long id;
    private UUID uuid;
    private TransactionType type;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String operator;
    private String serverId;
    private String reason;
    private long createdAt;

    public enum TransactionType {
        GIVE("增加"),
        TAKE("扣除"),
        SET("設定"),
        PAY_SEND("轉帳支出"),
        PAY_RECEIVE("轉帳收入"),
        WIPE("全服重置"),
        STARTING_BALANCE("初始餘額"),
        PLUGIN_API("插件API");

        private final String displayName;

        TransactionType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public TransactionLog(UUID uuid, TransactionType type, BigDecimal amount, BigDecimal balanceAfter,
                          String operator, String serverId, String reason) {
        this.uuid = uuid;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.operator = operator;
        this.serverId = serverId;
        this.reason = reason;
        this.createdAt = System.currentTimeMillis();
    }

    public TransactionLog(long id, UUID uuid, TransactionType type, BigDecimal amount, BigDecimal balanceAfter,
                          String operator, String serverId, String reason, long createdAt) {
        this.id = id;
        this.uuid = uuid;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.operator = operator;
        this.serverId = serverId;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    // Getters
    public long getId() {
        return id;
    }

    public UUID getUuid() {
        return uuid;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public String getOperator() {
        return operator;
    }

    public String getServerId() {
        return serverId;
    }

    public String getReason() {
        return reason;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
