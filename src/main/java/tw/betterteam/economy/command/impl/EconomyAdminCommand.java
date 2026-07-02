package tw.betterteam.economy.command.impl;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import tw.betterteam.economy.command.EconomyCommand;
import tw.betterteam.economy.config.ConfigManager;
import tw.betterteam.economy.model.TransactionLog;
import tw.betterteam.economy.service.EconomyService;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * /economy admin command implementation
 */
public class EconomyAdminCommand extends EconomyCommand {

    private final EconomyService economyService;
    private final ConfigManager configManager;
    
    // For wipe confirmation
    private final Map<String, Long> pendingConfirmations = new HashMap<>();
    private static final long CONFIRMATION_TIMEOUT = 30000; // 30 seconds

    public EconomyAdminCommand(JavaPlugin plugin, EconomyService economyService, ConfigManager configManager) {
        super(plugin);
        this.economyService = economyService;
        this.configManager = configManager;
    }

    @Override
    public void register() {
        plugin.getCommand("economy").setExecutor(this);
        plugin.getCommand("economy").setTabCompleter(this);
    }

    @Override
    protected String getCommandName() {
        return "economy";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "give":
                return handleGive(sender, args);
            case "take":
                return handleTake(sender, args);
            case "set":
                return handleSet(sender, args);
            case "wipe":
                return handleWipe(sender);
            case "confirm":
                return handleConfirm(sender);
            case "history":
                return handleHistory(sender, args);
            case "reload":
                return handleReload(sender);
            default:
                sendUsage(sender);
                return true;
        }
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("economy.admin.give")) {
            sender.sendMessage(configManager.getMessage("general.no-permission"));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(configManager.getMessage("general.invalid-amount"));
            return true;
        }

        String playerName = args[1];
        try {
            double amount = Double.parseDouble(args[2]);
            if (amount <= 0) {
                sender.sendMessage(configManager.getMessage("general.invalid-amount"));
                return true;
            }

            // Perform offline lookup and deposit asynchronously
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
                UUID uuid = player.getUniqueId();

                if (!player.hasPlayedBefore() && !player.isOnline()) {
                    Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(configManager.getMessage("general.player-not-found", "{player}", playerName)));
                    return;
                }

                boolean success = economyService.deposit(uuid, playerName, amount);
                BigDecimal newBalance = economyService.getBalance(uuid);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (success) {
                        sender.sendMessage(configManager.getMessage("admin.give-success",
                                "{player}", playerName,
                                "{amount}", String.format("%.2f", amount),
                                "{balance}", newBalance.toPlainString()));
                    } else {
                        sender.sendMessage(configManager.getMessage("admin.give-failed", "{player}", playerName));
                    }
                });
            });
        } catch (NumberFormatException e) {
            sender.sendMessage(configManager.getMessage("general.invalid-amount"));
        }

        return true;
    }

    private boolean handleTake(CommandSender sender, String[] args) {
        if (!sender.hasPermission("economy.admin.take")) {
            sender.sendMessage(configManager.getMessage("general.no-permission"));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(configManager.getMessage("general.invalid-amount"));
            return true;
        }

        String playerName = args[1];
        try {
            double amount = Double.parseDouble(args[2]);
            if (amount <= 0) {
                sender.sendMessage(configManager.getMessage("general.invalid-amount"));
                return true;
            }

            // Perform offline lookup and withdrawal asynchronously
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
                UUID uuid = player.getUniqueId();

                if (!player.hasPlayedBefore() && !player.isOnline()) {
                    Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(configManager.getMessage("general.player-not-found", "{player}", playerName)));
                    return;
                }

                if (!economyService.playerExists(uuid)) {
                    Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(configManager.getMessage("general.player-not-found", "{player}", playerName)));
                    return;
                }

                if (!economyService.has(uuid, amount)) {
                    Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(configManager.getMessage("admin.insufficient-balance", "{player}", playerName)));
                    return;
                }

                boolean success = economyService.withdraw(uuid, playerName, amount);
                BigDecimal newBalance = economyService.getBalance(uuid);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (success) {
                        sender.sendMessage(configManager.getMessage("admin.take-success",
                                "{player}", playerName,
                                "{amount}", String.format("%.2f", amount),
                                "{balance}", newBalance.toPlainString()));
                    } else {
                        sender.sendMessage(configManager.getMessage("admin.take-failed", "{player}", playerName));
                    }
                });
            });
        } catch (NumberFormatException e) {
            sender.sendMessage(configManager.getMessage("general.invalid-amount"));
        }

        return true;
    }

    private boolean handleSet(CommandSender sender, String[] args) {
        if (!sender.hasPermission("economy.admin.set")) {
            sender.sendMessage(configManager.getMessage("general.no-permission"));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(configManager.getMessage("general.invalid-amount"));
            return true;
        }

        String playerName = args[1];
        try {
            double amount = Double.parseDouble(args[2]);
            if (amount < 0 && !configManager.isNegativeBalanceAllowed()) {
                sender.sendMessage(configManager.getMessage("general.invalid-amount"));
                return true;
            }

            // Perform offline lookup and set balance asynchronously
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
                UUID uuid = player.getUniqueId();

                if (!player.hasPlayedBefore() && !player.isOnline()) {
                    Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(configManager.getMessage("general.player-not-found", "{player}", playerName)));
                    return;
                }

                boolean success = economyService.setBalance(uuid, playerName, BigDecimal.valueOf(amount));
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (success) {
                        sender.sendMessage(configManager.getMessage("admin.set-success",
                                "{player}", playerName,
                                "{balance}", String.format("%.2f", amount)));
                    } else {
                        sender.sendMessage(configManager.getMessage("admin.set-failed", "{player}", playerName));
                    }
                });
            });
        } catch (NumberFormatException e) {
            sender.sendMessage(configManager.getMessage("general.invalid-amount"));
        }

        return true;
    }

    private boolean handleWipe(CommandSender sender) {
        if (!sender.hasPermission("economy.admin.wipe")) {
            sender.sendMessage(configManager.getMessage("general.no-permission"));
            return true;
        }

        sender.sendMessage(configManager.getMessage("admin.wipe-warning"));
        pendingConfirmations.put(sender.getName(), System.currentTimeMillis());

        return true;
    }

    private boolean handleConfirm(CommandSender sender) {
        if (!sender.hasPermission("economy.admin.wipe")) {
            sender.sendMessage(configManager.getMessage("general.no-permission"));
            return true;
        }

        Long pendingTime = pendingConfirmations.get(sender.getName());
        if (pendingTime == null) {
            sender.sendMessage(configManager.getMessage("admin.wipe-no-pending"));
            return true;
        }

        if (System.currentTimeMillis() - pendingTime > CONFIRMATION_TIMEOUT) {
            pendingConfirmations.remove(sender.getName());
            sender.sendMessage(configManager.getMessage("admin.wipe-timeout"));
            return true;
        }

        // Perform wipe
        economyService.wipeAllBalances();
        pendingConfirmations.remove(sender.getName());
        sender.sendMessage(configManager.getMessage("admin.wipe-confirmed"));

        return true;
    }

    private boolean handleHistory(CommandSender sender, String[] args) {
        if (!sender.hasPermission("economy.admin.history")) {
            sender.sendMessage(configManager.getMessage("general.no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(configManager.getMessage("general.player-not-found", "{player}", ""));
            return true;
        }

        String playerName = args[1];
        int page = 1;

        if (args.length > 2) {
            try {
                page = Integer.parseInt(args[2]);
                if (page < 1) page = 1;
            } catch (NumberFormatException e) {
                // Default to 1
            }
        }

        // Perform history retrieval asynchronously
        final int requestedPage = page;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
            UUID uuid = player.getUniqueId();

            if (!economyService.playerExists(uuid)) {
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(configManager.getMessage("general.player-not-found", "{player}", playerName)));
                return;
            }

            int pageSize = configManager.getHistoryEntriesPerPage();
            List<TransactionLog> history = economyService.getTransactionHistory(uuid, requestedPage, pageSize);

            if (history.isEmpty()) {
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(configManager.getMessage("admin.history-empty", "{player}", playerName)));
                return;
            }

            int totalCount = economyService.getTransactionCount(uuid);
            int maxPage = (totalCount + pageSize - 1) / pageSize;

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(configManager.getMessage("admin.history-header",
                        "{player}", playerName,
                        "{page}", String.valueOf(requestedPage),
                        "{max_page}", String.valueOf(maxPage)));

                for (TransactionLog log : history) {
                    String typeDisplay = configManager.getMessage("log-types." + log.getType().name());
                    sender.sendMessage(configManager.getMessage("admin.history-entry",
                            "{time}", sdf.format(new Date(log.getCreatedAt())),
                            "{type}", typeDisplay,
                            "{amount}", log.getAmount().toPlainString(),
                            "{balance}", log.getBalanceAfter().toPlainString(),
                            "{operator}", log.getOperator()));
                }
            });
        });

        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("economy.admin.reload")) {
            sender.sendMessage(configManager.getMessage("general.no-permission"));
            return true;
        }

        sender.sendMessage(configManager.getMessage("admin.reload-started"));
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                configManager.reloadConfig();
                sender.sendMessage(configManager.getMessage("admin.reload-completed"));
            } catch (Exception e) {
                sender.sendMessage(configManager.getMessage("general.error-occurred", "{error}", e.getMessage()));
            }
        });

        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(configManager.getMessage("commands.economy.header"));
        sender.sendMessage(configManager.getMessage("commands.economy.give"));
        sender.sendMessage(configManager.getMessage("commands.economy.take"));
        sender.sendMessage(configManager.getMessage("commands.economy.set"));
        sender.sendMessage(configManager.getMessage("commands.economy.wipe"));
        sender.sendMessage(configManager.getMessage("commands.economy.confirm"));
        sender.sendMessage(configManager.getMessage("commands.economy.history"));
        sender.sendMessage(configManager.getMessage("commands.economy.reload"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String[] subCommands = {"give", "take", "set", "wipe", "confirm", "history", "reload"};
            String prefix = args[0].toLowerCase();
            for (String sub : subCommands) {
                if (sub.startsWith(prefix)) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("give") || 
                                         args[0].equalsIgnoreCase("take") ||
                                         args[0].equalsIgnoreCase("set") ||
                                         args[0].equalsIgnoreCase("history"))) {
            // Add player names
            for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                String name = player.getName();
                if (name != null && name.toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(name);
                }
            }
        } else if (args.length == 3 && (args[0].equalsIgnoreCase("give") || 
                                         args[0].equalsIgnoreCase("take") ||
                                         args[0].equalsIgnoreCase("set"))) {
            // Suggest amounts
            completions.add("100");
            completions.add("1000");
            completions.add("10000");
        }

        return completions;
    }
}
