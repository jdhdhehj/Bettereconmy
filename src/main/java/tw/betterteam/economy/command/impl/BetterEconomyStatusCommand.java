package tw.betterteam.economy.command.impl;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;
import tw.betterteam.economy.BetterEconomy;
import tw.betterteam.economy.config.ConfigManager;

import java.util.Collections;
import java.util.List;

public class BetterEconomyStatusCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;

    public BetterEconomyStatusCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configManager = ((BetterEconomy) plugin).getConfigManager();
    }

    public void register() {
        if (plugin.getCommand("bettereconmy") != null) {
            plugin.getCommand("bettereconmy").setExecutor(this);
            plugin.getCommand("bettereconmy").setTabCompleter(this);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && !args[0].equalsIgnoreCase("status")) {
            sender.sendMessage(configManager.getMessage("commands.status.usage"));
            return true;
        }

        BetterEconomy betterEconomy = BetterEconomy.getInstance();
        String vaultStatus = betterEconomy != null && betterEconomy.isVaultRegistered() ? "§a已註冊並連結" : "§c未註冊";
        String placeholderStatus = betterEconomy != null && betterEconomy.isPlaceholderAPIRegistered() ? "§a已註冊並連結" : "§c未註冊";

        sender.sendMessage(configManager.getMessage("commands.status.header"));
        sender.sendMessage(configManager.getMessage("commands.status.plugin"));
        sender.sendMessage(configManager.getMessage("commands.status.vault", "{status}", vaultStatus));
        sender.sendMessage(configManager.getMessage("commands.status.placeholderapi", "{status}", placeholderStatus));
        sender.sendMessage(configManager.getMessage("commands.status.database"));
        sender.sendMessage(configManager.getMessage("commands.status.commands"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
