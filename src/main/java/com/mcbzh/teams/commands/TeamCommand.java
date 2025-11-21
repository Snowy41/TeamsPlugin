package com.mcbzh.teams.commands;

import com.mcbzh.teams.TeamsPlugin;
import com.mcbzh.teams.gui.TeamListGUI;
import com.mcbzh.teams.gui.TeamManageGUI;
import com.mcbzh.teams.managers.TeamManager;
import com.mcbzh.teams.models.Team;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class TeamCommand implements CommandExecutor, TabCompleter {
    private final TeamsPlugin plugin;
    private final TeamManager teamManager;
    private final TeamListGUI teamListGUI;
    private final TeamManageGUI teamManageGUI;

    public TeamCommand(TeamsPlugin plugin) {
        this.plugin = plugin;
        this.teamManager = plugin.getTeamManager();
        this.teamListGUI = new TeamListGUI(plugin);
        this.teamManageGUI = new TeamManageGUI(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelpMessage(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create":
                return handleCreate(player, args);
            case "disband":
                return handleDisband(player);
            case "invite":
                return handleInvite(player, args);
            case "join":
                return handleJoin(player, args);
            case "leave":
                return handleLeave(player);
            case "kick":
                return handleKick(player, args);
            case "promote":
                return handlePromote(player, args);
            case "demote":
                return handleDemote(player, args);
            case "list":
                teamListGUI.openTeamList(player, 1);
                return true;
            case "info":
                return handleInfo(player, args);
            case "manage":
                teamManageGUI.openTeamManageMenu(player);
                return true;
            case "chat":
            case "c":
                return handleChat(player);
            case "stash":
                return handleStash(player);
            case "top":
                return handleTop(player, args);
            case "color":
                return handleColor(player, args);
            case "tag":
                return handleTag(player, args);
            case "description":
            case "desc":
                return handleDescription(player, args);
            case "transfer":
                return handleTransfer(player, args);
            case "ally":
                return handleAlly(player, args);
            case "allyaccept":
            case "acceptally":
                return handleAllyAccept(player, args);
            case "allyremove":
            case "removeally":
                return handleAllyRemove(player, args);
            case "allylist":
                return handleAllyList(player);
            case "allypermissions":
            case "allyperm":
                return handleAllyPermissions(player);
            case "help":
            default:
                sendHelpMessage(player);
                return true;
        }
    }

    private boolean handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /team create <name>");
            return true;
        }

        Team existingTeam = teamManager.getPlayerTeam(player.getUniqueId());
        if (existingTeam != null) {
            player.sendMessage(ChatColor.RED + "You are already in a team! Leave it first.");
            return true;
        }

        String teamName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        if (teamName.length() > 16) {
            player.sendMessage(ChatColor.RED + "Team name must be 16 characters or less!");
            return true;
        }

        Team team = teamManager.createTeam(teamName, player);

        if (team == null) {
            player.sendMessage(ChatColor.RED + "A team with that name already exists!");
            return true;
        }

        // Update nametag
        if (plugin.getNametagManager() != null) {
            plugin.getNametagManager().updatePlayer(player);
        }

        player.sendMessage(ChatColor.GREEN + "Successfully created team: " + team.getColoredName());
        player.sendMessage(ChatColor.YELLOW + "Use " + ChatColor.WHITE + "/team manage" +
                ChatColor.YELLOW + " to configure your team!");

        return true;
    }


    // Add these new methods to handle ally commands:

    private boolean handleAlly(Player player, String[] args) {
        Team team = teamManager.getPlayerTeam(player.getUniqueId());

        if (team == null) {
            player.sendMessage(ChatColor.RED + "You are not in a team!");
            return true;
        }

        if (!team.isLeader(player.getUniqueId()) && !team.isModerator(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Only team leaders and moderators can manage allies!");
            return true;
        }

        if (!team.isAllowAlliances()) {
            player.sendMessage(ChatColor.RED + "Your team doesn't allow alliances!");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /team ally <team name>");
            return true;
        }

        String targetTeamName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        Team targetTeam = teamManager.getTeamByName(targetTeamName);

        if (targetTeam == null) {
            player.sendMessage(ChatColor.RED + "Team not found!");
            return true;
        }

        if (targetTeam.equals(team)) {
            player.sendMessage(ChatColor.RED + "You cannot ally with your own team!");
            return true;
        }

        if (team.isAlly(targetTeam.getId())) {
            player.sendMessage(ChatColor.RED + "You are already allies with " + targetTeam.getColoredName() + "!");
            return true;
        }

        if (!targetTeam.isAllowAlliances()) {
            player.sendMessage(ChatColor.RED + "That team doesn't allow alliances!");
            return true;
        }

        // Send ally invitation
        team.sendAllyInvite(targetTeam.getId());
        targetTeam.sendAllyInvite(team.getId()); // Store invite on both sides

        player.sendMessage(ChatColor.GREEN + "Sent ally request to " + targetTeam.getColoredName() + "!");

        // Notify target team leaders and moderators
        teamManager.broadcastToTeamLeadership(targetTeam,
                ChatColor.GOLD + "╔═══════════════════════════════╗");
        teamManager.broadcastToTeamLeadership(targetTeam,
                ChatColor.YELLOW + team.getColoredName() + " wants to be allies!");
        teamManager.broadcastToTeamLeadership(targetTeam,
                ChatColor.GRAY + "Use " + ChatColor.WHITE + "/team allyaccept " + team.getName() +
                        ChatColor.GRAY + " to accept");
        teamManager.broadcastToTeamLeadership(targetTeam,
                ChatColor.GRAY + "Request expires in 5 minutes");
        teamManager.broadcastToTeamLeadership(targetTeam,
                ChatColor.GOLD + "╚═══════════════════════════════╝");

        return true;
    }

    private boolean handleAllyAccept(Player player, String[] args) {
        Team team = teamManager.getPlayerTeam(player.getUniqueId());

        if (team == null) {
            player.sendMessage(ChatColor.RED + "You are not in a team!");
            return true;
        }

        if (!team.isLeader(player.getUniqueId()) && !team.isModerator(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Only team leaders and moderators can accept ally requests!");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /team allyaccept <team name>");
            return true;
        }

        String targetTeamName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        Team targetTeam = teamManager.getTeamByName(targetTeamName);

        if (targetTeam == null) {
            player.sendMessage(ChatColor.RED + "Team not found!");
            return true;
        }

        if (!targetTeam.hasAllyInvite(team.getId())) {
            player.sendMessage(ChatColor.RED + "You don't have an ally request from " +
                    targetTeam.getColoredName() + "!");
            return true;
        }

        // Form alliance (mutual)
        team.addAlly(targetTeam.getId());
        targetTeam.addAlly(team.getId());

        teamManager.saveTeams();

        // Notify both teams
        teamManager.broadcastToTeam(team,
                ChatColor.GREEN + "✓ You are now allies with " + targetTeam.getColoredName() + "!");
        teamManager.broadcastToTeam(targetTeam,
                ChatColor.GREEN + "✓ You are now allies with " + team.getColoredName() + "!");

        return true;
    }

    private boolean handleAllyRemove(Player player, String[] args) {
        Team team = teamManager.getPlayerTeam(player.getUniqueId());

        if (team == null) {
            player.sendMessage(ChatColor.RED + "You are not in a team!");
            return true;
        }

        if (!team.isLeader(player.getUniqueId()) && !team.isModerator(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Only team leaders and moderators can remove allies!");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /team allyremove <team name>");
            return true;
        }

        String targetTeamName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        Team targetTeam = teamManager.getTeamByName(targetTeamName);

        if (targetTeam == null) {
            player.sendMessage(ChatColor.RED + "Team not found!");
            return true;
        }

        if (!team.isAlly(targetTeam.getId())) {
            player.sendMessage(ChatColor.RED + "You are not allies with " + targetTeam.getColoredName() + "!");
            return true;
        }

        // Remove alliance (mutual)
        team.removeAlly(targetTeam.getId());
        targetTeam.removeAlly(team.getId());

        teamManager.saveTeams();

        // Notify both teams
        teamManager.broadcastToTeam(team,
                ChatColor.YELLOW + "Alliance with " + targetTeam.getColoredName() + " has been broken");
        teamManager.broadcastToTeam(targetTeam,
                ChatColor.YELLOW + "Alliance with " + team.getColoredName() + " has been broken");

        return true;
    }

    private boolean handleAllyList(Player player) {
        Team team = teamManager.getPlayerTeam(player.getUniqueId());

        if (team == null) {
            player.sendMessage(ChatColor.RED + "You are not in a team!");
            return true;
        }

        Set<UUID> allies = team.getAllies();

        if (allies.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "Your team has no allies");
            return true;
        }

        player.sendMessage(ChatColor.GOLD + "╔═══════════════════════════════╗");
        player.sendMessage(ChatColor.YELLOW + "      Team Allies");
        player.sendMessage(ChatColor.GOLD + "╠═══════════════════════════════╣");

        for (UUID allyId : allies) {
            Team allyTeam = teamManager.getTeam(allyId);
            if (allyTeam != null) {
                player.sendMessage(ChatColor.WHITE + "• " + allyTeam.getColoredName() +
                        ChatColor.GRAY + " (" + allyTeam.getMemberCount() + " members)");
            }
        }

        player.sendMessage(ChatColor.GOLD + "╚═══════════════════════════════╝");

        return true;
    }

    private boolean handleAllyPermissions(Player player) {
        Team team = teamManager.getPlayerTeam(player.getUniqueId());

        if (team == null) {
            player.sendMessage(ChatColor.RED + "You are not in a team!");
            return true;
        }

        if (!team.isLeader(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Only the team leader can manage ally permissions!");
            return true;
        }

        // Open GUI for ally permissions
        teamManageGUI.openAllyPermissionsMenu(player, team);

        return true;
    }

    private boolean handleDisband(Player player) {
        Team team = teamManager.getPlayerTeam(player.getUniqueId());

        if (team == null) {
            player.sendMessage(ChatColor.RED + "You are not in a team!");
            return true;
        }

        if (!team.isLeader(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Only the team leader can disband the team!");
            return true;
        }

        String teamName = team.getColoredName();

        // Get all members before disbanding
        Set<UUID> members = new HashSet<>(team.getMembers());

        // Notify all members
        teamManager.broadcastToTeam(team,
                ChatColor.RED + "Your team has been disbanded by the leader!");

        teamManager.deleteTeam(team.getId());

        // Update nametags for all former members
        if (plugin.getNametagManager() != null) {
            for (UUID memberId : members) {
                Player member = Bukkit.getPlayer(memberId);
                if (member != null && member.isOnline()) {
                    plugin.getNametagManager().updatePlayer(member);
                }
            }
        }

        player.sendMessage(ChatColor.GREEN + "Successfully disbanded " + teamName);

        return true;
    }

    private boolean handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /team invite <player>");
            return true;
        }

        Team team = teamManager.getPlayerTeam(player.getUniqueId());

        if (team == null) {
            player.sendMessage(ChatColor.RED + "You are not in a team!");
            return true;
        }

        if (!team.isModerator(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Only team moderators can invite players!");
            return true;
        }

        if (team.isFull()) {
            player.sendMessage(ChatColor.RED + "Your team is full!");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);

        if (target == null) {
            player.sendMessage(ChatColor.RED + "Player not found!");
            return true;
        }

        if (teamManager.getPlayerTeam(target.getUniqueId()) != null) {
            player.sendMessage(ChatColor.RED + "That player is already in a team!");
            return true;
        }

        team.addInvitation(target.getUniqueId());

        player.sendMessage(ChatColor.GREEN + "Invited " + target.getName() + " to your team!");
        target.sendMessage(ChatColor.GOLD + "╔═══════════════════════════════╗");
        target.sendMessage(ChatColor.YELLOW + "You've been invited to join " + team.getColoredName());
        target.sendMessage(ChatColor.GRAY + "Use " + ChatColor.WHITE + "/team join " + team.getName() +
                ChatColor.GRAY + " to accept");
        target.sendMessage(ChatColor.GRAY + "Invitation expires in 5 minutes");
        target.sendMessage(ChatColor.GOLD + "╚═══════════════════════════════╝");

        return true;
    }

    private boolean handleJoin(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /team join <team name>");
            return true;
        }

        if (teamManager.getPlayerTeam(player.getUniqueId()) != null) {
            player.sendMessage(ChatColor.RED + "You are already in a team!");
            return true;
        }

        String teamName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        Team team = teamManager.getTeamByName(teamName);

        if (team == null) {
            player.sendMessage(ChatColor.RED + "Team not found!");
            return true;
        }

        if (!team.hasInvitation(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You don't have an invitation to this team!");
            return true;
        }

        if (team.isFull()) {
            player.sendMessage(ChatColor.RED + "That team is full!");
            return true;
        }

        teamManager.addPlayerToTeam(player.getUniqueId(), team);

        // Update nametag
        if (plugin.getNametagManager() != null) {
            plugin.getNametagManager().updatePlayer(player);
        }

        player.sendMessage(ChatColor.GREEN + "Successfully joined " + team.getColoredName() + "!");
        teamManager.broadcastToTeam(team,
                ChatColor.YELLOW + player.getName() + " has joined the team!");

        return true;
    }

    private boolean handleLeave(Player player) {
        Team team = teamManager.getPlayerTeam(player.getUniqueId());

        if (team == null) {
            player.sendMessage(ChatColor.RED + "You are not in a team!");
            return true;
        }

        if (team.isLeader(player.getUniqueId()) && team.getMemberCount() > 1) {
            player.sendMessage(ChatColor.RED + "You cannot leave as leader! Transfer leadership or disband the team.");
            return true;
        }

        String teamName = team.getColoredName();
        teamManager.removePlayerFromTeam(player.getUniqueId(), team);

        // Update nametag
        if (plugin.getNametagManager() != null) {
            plugin.getNametagManager().updatePlayer(player);
        }

        player.sendMessage(ChatColor.GREEN + "You left " + teamName);
        teamManager.broadcastToTeam(team,
                ChatColor.YELLOW + player.getName() + " has left the team!");

        return true;
    }

    private boolean handleKick(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /team kick <player>");
            return true;
        }

        Team team = teamManager.getPlayerTeam(player.getUniqueId());

        if (team == null) {
            player.sendMessage(ChatColor.RED + "You are not in a team!");
            return true;
        }

        if (!team.isModerator(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Only team moderators can kick players!");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);

        if (target == null) {
            player.sendMessage(ChatColor.RED + "Player not found!");
            return true;
        }

        if (!team.isMember(target.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "That player is not in your team!");
            return true;
        }

        if (team.isLeader(target.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You cannot kick the team leader!");
            return true;
        }

        teamManager.removePlayerFromTeam(target.getUniqueId(), team);

        // Update nametag for kicked player
        if (plugin.getNametagManager() != null) {
            plugin.getNametagManager().updatePlayer(target);
        }

        player.sendMessage(ChatColor.GREEN + "Kicked " + target.getName() + " from the team!");
        target.sendMessage(ChatColor.RED + "You were kicked from " + team.getColoredName());
        teamManager.broadcastToTeam(team,
                ChatColor.YELLOW + target.getName() + " was kicked from the team!");

        return true;
    }

    private boolean handlePromote(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /team promote <player>");
            return true;
        }

        Team team = teamManager.getPlayerTeam(player.getUniqueId());

        if (team == null) {
            player.sendMessage(ChatColor.RED + "You are not in a team!");
            return true;
        }

        if (!team.isLeader(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Only the team leader can promote members!");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);

        if (target == null) {
            player.sendMessage(ChatColor.RED + "Player not found!");
            return true;
        }

        if (!team.isMember(target.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "That player is not in your team!");
            return true;
        }

        if (team.addModerator(target.getUniqueId())) {
            player.sendMessage(ChatColor.GREEN + "Promoted " + target.getName() + " to moderator!");
            target.sendMessage(ChatColor.GREEN + "You were promoted to moderator in " + team.getColoredName() + "!");
            teamManager.broadcastToTeam(team,
                    ChatColor.YELLOW + target.getName() + " was promoted to moderator!");
        } else {
            player.sendMessage(ChatColor.RED + "Failed to promote player!");
        }

        return true;
    }

    private boolean handleDemote(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /team demote <player>");
            return true;
        }

        Team team = teamManager.getPlayerTeam(player.getUniqueId());

        if (team == null) {
            player.sendMessage(ChatColor.RED + "You are not in a team!");
            return true;
        }

        if (!team.isLeader(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Only the team leader can demote moderators!");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);

        if (target == null) {
            player.sendMessage(ChatColor.RED + "Player not found!");
            return true;
        }

        if (team.removeModerator(target.getUniqueId())) {
            player.sendMessage(ChatColor.GREEN + "Demoted " + target.getName() + " from moderator!");
            target.sendMessage(ChatColor.YELLOW + "You were demoted in " + team.getColoredName());
        } else {
            player.sendMessage(ChatColor.RED + "That player is not a moderator!");
        }

        return true;
    }

    private boolean handleInfo(Player player, String[] args) {
        Team team;

        if (args.length > 1) {
            String teamName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            team = teamManager.getTeamByName(teamName);
        } else {
            team = teamManager.getPlayerTeam(player.getUniqueId());
        }

        if (team == null) {
            player.sendMessage(ChatColor.RED + "Team not found!");
            return true;
        }

        player.sendMessage(teamManager.formatTeamInfo(team));

        return true;
    }

    private boolean handleChat(Player player) {
        Team team = teamManager.getPlayerTeam(player.getUniqueId());

        if (team == null) {
            player.sendMessage(ChatColor.RED + "You are not in a team!");
            return true;
        }

        plugin.getChatManager().toggleTeamChat(player);
        return true;
    }

    private boolean handleStash(Player player) {
        Team team = teamManager.getPlayerTeam(player.getUniqueId());

        if (team == null) {
            player.sendMessage(ChatColor.RED + "You are not in a team!");
            return true;
        }

        plugin.getStashManager().openStash(player);
        return true;
    }

    private boolean handleTop(Player player, String[] args) {
        String type = args.length > 1 ? args[1].toLowerCase() : "kills";

        List<Team> topTeams;
        String title;

        if (type.equals("kd")) {
            topTeams = teamManager.getTopTeamsByKD(10);
            title = "Top Teams by K/D Ratio";
        } else {
            topTeams = teamManager.getTopTeamsByKills(10);
            title = "Top Teams by Kills";
        }

        player.sendMessage(ChatColor.GOLD + "╔═══════════════════════════════╗");
        player.sendMessage(ChatColor.YELLOW + "      " + title);
        player.sendMessage(ChatColor.GOLD + "╠═══════════════════════════════╣");

        int rank = 1;
        for (Team team : topTeams) {
            String statValue = type.equals("kd") ?
                    String.format("%.2f", team.getKDRatio()) :
                    String.valueOf(team.getTotalKills());

            player.sendMessage(ChatColor.WHITE + "#" + rank + " " + team.getColoredName() +
                    ChatColor.GRAY + " - " + ChatColor.GOLD + statValue);
            rank++;
        }

        player.sendMessage(ChatColor.GOLD + "╚═══════════════════════════════╝");

        return true;
    }

    private boolean handleColor(Player player, String[] args) {
        Team team = teamManager.getPlayerTeam(player.getUniqueId());

        if (team == null) {
            player.sendMessage(ChatColor.RED + "You are not in a team!");
            return true;
        }

        if (!team.isLeader(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Only the team leader can change the team color!");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /team color <color>");
            player.sendMessage(ChatColor.GRAY + "Available colors: red, blue, green, yellow, aqua, gold, white");
            return true;
        }

        ChatColor color;
        try {
            color = ChatColor.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "Invalid color!");
            return true;
        }

        team.setColor(color);
        teamManager.saveTeams();

        // Update nametags for all team members
        if (plugin.getNametagManager() != null) {
            plugin.getNametagManager().updateTeam(team);
        }

        player.sendMessage(ChatColor.GREEN + "Team color changed to " + color + color.name());

        return true;
    }

    private boolean handleTag(Player player, String[] args) {
        Team team = teamManager.getPlayerTeam(player.getUniqueId());

        if (team == null) {
            player.sendMessage(ChatColor.RED + "You are not in a team!");
            return true;
        }

        if (!team.isLeader(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Only the team leader can change the tag!");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /team tag <text>");
            return true;
        }

        String tag = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        if (tag.length() > 16) {
            player.sendMessage(ChatColor.RED + "Tag must be 16 characters or less!");
            return true;
        }

        team.setTag(tag);
        teamManager.saveTeams();

        // Update nametags for all team members
        if (plugin.getNametagManager() != null) {
            plugin.getNametagManager().updateTeam(team);
        }

        player.sendMessage(ChatColor.GREEN + "Team tag updated!");

        return true;
    }

    private boolean handleDescription(Player player, String[] args) {
        Team team = teamManager.getPlayerTeam(player.getUniqueId());

        if (team == null) {
            player.sendMessage(ChatColor.RED + "You are not in a team!");
            return true;
        }

        if (!team.isLeader(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Only the team leader can change the description!");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /team description <text>");
            return true;
        }

        String description = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        if (description.length() > 100) {
            player.sendMessage(ChatColor.RED + "Description must be 100 characters or less!");
            return true;
        }

        team.setDescription(description);
        teamManager.saveTeams();

        player.sendMessage(ChatColor.GREEN + "Team description updated!");

        return true;
    }

    private boolean handleTransfer(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /team transfer <player>");
            return true;
        }

        Team team = teamManager.getPlayerTeam(player.getUniqueId());

        if (team == null) {
            player.sendMessage(ChatColor.RED + "You are not in a team!");
            return true;
        }

        if (!team.isLeader(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Only the team leader can transfer leadership!");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);

        if (target == null) {
            player.sendMessage(ChatColor.RED + "Player not found!");
            return true;
        }

        if (!team.isMember(target.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "That player is not in your team!");
            return true;
        }

        team.setLeader(target.getUniqueId());
        teamManager.saveTeams();

        player.sendMessage(ChatColor.GREEN + "Transferred leadership to " + target.getName());
        target.sendMessage(ChatColor.GOLD + "You are now the leader of " + team.getColoredName() + "!");
        teamManager.broadcastToTeam(team,
                ChatColor.YELLOW + target.getName() + " is now the team leader!");

        return true;
    }

    private void sendHelpMessage(Player player) {
        player.sendMessage(ChatColor.GOLD + "╔═══════════════════════════════╗");
        player.sendMessage(ChatColor.YELLOW + "         Team Commands");
        player.sendMessage(ChatColor.GOLD + "╠═══════════════════════════════╣");
        player.sendMessage(ChatColor.AQUA + "/team create <name>" + ChatColor.GRAY + " - Create a team");
        player.sendMessage(ChatColor.AQUA + "/team list" + ChatColor.GRAY + " - View all teams");
        player.sendMessage(ChatColor.AQUA + "/team info [team]" + ChatColor.GRAY + " - Team info");
        player.sendMessage(ChatColor.AQUA + "/team manage" + ChatColor.GRAY + " - Manage team (GUI)");
        player.sendMessage(ChatColor.AQUA + "/team chat" + ChatColor.GRAY + " - Toggle team chat");
        player.sendMessage(ChatColor.AQUA + "/team stash" + ChatColor.GRAY + " - Open team stash");
        player.sendMessage(ChatColor.AQUA + "/team invite <player>" + ChatColor.GRAY + " - Invite player");
        player.sendMessage(ChatColor.AQUA + "/team join <team>" + ChatColor.GRAY + " - Join team");
        player.sendMessage(ChatColor.AQUA + "/team leave" + ChatColor.GRAY + " - Leave team");
        player.sendMessage(ChatColor.AQUA + "/team top [kills|kd]" + ChatColor.GRAY + " - Top teams");
        player.sendMessage(ChatColor.GOLD + "╠═══════════════════════════════╣");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "/team ally <team>" + ChatColor.GRAY + " - Request alliance");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "/team allyaccept <team>" + ChatColor.GRAY + " - Accept alliance");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "/team allyremove <team>" + ChatColor.GRAY + " - Remove ally");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "/team allylist" + ChatColor.GRAY + " - List allies");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "/team allypermissions" + ChatColor.GRAY + " - Manage ally perms");
        player.sendMessage(ChatColor.GOLD + "╚═══════════════════════════════╝");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("create", "list", "info", "manage", "invite",
                    "join", "leave", "kick", "promote", "demote", "top", "disband", "color",
                    "tag", "description", "transfer", "chat", "stash", "help",
                    "ally", "allyaccept", "allyremove", "allylist", "allypermissions"));

            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {

            if (args[0].equalsIgnoreCase("ally") || args[0].equalsIgnoreCase("allyaccept") ||
                    args[0].equalsIgnoreCase("allyremove")) {
                return teamManager.getAllTeams().stream()
                        .map(Team::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }

            if (args[0].equalsIgnoreCase("top")) {
                return Arrays.asList("kills", "kd");
            }

            if (args[0].equalsIgnoreCase("color")) {
                return Arrays.asList("red", "blue", "green", "yellow", "aqua", "gold", "white");
            }

            if (args[0].equalsIgnoreCase("join") || args[0].equalsIgnoreCase("info")) {
                return teamManager.getAllTeams().stream()
                        .map(Team::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }

            if (args[0].equalsIgnoreCase("invite") || args[0].equalsIgnoreCase("kick") ||
                    args[0].equalsIgnoreCase("promote") || args[0].equalsIgnoreCase("demote") ||
                    args[0].equalsIgnoreCase("transfer")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }


        }

        return completions;
    }
}
