package tw.betterteam.economy.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import tw.betterteam.economy.BetterEconomy;
import tw.betterteam.economy.model.PlayerBalance;
import tw.betterteam.economy.service.EconomyService;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;

/**
 * PlaceholderAPI expansion for economy placeholders
 * Prefix: %economy_...%
 */
public class EconomyExpansion extends PlaceholderExpansion {

    private final EconomyService economyService;
    private final BetterEconomy plugin;

    public EconomyExpansion(EconomyService economyService) {
        this.economyService = economyService;
        this.plugin = BetterEconomy.getInstance();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "economy";
    }

    @Override
    public @NotNull String getAuthor() {
        return "BetterTeam";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return null;
        }

        // Basic balance placeholders
        if (params.equals("balance")) {
            return formatBalance(economyService.getBalance(player.getUniqueId()));
        }

        if (params.equals("balance_raw")) {
            return economyService.getBalance(player.getUniqueId()).toPlainString();
        }

        if (params.equals("balance_formatted")) {
            return formatBalanceWithSymbol(economyService.getBalance(player.getUniqueId()));
        }

        // Rank placeholders
        if (params.equals("rank")) {
            int rank = economyService.getPlayerRank(player.getUniqueId());
            return rank > 0 ? String.valueOf(rank) : "N/A";
        }

        // Top balance placeholders (e.g., %economy_rank_1_name%, %economy_rank_1_balance%)
        if (params.startsWith("rank_")) {
            return handleRankPlaceholder(params);
        }

        // Server statistics
        if (params.equals("total_balance")) {
            return formatBalance(economyService.getTotalBalance());
        }

        if (params.equals("total_players")) {
            return String.valueOf(economyService.getTotalPlayerCount());
        }

        // Currency symbol
        if (params.equals("currency_symbol")) {
            return getCurrencySymbol();
        }

        return null;
    }

    /**
     * Handle rank placeholders like %economy_rank_1_name%, %economy_rank_1_balance%
     */
    private String handleRankPlaceholder(String params) {
        // Parse rank_N_type format
        String[] parts = params.split("_");
        if (parts.length < 3) {
            return null;
        }

        try {
            int rankNumber = Integer.parseInt(parts[1]);
            String type = parts[2];

            if (rankNumber < 1 || rankNumber > 10) {
                return null;
            }

            List<PlayerBalance> topBalances = economyService.getTopBalances(rankNumber);
            if (topBalances.size() < rankNumber) {
                return null;
            }

            PlayerBalance playerBalance = topBalances.get(rankNumber - 1);

            switch (type) {
                case "name":
                    return playerBalance.getUsername();
                case "balance":
                    return formatBalance(playerBalance.getBalance());
                default:
                    return null;
            }
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Format balance for display
     */
    private String formatBalance(BigDecimal balance) {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance();
        String separator = plugin.getConfigManager().getThousandsSeparator();
        if (separator != null && !separator.isEmpty()) {
            symbols.setGroupingSeparator(separator.charAt(0));
        }

        DecimalFormat df = new DecimalFormat("#,##0.00", symbols);
        df.setMaximumFractionDigits(plugin.getConfigManager().getDecimalPlaces());
        df.setMinimumFractionDigits(plugin.getConfigManager().getDecimalPlaces());
        return df.format(balance);
    }

    /**
     * Format balance with currency symbol
     */
    private String formatBalanceWithSymbol(BigDecimal balance) {
        String formatted = formatBalance(balance);
        String symbol = getCurrencySymbol();
        String position = plugin.getConfigManager().getSymbolPosition();

        if ("PREFIX".equalsIgnoreCase(position)) {
            return symbol + formatted;
        } else {
            return formatted + symbol;
        }
    }

    /**
     * Get currency symbol
     */
    private String getCurrencySymbol() {
        String symbolType = plugin.getConfigManager().getCurrencySymbol();

        return switch (symbolType.toUpperCase()) {
            case "DOLLAR" -> "$";
            case "EURO" -> "€";
            case "POUND" -> "£";
            case "YEN" -> "¥";
            case "WON" -> "₩";
            case "RUBLE" -> "₽";
            case "CENT" -> "¢";
            case "CUSTOM" -> plugin.getConfigManager().getCustomSymbol();
            default -> "$";
        };
    }
}
