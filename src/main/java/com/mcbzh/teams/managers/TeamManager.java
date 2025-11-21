package com.mcbzh.teams.managers;

import com.mcbzh.teams.TeamsPlugin;
import com.mcbzh.teams.models.Team;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class TeamManager {
    private final TeamsPlugin plugin;
    private final Map<UUID, Team> teams;
    private final Map<UUID, UUID> playerTeams;
    private File teamsFile;
    private FileConfiguration teamsConfig;

    public TeamManager(TeamsPlugin plugin) {
        this.plugin = plugin;
        this.teams = new HashMap<>();
        this.playerTeams = new HashMap<>();
        setupTeamsFile();
        loadTeams();
    }

    private void setupTeamsFile() {
        teamsFile = new File(plugin.getDataFolder(), "teams.yml");
        if (!teamsFile.exists()) {
            try {
                teamsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create teams.yml!");
                e.printStackTrace();
            }
        }
        teamsConfig = YamlConfiguration.loadConfiguration(teamsFile);
    }

    public Team createTeam(String name, Player leader) {
        if (getPlayerTeam(leader.getUniqueId()) != null) {
            return null;
        }

        if (getTeamByName(name) != null) {
            return null;
        }

        Team team = new Team(name, leader.getUniqueId());
        teams.put(team.getId(), team);
        playerTeams.put(leader.getUniqueId(), team.getId());

        saveTeams();

        return team;
    }

    public boolean deleteTeam(UUID teamId) {
        Team team = teams.get(teamId);
        if (team == null) {
            return false;
        }

        // Remove all player associations
        for (UUID member : team.getMembers()) {
            playerTeams.remove(member);
        }

        teams.remove(teamId);

        // Remove team stash
        if (plugin.getStashManager() != null) {
            plugin.getStashManager().removeStash(teamId);
        }

        saveTeams();

        return true;
    }

    public Team getTeam(UUID teamId) {
        return teams.get(teamId);
    }

    public Team getTeamByName(String name) {
        return teams.values().stream()
                .filter(t -> t.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public Team getPlayerTeam(UUID playerId) {
        UUID teamId = playerTeams.get(playerId);
        return teamId != null ? teams.get(teamId) : null;
    }

    public boolean addPlayerToTeam(UUID playerId, Team team) {
        if (getPlayerTeam(playerId) != null) {
            return false;
        }

        if (team.isFull()) {
            return false;
        }

        if (team.addMember(playerId)) {
            playerTeams.put(playerId, team.getId());
            saveTeams();
            return true;
        }

        return false;
    }

    public boolean removePlayerFromTeam(UUID playerId, Team team) {
        if (!team.isMember(playerId)) {
            return false;
        }

        if (team.isLeader(playerId)) {
            Set<UUID> members = team.getMembers();
            if (members.size() > 1) {
                UUID newLeader = members.stream()
                        .filter(uuid -> !uuid.equals(playerId))
                        .findFirst()
                        .orElse(null);

                if (newLeader != null) {
                    team.setLeader(newLeader);
                }
            } else {
                deleteTeam(team.getId());
                return true;
            }
        }

        team.removeMember(playerId);
        playerTeams.remove(playerId);
        saveTeams();

        return true;
    }

    public List<Team> getAllTeams() {
        return new ArrayList<>(teams.values());
    }

    public List<Team> getTopTeamsByKills(int limit) {
        return teams.values().stream()
                .sorted(Comparator.comparingInt(Team::getTotalKills).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<Team> getTopTeamsByKD(int limit) {
        return teams.values().stream()
                .sorted(Comparator.comparingDouble(Team::getKDRatio).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public boolean areTeammates(UUID player1, UUID player2) {
        Team team1 = getPlayerTeam(player1);
        Team team2 = getPlayerTeam(player2);

        return team1 != null && team2 != null && team1.equals(team2);
    }

    public void saveTeams() {
        ConfigurationSection teamsSection = teamsConfig.createSection("teams");

        for (Team team : teams.values()) {
            ConfigurationSection teamSection = teamsSection.createSection(team.getId().toString());
            Map<String, Object> data = team.serialize();

            for (Map.Entry<String, Object> entry : data.entrySet()) {
                teamSection.set(entry.getKey(), entry.getValue());
            }
        }

        try {
            teamsConfig.save(teamsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save teams.yml!");
            e.printStackTrace();
        }
    }

    public void loadTeams() {
        if (!teamsConfig.contains("teams")) {
            return;
        }

        ConfigurationSection teamsSection = teamsConfig.getConfigurationSection("teams");
        if (teamsSection == null) {
            return;
        }

        for (String teamIdStr : teamsSection.getKeys(false)) {
            ConfigurationSection teamSection = teamsSection.getConfigurationSection(teamIdStr);
            if (teamSection == null) continue;

            Map<String, Object> data = new HashMap<>();
            for (String key : teamSection.getKeys(false)) {
                data.put(key, teamSection.get(key));
            }

            try {
                Team team = Team.deserialize(data);
                teams.put(team.getId(), team);

                for (UUID member : team.getMembers()) {
                    playerTeams.put(member, team.getId());
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load team: " + teamIdStr);
                e.printStackTrace();
            }
        }

        plugin.getLogger().info("Loaded " + teams.size() + " teams");
    }

    public void broadcastToTeam(Team team, String message) {
        for (UUID memberId : team.getMembers()) {
            Player player = Bukkit.getPlayer(memberId);
            if (player != null && player.isOnline()) {
                player.sendMessage(message);
            }
        }
    }

    public String formatTeamInfo(Team team) {
        StringBuilder sb = new StringBuilder();
        sb.append(ChatColor.GOLD).append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append(ChatColor.YELLOW).append("Team: ").append(team.getColoredName()).append("\n");
        sb.append(ChatColor.GRAY).append("Description: ").append(ChatColor.WHITE).append(team.getDescription()).append("\n");
        sb.append(ChatColor.GRAY).append("Leader: ").append(ChatColor.WHITE);

        Player leader = Bukkit.getPlayer(team.getLeader());
        sb.append(leader != null ? leader.getName() : "Unknown").append("\n");

        sb.append(ChatColor.GRAY).append("Members: ").append(ChatColor.WHITE)
                .append(team.getMemberCount()).append("/").append(team.getMaxMembers()).append("\n");

        sb.append(ChatColor.GRAY).append("Statistics:\n");
        sb.append(ChatColor.WHITE).append("  Kills: ").append(ChatColor.GREEN).append(team.getTotalKills()).append("\n");
        sb.append(ChatColor.WHITE).append("  Deaths: ").append(ChatColor.RED).append(team.getTotalDeaths()).append("\n");
        sb.append(ChatColor.WHITE).append("  K/D Ratio: ").append(ChatColor.AQUA)
                .append(String.format("%.2f", team.getKDRatio())).append("\n");

        sb.append(ChatColor.GRAY).append("Settings:\n");
        sb.append(ChatColor.WHITE).append("  Friendly Fire: ")
                .append(team.isFriendlyFire() ? ChatColor.RED + "Enabled" : ChatColor.GREEN + "Disabled").append("\n");

        sb.append(ChatColor.GOLD).append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        return sb.toString();
    }
}