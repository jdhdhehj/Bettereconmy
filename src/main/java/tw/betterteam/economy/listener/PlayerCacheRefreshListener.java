package tw.betterteam.economy.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import tw.betterteam.economy.BetterEconomy;
import tw.betterteam.economy.service.EconomyService;

public class PlayerCacheRefreshListener implements Listener {

    private final EconomyService economyService;

    public PlayerCacheRefreshListener(BetterEconomy plugin) {
        this.economyService = plugin.getEconomyService();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        economyService.refreshPlayerCache(event.getPlayer().getUniqueId());
    }
}
