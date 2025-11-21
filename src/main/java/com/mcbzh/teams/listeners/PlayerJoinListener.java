package com.mcbzh.teams.listeners;

import com.mcbzh.teams.TeamsPlugin;
import com.mcbzh.teams.models.Team;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final TeamsPlugin plugin;

    public PlayerJoinListener(TeamsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());

        if (team != null) {
            // Re-add player to scoreboard team (in case they logged off)
            plugin.getTeamManager().addPlayerToTeam(player.getUniqueId(), team);
        }
        
        // Update nametag after a short delay to ensure player is fully loaded
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (plugin.getNametagManager() != null) {
                plugin.getNametagManager().updatePlayer(player);
            }
        }, 10L);
    }
}
