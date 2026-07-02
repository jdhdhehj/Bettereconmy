package tw.betterteam.economy.command;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;

/**
 * Base class for economy commands
 */
public abstract class EconomyCommand implements CommandExecutor, TabCompleter {

    protected final JavaPlugin plugin;

    public EconomyCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Register this command
     */
    public abstract void register();

    /**
     * Get command name
     */
    protected abstract String getCommandName();
}
