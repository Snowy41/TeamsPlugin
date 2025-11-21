package com.mcbzh.teams.listeners;

import com.mcbzh.teams.TeamsPlugin;
import com.mcbzh.teams.gui.TeamListGUI;
import com.mcbzh.teams.gui.TeamManageGUI;
import com.mcbzh.teams.managers.TeamManager;
import com.mcbzh.teams.models.Team;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GUIListener implements Listener {
    private final TeamsPlugin plugin;
    private final TeamManager teamManager;
    private final TeamListGUI teamListGUI;
    private final TeamManageGUI teamManageGUI;

    public GUIListener(TeamsPlugin plugin) {
        this.plugin = plugin;
        this.teamManager = plugin.getTeamManager();
        this.teamListGUI = new TeamListGUI(plugin);
        this.teamManageGUI = new TeamManageGUI(plugin);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        // Check if it's one of our GUIs
        if (!title.contains("Teams") && !title.contains("Manage:") && !title.contains("Members") && !title.contains("Settings")) {
            return;
        }

        event.setCancelled(true); // Cancel all clicks in our GUIs

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        // Team List GUI
        if (title.contains("Teams")) {
            handleTeamListClick(player, clicked, title);
        }
        // Team Management GUI
        else if (title.contains("Manage:")) {
            handleTeamManageClick(player, clicked);
        }
        // Members GUI
        else if (title.contains("Members")) {
            handleMembersClick(player, clicked, event.getClick());
        }
        // Settings GUI
        else if (title.contains("Settings")) {
            handleSettingsClick(player, clicked, event.getClick());
        }
    }

    private void handleTeamListClick(Player player, ItemStack clicked, String title) {
        String displayName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());

        // Navigation
        if (displayName.equals("Previous Page")) {
            int currentPage = extractPageNumber(title);
            teamListGUI.openTeamList(player, currentPage - 1);
            return;
        }

        if (displayName.equals("Next Page")) {
            int currentPage = extractPageNumber(title);
            teamListGUI.openTeamList(player, currentPage + 1);
            return;
        }

        if (displayName.equals("Close")) {
            player.closeInventory();
            return;
        }

        if (displayName.equals("Create New Team")) {
            player.closeInventory();
            player.sendMessage(ChatColor.YELLOW + "Use " + ChatColor.WHITE + "/team create <name>" +
                    ChatColor.YELLOW + " to create a team!");
            return;
        }

        // Team item clicked
        if (clicked.getType() == Material.WHITE_BANNER || clicked.getType() == Material.RED_BANNER) {
            Team clickedTeam = findTeamByDisplayName(clicked);
            if (clickedTeam == null) return;

            Team playerTeam = teamManager.getPlayerTeam(player.getUniqueId());

            // If player is in this team, open management
            if (playerTeam != null && playerTeam.equals(clickedTeam)) {
                teamManageGUI.openTeamManageMenu(player);
                return;
            }

            // If player has invitation, join
            if (clickedTeam.hasInvitation(player.getUniqueId())) {
                if (playerTeam != null) {
                    player.sendMessage(ChatColor.RED + "You must leave your current team first!");
                    return;
                }

                if (clickedTeam.isFull()) {
                    player.sendMessage(ChatColor.RED + "That team is now full!");
                    return;
                }

                teamManager.addPlayerToTeam(player.getUniqueId(), clickedTeam);
                player.closeInventory();
                player.sendMessage(ChatColor.GREEN + "Successfully joined " + clickedTeam.getColoredName() + "!");
                teamManager.broadcastToTeam(clickedTeam,
                        ChatColor.YELLOW + player.getName() + " has joined the team!");
                return;
            }

            // Otherwise show team info
            player.closeInventory();
            player.sendMessage(teamManager.formatTeamInfo(clickedTeam));
        }
    }

    private void handleTeamManageClick(Player player, ItemStack clicked) {
        Team team = teamManager.getPlayerTeam(player.getUniqueId());
        if (team == null) return;

        String displayName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());

        switch (displayName) {
            case "View Members":
                teamManageGUI.openMembersMenu(player, team);
                break;

            case "Invite Player":
                if (team.isModerator(player.getUniqueId())) {
                    player.closeInventory();
                    player.sendMessage(ChatColor.YELLOW + "Use " + ChatColor.WHITE + "/team invite <player>" +
                            ChatColor.YELLOW + " to invite someone!");
                }
                break;

            case "Team Settings":
                if (team.isLeader(player.getUniqueId())) {
                    teamManageGUI.openSettingsMenu(player, team);
                }
                break;

            case "Leave Team":
                if (!team.isLeader(player.getUniqueId())) {
                    player.closeInventory();
                    Bukkit.dispatchCommand(player, "team leave");
                }
                break;

            case "Disband Team":
                if (team.isLeader(player.getUniqueId())) {
                    player.closeInventory();
                    Bukkit.dispatchCommand(player, "team disband");
                }
                break;

            case "Team Chat":
                // Toggle team chat
                player.closeInventory();
                plugin.getChatManager().toggleTeamChat(player);
                break;

            case "Team Stash":
                // Open team stash
                player.closeInventory();
                plugin.getStashManager().openStash(player);
                break;

            case "Close":
                player.closeInventory();
                break;
        }
    }

    private void handleMembersClick(Player player, ItemStack clicked, ClickType clickType) {
        Team team = teamManager.getPlayerTeam(player.getUniqueId());
        if (team == null) return;

        String displayName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());

        if (displayName.equals("Back")) {
            teamManageGUI.openTeamManageMenu(player);
            return;
        }

        // Member head clicked
        if (clicked.getType() == Material.PLAYER_HEAD) {
            // Get player name from display name
            String memberName = ChatColor.stripColor(displayName);
            Player target = Bukkit.getPlayer(memberName);

            if (target == null) {
                player.sendMessage(ChatColor.RED + "Player not found!");
                return;
            }

            if (target.equals(player)) {
                return; // Can't manage yourself
            }

            boolean isLeader = team.isLeader(player.getUniqueId());
            boolean isMod = team.isModerator(player.getUniqueId());
            boolean targetIsMod = team.isModerator(target.getUniqueId());
            boolean targetIsLeader = team.isLeader(target.getUniqueId());

            // Shift-click to kick
            if (clickType == ClickType.SHIFT_LEFT || clickType == ClickType.SHIFT_RIGHT) {
                if ((isMod && !targetIsMod && !targetIsLeader) || (isLeader && !targetIsLeader)) {
                    player.closeInventory();
                    Bukkit.dispatchCommand(player, "team kick " + target.getName());
                }
                return;
            }

            // Regular click to promote/demote (leader only)
            if (isLeader && !targetIsLeader) {
                if (targetIsMod) {
                    player.closeInventory();
                    Bukkit.dispatchCommand(player, "team demote " + target.getName());
                } else {
                    player.closeInventory();
                    Bukkit.dispatchCommand(player, "team promote " + target.getName());
                }

                // Reopen menu after short delay
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    teamManageGUI.openMembersMenu(player, team);
                }, 10L);
            }
        }
    }

    private void handleSettingsClick(Player player, ItemStack clicked, ClickType clickType) {
        Team team = teamManager.getPlayerTeam(player.getUniqueId());
        if (team == null || !team.isLeader(player.getUniqueId())) return;

        String displayName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());

        switch (displayName) {
            case "Friendly Fire":
                team.setFriendlyFire(!team.isFriendlyFire());
                teamManager.saveTeams();
                player.sendMessage(ChatColor.GREEN + "Friendly fire " +
                        (team.isFriendlyFire() ? "enabled" : "disabled") + "!");
                teamManageGUI.openSettingsMenu(player, team);
                break;

            case "Team Color":
                player.closeInventory();
                player.sendMessage(ChatColor.YELLOW + "Use " + ChatColor.WHITE + "/team color <color>" +
                        ChatColor.YELLOW + " to change team color!");
                player.sendMessage(ChatColor.GRAY + "Available: red, blue, green, yellow, aqua, gold, white");
                break;

            case "Team Description":
                player.closeInventory();
                player.sendMessage(ChatColor.YELLOW + "Use " + ChatColor.WHITE + "/team description <text>" +
                        ChatColor.YELLOW + " to change team description!");
                break;

            case "Maximum Members":
                int current = team.getMaxMembers();
                int newMax = current;

                if (clickType == ClickType.LEFT) {
                    newMax = Math.min(current + 1, 50);
                } else if (clickType == ClickType.RIGHT) {
                    newMax = Math.max(current - 1, team.getMemberCount());
                }

                if (newMax != current) {
                    team.setMaxMembers(newMax);
                    teamManager.saveTeams();
                    player.sendMessage(ChatColor.GREEN + "Maximum members set to " + newMax);
                    teamManageGUI.openSettingsMenu(player, team);
                }
                break;

            case "Transfer Leadership":
                player.closeInventory();
                player.sendMessage(ChatColor.YELLOW + "Use " + ChatColor.WHITE + "/team transfer <player>" +
                        ChatColor.YELLOW + " to transfer leadership!");
                break;

            case "Back":
                teamManageGUI.openTeamManageMenu(player);
                break;
        }
    }

    private int extractPageNumber(String title) {
        Pattern pattern = Pattern.compile("\\((\\d+)/\\d+\\)");
        Matcher matcher = pattern.matcher(title);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 1;
    }

    private Team findTeamByDisplayName(ItemStack item) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return null;
        }

        String displayName = item.getItemMeta().getDisplayName();
        // Strip color codes to get team name
        String teamName = ChatColor.stripColor(displayName);

        return teamManager.getTeamByName(teamName);
    }
}
