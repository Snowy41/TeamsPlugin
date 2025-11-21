package com.mcbzh.teams;

import com.mcbzh.teams.commands.TeamCommand;
import com.mcbzh.teams.listeners.*;
import com.mcbzh.teams.managers.TeamManager;
import com.mcbzh.teams.managers.NametagManager;
import com.mcbzh.teams.managers.TeamStashManager;
import com.mcbzh.teams.managers.TeamChatManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class TeamsPlugin extends JavaPlugin {
    private TeamManager teamManager;
    private NametagManager nametagManager;
    private TeamStashManager stashManager;
    private TeamChatManager chatManager;

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();

        // Initialize managers
        teamManager = new TeamManager(this);
        nametagManager = new NametagManager(this);
        stashManager = new TeamStashManager(this);
        chatManager = new TeamChatManager(this);

        // Register commands
        TeamCommand teamCommand = new TeamCommand(this);
        getCommand("team").setExecutor(teamCommand);
        getCommand("team").setTabCompleter(teamCommand);

        // Register /tc command
        getCommand("tc").setExecutor((sender, cmd, label, args) -> {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cThis command can only be used by players!");
                return true;
            }

            Player player = (Player) sender;

            // If no args, toggle team chat
            if (args.length == 0) {
                chatManager.toggleTeamChat(player);
                return true;
            }

            // Send as team message
            String message = String.join(" ", args);
            chatManager.sendTeamMessage(player, message);
            return true;
        });

        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new TeamCombatListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new StashListener(this), this);

        // Start auto-save task (every 5 minutes)
        int autoSaveInterval = getConfig().getInt("performance.auto-save-interval", 5) * 60 * 20;
        getServer().getScheduler().runTaskTimer(this, () -> {
            teamManager.saveTeams();
            stashManager.saveStashes();
            getLogger().info("Auto-saved team data and stashes");
        }, autoSaveInterval, autoSaveInterval);

        // Update all online players' nametags after a short delay
        getServer().getScheduler().runTaskLater(this, () -> {
            nametagManager.refreshAllScoreboards();
            getLogger().info("Refreshed all player nametags");
        }, 20L);

        getLogger().info("TeamsPlugin has been enabled!");
        getLogger().info("Loaded " + teamManager.getAllTeams().size() + " teams");
    }

    @Override
    public void onDisable() {
        // Save all team data before shutdown
        if (teamManager != null) {
            teamManager.saveTeams();
            getLogger().info("Saved all team data");
        }

        // Save all stashes
        if (stashManager != null) {
            stashManager.saveStashes();
            getLogger().info("Saved all team stashes");
        }

        // Clean up scoreboards
        if (nametagManager != null) {
            nametagManager.cleanupAllTeams();
        }

        getLogger().info("TeamsPlugin has been disabled!");
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }

    public NametagManager getNametagManager() {
        return nametagManager;
    }

    public TeamStashManager getStashManager() {
        return stashManager;
    }

    public TeamChatManager getChatManager() {
        return chatManager;
    }
}