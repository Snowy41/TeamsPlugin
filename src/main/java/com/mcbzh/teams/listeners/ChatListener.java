package com.mcbzh.teams.listeners;

import com.mcbzh.teams.TeamsPlugin;
import com.mcbzh.teams.models.Team;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {
    private final TeamsPlugin plugin;

    public ChatListener(TeamsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());

        // Check if player has team chat enabled
        if (plugin.getChatManager().isTeamChatEnabled(player.getUniqueId())) {
            event.setCancelled(true);

            // Send via team chat manager
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                plugin.getChatManager().sendTeamMessage(player, event.getMessage());
            });
            return;
        }

        // Normal chat with team prefix
        if (team != null) {
            // Format: [TAG] PlayerName: message
            String teamPrefix = team.getColoredTag() + " ";
            String format = event.getFormat();

            // Replace %1$s (player name) with team prefix + player name
            String newFormat = format.replace("%1$s", teamPrefix + "%1$s");
            event.setFormat(newFormat);
        }
    }
}