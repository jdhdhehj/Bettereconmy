package tw.betterteam.economy.command.impl;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import tw.betterteam.economy.command.EconomyCommand;
import tw.betterteam.economy.config.ConfigManager;
import tw.betterteam.economy.model.PlayerBalance;
import tw.betterteam.economy.service.EconomyService;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * /baltop command implementation
 */
public class BaltopCommand extends EconomyCommand {

    private final EconomyService economyService;
    private final ConfigManager configManager;

    public BaltopCommand(JavaPlugin plugin, EconomyService economyService, ConfigManager configManager) {
        super(plugin);
        this.economyService = economyService;
        this.configManager = configManager;
    }

    @Override
    public void register() {
        plugin.getCommand("baltop").setExecutor(this);
        plugin.getCommand("baltop").setTabCompleter(this);
    }

    @Override
    protected String getCommandName() {
        return "baltop";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("economy.baltop")) {
            sender.sendMessage(configManager.getMessage("general.no-permission"));
            return true;
        }

        int page = 1;
        int entriesPerPage = configManager.getBaltopEntriesPerPage();

        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
                if (page < 1) {
                    page = 1;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(configManager.getMessage("baltop.invalid-page"));
                return true;
            }
        }

        List<String> excludedPlayers = configManager.getBaltopExcludedPlayers().stream()
                .filter(name -> name != null && !name.isBlank())
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(Collectors.toList());

        int totalPlayers = economyService.getTotalPlayerCount();
        int maxPage = Math.max((totalPlayers + entriesPerPage - 1) / entriesPerPage, 1);

        // Perform retrieval asynchronously
        final int requestedPage = page;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int fetchLimit = Math.min(totalPlayers, requestedPage * entriesPerPage + Math.max(excludedPlayers.size() * entriesPerPage, 20));
            List<PlayerBalance> topBalances;
            final java.util.concurrent.atomic.AtomicReference<List<PlayerBalance>> filteredBalancesRef = new java.util.concurrent.atomic.AtomicReference<>();
            int displayPage = Math.min(requestedPage, maxPage);
            int startIndex = (displayPage - 1) * entriesPerPage;

            while (true) {
                topBalances = economyService.getTopBalances(fetchLimit);
                filteredBalancesRef.set(topBalances.stream()
                        .filter(balance -> excludedPlayers.isEmpty() || !excludedPlayers.contains(balance.getUsername().toLowerCase(Locale.ROOT)))
                        .collect(Collectors.toList()));

                if (filteredBalancesRef.get().size() > startIndex || fetchLimit >= totalPlayers) {
                    break;
                }

                fetchLimit = Math.min(totalPlayers, fetchLimit + entriesPerPage * 10);
            }

            List<PlayerBalance> filteredBalances = filteredBalancesRef.get();
            if (filteredBalances.isEmpty()) {
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(configManager.getMessage("baltop.empty")));
                return;
            }

            if (startIndex >= filteredBalances.size()) {
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(configManager.getMessage("baltop.invalid-page")));
                return;
            }

            int endIndex = Math.min(startIndex + entriesPerPage, filteredBalances.size());
            final List<PlayerBalance> displayedBalances = filteredBalances;
            final int finalStartIndex = startIndex;
            final int finalEndIndex = endIndex;
            final int finalDisplayPage = displayPage;

            // Render results on main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(configManager.getMessage("baltop.header",
                        "{page}", String.valueOf(finalDisplayPage),
                        "{max_page}", String.valueOf(maxPage)));

                for (int i = startIndex; i < endIndex; i++) {
                    PlayerBalance balance = filteredBalances.get(i);
                    int rank = i + 1;

                    sender.sendMessage(configManager.getMessage("baltop.entry",
                            "{rank}", String.valueOf(rank),
                            "{player}", balance.getUsername(),
                            "{balance}", formatBalance(balance.getBalance())));
                }

                sender.sendMessage(configManager.getMessage("baltop.footer"));
            });
        });

        return true;
    }

    private String abbreviateNumber(double number) {
        if (number >= 1_000_000) {
            return String.format("%.1fM", number / 1_000_000);
        } else if (number >= 1_000) {
            return String.format("%.1fK", number / 1_000);
        }
        return String.format("%.0f", number);
    }

    private String formatBalance(BigDecimal balance) {
        if (configManager.isAbbreviateEnabled()) {
            return abbreviateNumber(balance.doubleValue());
        }

        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance();
        String separator = configManager.getThousandsSeparator();
        if (separator != null && !separator.isEmpty()) {
            symbols.setGroupingSeparator(separator.charAt(0));
        }

        DecimalFormat df = new DecimalFormat("#,##0.00", symbols);
        df.setMaximumFractionDigits(configManager.getDecimalPlaces());
        df.setMinimumFractionDigits(configManager.getDecimalPlaces());
        return df.format(balance);
    }

    private String getCurrencySymbol() {
        String symbolType = configManager.getCurrencySymbol();

        return switch (symbolType.toUpperCase()) {
            case "DOLLAR" -> "$";
            case "EURO" -> "€";
            case "POUND" -> "£";
            case "YEN" -> "¥";
            case "WON" -> "₩";
            case "RUBLE" -> "₽";
            case "CENT" -> "¢";
            case "CUSTOM" -> configManager.getCustomSymbol();
            default -> "$";
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Suggest page numbers
            int totalPlayers = economyService.getTotalPlayerCount();
            int entriesPerPage = configManager.getBaltopEntriesPerPage();
            int maxPage = (totalPlayers + entriesPerPage - 1) / entriesPerPage;

            for (int i = 1; i <= maxPage && i <= 10; i++) {
                completions.add(String.valueOf(i));
            }
        }

        return completions;
    }
}
