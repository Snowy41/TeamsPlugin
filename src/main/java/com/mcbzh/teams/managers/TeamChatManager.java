package com.mcbzh.teams.managers;

import com.mcbzh.teams.TeamsPlugin;
import com.mcbzh.teams.models.Team;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TeamChatManager {
    private final TeamsPlugin plugin;
    private final TeamManager teamManager;
    private final Set<UUID> teamChatEnabled;

    public TeamChatManager(TeamsPlugin plugin) {
        this.plugin = plugin;
        this.teamManager = plugin.getTeamManager();
        this.teamChatEnabled = new HashSet<>();
    }

    /**
     * Toggle team chat mode for a player
     */
    public void toggleTeamChat(Player player) {
        UUID playerId = player.getUniqueId();

        if (teamChatEnabled.contains(playerId)) {
            teamChatEnabled.remove(playerId);
            player.sendMessage(ChatColor.YELLOW + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            player.sendMessage(ChatColor.RED + "✖ Team Chat Disabled");
            player.sendMessage(ChatColor.GRAY + "Your messages are now visible to everyone");
            player.sendMessage(ChatColor.YELLOW + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        } else {
            teamChatEnabled.add(playerId);
            player.sendMessage(ChatColor.YELLOW + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            player.sendMessage(ChatColor.GREEN + "✔ Team Chat Enabled");
            player.sendMessage(ChatColor.GRAY + "Your messages will only be visible to your team");
            player.sendMessage(ChatColor.GRAY + "Use " + ChatColor.WHITE + "/tc" + ChatColor.GRAY + " to toggle back");
            player.sendMessage(ChatColor.YELLOW + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        }
    }

    /**
     * Send a message to team chat
     */
    public void sendTeamMessage(Player sender, String message) {
        Team team = teamManager.getPlayerTeam(sender.getUniqueId());

        if (team == null) {
            sender.sendMessage(ChatColor.RED + "You are not in a team!");
            return;
        }

        // Format: [TC] [TAG] PlayerName: message
        String formattedMessage = ChatColor.AQUA + "[TC] " +
                team.getColoredTag() + " " +
                ChatColor.WHITE + sender.getName() +
                ChatColor.GRAY + ": " +
                ChatColor.WHITE + message;

        // Send to all online team members
        for (UUID memberId : team.getMembers()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && member.isOnline()) {
                member.sendMessage(formattedMessage);
            }
        }

        // Log to console
        plugin.getLogger().info("[Team Chat] " + team.getName() + " | " +
                sender.getName() + ": " + message);
    }

    /**
     * Check if player has team chat enabled
     */
    public boolean isTeamChatEnabled(UUID playerId) {
        return teamChatEnabled.contains(playerId);
    }

    /**
     * Remove player from team chat mode (on quit)
     */
    public void removePlayer(UUID playerId) {
        teamChatEnabled.remove(playerId);
    }
}