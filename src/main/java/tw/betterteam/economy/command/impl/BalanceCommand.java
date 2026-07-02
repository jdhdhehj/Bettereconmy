package tw.betterteam.economy.command.impl;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import tw.betterteam.economy.command.EconomyCommand;
import tw.betterteam.economy.config.ConfigManager;
import tw.betterteam.economy.service.EconomyService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * /balance command implementation
 */
public class BalanceCommand extends EconomyCommand {

    private final EconomyService economyService;
    private final ConfigManager configManager;

    public BalanceCommand(JavaPlugin plugin, EconomyService economyService, ConfigManager configManager) {
        super(plugin);
        this.economyService = economyService;
        this.configManager = configManager;
    }

    public BalanceCommand(JavaPlugin plugin, EconomyService economyService) {
        super(plugin);
        this.economyService = economyService;
        this.configManager = ((tw.betterteam.economy.BetterEconomy) plugin).getConfigManager();
    }

    @Override
    public void register() {
        plugin.getCommand("balance").setExecutor(this);
        plugin.getCommand("balance").setTabCompleter(this);
    }

    @Override
    protected String getCommandName() {
        return "balance";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            // Check own balance
            if (!(sender instanceof Player)) {
                sender.sendMessage(configManager.getMessage("general.player-not-found", "{player}", "Console"));
                return true;
            }

            if (!sender.hasPermission("economy.balance.self")) {
                sender.sendMessage(configManager.getMessage("general.no-permission"));
                return true;
            }

            Player player = (Player) sender;
            UUID uuid = player.getUniqueId();
            String balance = economyService.getBalance(uuid).toPlainString();

            sender.sendMessage(configManager.getMessage("balance.self", "{balance}", balance));
            return true;
        }

        // Check other player's balance
        if (!sender.hasPermission("economy.balance.others")) {
            sender.sendMessage(configManager.getMessage("general.no-permission"));
            return true;
        }

        String playerName = args[0];

        // Perform offline lookup and balance retrieval asynchronously
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
            UUID uuid = target.getUniqueId();

            if (!economyService.playerExists(uuid)) {
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(configManager.getMessage("general.player-not-found", "{player}", playerName)));
                return;
            }

            String balance = economyService.getBalance(uuid).toPlainString();
            Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(configManager.getMessage("balance.others",
                    "{player}", playerName,
                    "{balance}", balance)));
        });

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                String name = player.getName();
                if (name != null && name.toLowerCase().startsWith(prefix)) {
                    completions.add(name);
                }
            }
        }

        return completions;
    }
}
