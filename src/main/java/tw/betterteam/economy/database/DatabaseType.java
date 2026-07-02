package tw.betterteam.economy.database;

public enum DatabaseType {
    SQLITE("SQLite"),
    MYSQL("MySQL");

    private final String displayName;

    DatabaseType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
