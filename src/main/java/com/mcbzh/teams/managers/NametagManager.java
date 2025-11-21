package com.mcbzh.teams.managers;

import com.mcbzh.teams.TeamsPlugin;
import com.mcbzh.teams.models.Team;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NametagManager {
    private final TeamsPlugin plugin;
    private final TeamManager teamManager;
    private final Map<String, org.bukkit.scoreboard.Team> teamScoreboards;
    private Scoreboard mainScoreboard;

    // For triggering CustomScoreboard updates
    private Plugin scoreboardPlugin;
    private Method updateScoreboardMethod;

    public NametagManager(TeamsPlugin plugin) {
        this.plugin = plugin;
        this.teamManager = plugin.getTeamManager();
        this.teamScoreboards = new HashMap<>();

        // Get or create main scoreboard
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            this.mainScoreboard = manager.getMainScoreboard();
            plugin.getLogger().info("NametagManager initialized successfully");
        } else {
            plugin.getLogger().severe("Failed to initialize NametagManager - ScoreboardManager is null!");
        }

        // Try to hook into ScoreboardPlugin to trigger updates
        setupScoreboardPluginHook();
    }

    private void setupScoreboardPluginHook() {
        scoreboardPlugin = Bukkit.getPluginManager().getPlugin("CustomScoreboard");
        if (scoreboardPlugin != null) {
            try {
                Class<?> scoreboardPluginClass = scoreboardPlugin.getClass();
                updateScoreboardMethod = scoreboardPluginClass.getMethod("updateScoreboard", Player.class);
                plugin.getLogger().info("Successfully hooked into CustomScoreboard for updates!");
            } catch (Exception e) {
                plugin.getLogger().warning("Could not hook into CustomScoreboard: " + e.getMessage());
            }
        }
    }

    /**
     * Trigger CustomScoreboard update for a player
     * This ensures they see the updated teams immediately
     */
    private void triggerScoreboardUpdate(Player player) {
        if (scoreboardPlugin != null && updateScoreboardMethod != null) {
            try {
                updateScoreboardMethod.invoke(scoreboardPlugin, player);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to trigger scoreboard update: " + e.getMessage());
            }
        }
    }

    /**
     * Update a player's nametag (above their head in-game AND in tablist via scoreboard team)
     */
    public void updatePlayer(Player player) {
        if (mainScoreboard == null) {
            plugin.getLogger().warning("Cannot update player " + player.getName() + " - scoreboard is null");
            return;
        }

        // Remove player from any existing scoreboard team
        removePlayerFromAllTeams(player);

        Team team = teamManager.getPlayerTeam(player.getUniqueId());

        if (team != null) {
            // Add to team with colored prefix (for nametag above head AND tablist)
            addToScoreboardTeam(player, team);
        }

        // Trigger CustomScoreboard update so they see changes immediately
        triggerScoreboardUpdate(player);

        plugin.getLogger().info("Updated nametag for " + player.getName());
    }

    /**
     * Update all players' nametags
     */
    public void updateAllPlayers() {
        plugin.getLogger().info("Updating all player nametags...");
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayer(player);
        }
    }

    /**
     * Update a specific team's display for all members
     */
    public void updateTeam(Team team) {
        if (mainScoreboard == null) return;

        // Update the scoreboard team settings
        String teamName = getScoreboardTeamName(team);
        org.bukkit.scoreboard.Team scoreboardTeam = mainScoreboard.getTeam(teamName);

        if (scoreboardTeam != null) {
            updateScoreboardTeamSettings(scoreboardTeam, team);
        }

        // Update all members
        for (UUID memberId : team.getMembers()) {
            Player player = Bukkit.getPlayer(memberId);
            if (player != null && player.isOnline()) {
                updatePlayer(player);
            }
        }

        // Trigger updates for ALL online players so they see the changes
        for (Player player : Bukkit.getOnlinePlayers()) {
            triggerScoreboardUpdate(player);
        }
    }

    /**
     * Add a player to a scoreboard team with team prefix
     * This affects BOTH the nametag above their head AND tablist prefix
     */
    private void addToScoreboardTeam(Player player, Team team) {
        if (mainScoreboard == null) return;

        // Get or create scoreboard team
        String teamName = getScoreboardTeamName(team);
        org.bukkit.scoreboard.Team scoreboardTeam = teamScoreboards.get(teamName);

        if (scoreboardTeam == null) {
            scoreboardTeam = mainScoreboard.getTeam(teamName);

            if (scoreboardTeam == null) {
                scoreboardTeam = mainScoreboard.registerNewTeam(teamName);
                plugin.getLogger().info("Created new scoreboard team: " + teamName);
            }

            teamScoreboards.put(teamName, scoreboardTeam);
        }

        // Update team settings
        updateScoreboardTeamSettings(scoreboardTeam, team);

        // Add player to team
        if (!scoreboardTeam.hasEntry(player.getName())) {
            scoreboardTeam.addEntry(player.getName());
            plugin.getLogger().info("Added " + player.getName() + " to scoreboard team: " + teamName);
        }
    }

    /**
     * Update scoreboard team settings (prefix, color, etc.)
     * NOTE: Don't set suffix here - TabListPlugin handles suffix for deaths/AFK
     */
    private void updateScoreboardTeamSettings(org.bukkit.scoreboard.Team scoreboardTeam, Team team) {
        // Create a safe prefix (max 16 chars including color codes)
        String prefix = createSafePrefix(team);

        scoreboardTeam.setPrefix(prefix);
        scoreboardTeam.setColor(team.getColor());

        // DON'T set suffix here - TabListPlugin will handle it
        // scoreboardTeam.setSuffix(...);

        // Set options
        scoreboardTeam.setAllowFriendlyFire(team.isFriendlyFire());
        scoreboardTeam.setCanSeeFriendlyInvisibles(true);

        // Set name tag visibility
        try {
            scoreboardTeam.setOption(org.bukkit.scoreboard.Team.Option.NAME_TAG_VISIBILITY,
                    org.bukkit.scoreboard.Team.OptionStatus.ALWAYS);
        } catch (Exception e) {
            // Older version compatibility - ignore
        }

        plugin.getLogger().info("Updated scoreboard team settings for: " + scoreboardTeam.getName() + " with prefix: " + prefix);
    }

    /**
     * Create a safe prefix that fits within Minecraft's 16-character limit
     * Format: §c[TAG]§r  (with space)
     */
    private String createSafePrefix(Team team) {
        String colorCode = team.getColor().toString(); // 2 chars (e.g., "§c")
        String resetCode = ChatColor.RESET.toString(); // 2 chars (§r)

        // Use tag if set, otherwise use display name
        String text = team.getTag();
        if (text == null || text.isEmpty()) {
            text = team.getDisplayName();
        }

        // Format: §c[TEXT]§r
        // Total: 2 + 1 + text + 1 + 2 + 1 = 7 + text length
        // Maximum text length: 16 - 7 = 9 characters

        int maxTextLength = 9;
        if (text.length() > maxTextLength) {
            text = text.substring(0, maxTextLength);
        }

        String prefix = colorCode + "[" + text + "]" + resetCode + " ";

        // Double check length (shouldn't exceed 16)
        if (prefix.length() > 16) {
            // Fallback: just use color code + truncated text
            text = text.substring(0, Math.min(text.length(), 13));
            prefix = colorCode + text + resetCode + " ";
        }

        return prefix;
    }

    /**
     * Remove a player from all scoreboard teams
     */
    private void removePlayerFromAllTeams(Player player) {
        if (mainScoreboard == null) return;

        // Check all scoreboard teams
        for (org.bukkit.scoreboard.Team team : mainScoreboard.getTeams()) {
            if (team.hasEntry(player.getName())) {
                team.removeEntry(player.getName());
                plugin.getLogger().info("Removed " + player.getName() + " from scoreboard team: " + team.getName());
            }
        }
    }

    /**
     * Get a consistent scoreboard team name for a team
     * Use team name instead of UUID for consistency
     */
    private String getScoreboardTeamName(Team team) {
        // Use sanitized team name (max 16 chars for scoreboard team name)
        String name = team.getName().replaceAll("[^a-zA-Z0-9]", "").toLowerCase();

        if (name.length() > 16) {
            name = name.substring(0, 16);
        }

        // Prefix with color ordinal to ensure sorting in tablist
        String colorPrefix = String.format("%02d", team.getColor().ordinal());

        // Total max 16 chars: 2 (color) + 1 (underscore) + 13 (name)
        if (name.length() > 13) {
            name = name.substring(0, 13);
        }

        return colorPrefix + "_" + name;
    }

    /**
     * Clean up a team's scoreboard team
     */
    public void removeTeamScoreboard(Team team) {
        if (mainScoreboard == null) return;

        String teamName = getScoreboardTeamName(team);
        org.bukkit.scoreboard.Team scoreboardTeam = mainScoreboard.getTeam(teamName);

        if (scoreboardTeam != null) {
            scoreboardTeam.unregister();
            teamScoreboards.remove(teamName);
            plugin.getLogger().info("Removed scoreboard team: " + teamName);
        }

        // Update all players' scoreboards
        for (Player player : Bukkit.getOnlinePlayers()) {
            triggerScoreboardUpdate(player);
        }
    }

    /**
     * Clean up all team scoreboards
     */
    public void cleanupAllTeams() {
        if (mainScoreboard == null) return;

        plugin.getLogger().info("Cleaning up all team scoreboards...");

        // Remove all tracked scoreboard teams
        for (org.bukkit.scoreboard.Team team : teamScoreboards.values()) {
            try {
                team.unregister();
            } catch (Exception e) {
                // Team might already be unregistered
            }
        }

        teamScoreboards.clear();
        plugin.getLogger().info("Cleanup complete");
    }

    /**
     * Refresh all scoreboards (useful after server reload)
     */
    public void refreshAllScoreboards() {
        if (mainScoreboard == null) {
            plugin.getLogger().warning("Cannot refresh scoreboards - mainScoreboard is null");
            return;
        }

        plugin.getLogger().info("Refreshing all scoreboards...");

        // Clean up existing teams
        cleanupAllTeams();

        // Recreate scoreboards for all online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayer(player);
        }

        plugin.getLogger().info("Scoreboard refresh complete");
    }
}