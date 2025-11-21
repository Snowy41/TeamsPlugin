package com.mcbzh.teams.gui;

import com.mcbzh.teams.TeamsPlugin;
import com.mcbzh.teams.managers.TeamManager;
import com.mcbzh.teams.models.Team;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class TeamListGUI {
    private final TeamsPlugin plugin;
    private final TeamManager teamManager;

    public TeamListGUI(TeamsPlugin plugin) {
        this.plugin = plugin;
        this.teamManager = plugin.getTeamManager();
    }

    public void openTeamList(Player player, int page) {
        List<Team> teams = teamManager.getAllTeams();
        int maxPage = (int) Math.ceil(teams.size() / 28.0);

        if (maxPage == 0) maxPage = 1;
        if (page > maxPage) page = maxPage;
        if (page < 1) page = 1;

        Inventory inv = Bukkit.createInventory(null, 54,
                ChatColor.DARK_GREEN + "Teams " + ChatColor.GRAY + "(" + page + "/" + maxPage + ")");

        // Add teams to inventory
        int startIndex = (page - 1) * 28;
        int endIndex = Math.min(startIndex + 28, teams.size());

        int slot = 10;
        for (int i = startIndex; i < endIndex; i++) {
            Team team = teams.get(i);

            ItemStack item = createTeamItem(team, player);
            inv.setItem(slot, item);

            slot++;
            if (slot == 17) slot = 19;
            if (slot == 26) slot = 28;
            if (slot == 35) slot = 37;
        }

        // Navigation and control items
        addBorderGlass(inv);

        // Previous page
        if (page > 1) {
            ItemStack prevPage = createItem(Material.ARROW,
                    ChatColor.YELLOW + "Previous Page",
                    ChatColor.GRAY + "Go to page " + (page - 1));
            inv.setItem(48, prevPage);
        }

        // Next page
        if (page < maxPage) {
            ItemStack nextPage = createItem(Material.ARROW,
                    ChatColor.YELLOW + "Next Page",
                    ChatColor.GRAY + "Go to page " + (page + 1));
            inv.setItem(50, nextPage);
        }

        // Create new team button
        Team playerTeam = teamManager.getPlayerTeam(player.getUniqueId());
        if (playerTeam == null) {
            ItemStack createTeam = createItem(Material.NETHER_STAR,
                    ChatColor.GREEN + "Create New Team",
                    ChatColor.GRAY + "Click to create your own team!",
                    "",
                    ChatColor.YELLOW + "⚠ You must not be in a team");
            inv.setItem(49, createTeam);
        }

        // Info item
        ItemStack info = createItem(Material.BOOK,
                ChatColor.AQUA + "Team Information",
                ChatColor.GRAY + "Total Teams: " + ChatColor.WHITE + teams.size(),
                "",
                ChatColor.YELLOW + "Click on a team to view details",
                ChatColor.YELLOW + "or request to join!");
        inv.setItem(4, info);

        // Close button
        ItemStack close = createItem(Material.BARRIER,
                ChatColor.RED + "Close",
                ChatColor.GRAY + "Close this menu");
        inv.setItem(45, close);

        player.openInventory(inv);
    }

    private ItemStack createTeamItem(Team team, Player viewer) {
        Material material = team.isFull() ? Material.RED_BANNER : Material.WHITE_BANNER;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(team.getColoredName());

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "━━━━━━━━━━━━━━━━━━━");
        lore.add(ChatColor.YELLOW + "Leader: " + ChatColor.WHITE +
                getPlayerName(team.getLeader()));
        lore.add(ChatColor.YELLOW + "Members: " + ChatColor.WHITE +
                team.getMemberCount() + "/" + team.getMaxMembers());
        lore.add("");
        lore.add(ChatColor.GRAY + "Description:");
        lore.add(ChatColor.WHITE + team.getDescription());
        lore.add("");
        lore.add(ChatColor.AQUA + "Statistics:");
        lore.add(ChatColor.WHITE + "  ⚔ Kills: " + ChatColor.GREEN + team.getTotalKills());
        lore.add(ChatColor.WHITE + "  ☠ Deaths: " + ChatColor.RED + team.getTotalDeaths());
        lore.add(ChatColor.WHITE + "  📊 K/D: " + ChatColor.GOLD +
                String.format("%.2f", team.getKDRatio()));
        lore.add("");

        Team viewerTeam = teamManager.getPlayerTeam(viewer.getUniqueId());

        if (viewerTeam != null && viewerTeam.equals(team)) {
            lore.add(ChatColor.GREEN + "✓ You are in this team!");
            lore.add(ChatColor.YELLOW + "Click to manage");
        } else if (team.isFull()) {
            lore.add(ChatColor.RED + "✗ Team is full");
        } else if (viewerTeam != null) {
            lore.add(ChatColor.RED + "✗ You're already in a team");
        } else if (team.hasInvitation(viewer.getUniqueId())) {
            lore.add(ChatColor.GREEN + "✓ You have an invitation!");
            lore.add(ChatColor.YELLOW + "Click to join");
        } else {
            lore.add(ChatColor.YELLOW + "Click to request joining");
        }

        lore.add(ChatColor.GRAY + "━━━━━━━━━━━━━━━━━━━");

        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }

    private void addBorderGlass(Inventory inv) {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.setDisplayName(" ");
        glass.setItemMeta(meta);

        // Top and bottom rows
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, glass);
            inv.setItem(i + 45, glass);
        }

        // Side columns
        inv.setItem(9, glass);
        inv.setItem(18, glass);
        inv.setItem(27, glass);
        inv.setItem(36, glass);
        inv.setItem(17, glass);
        inv.setItem(26, glass);
        inv.setItem(35, glass);
        inv.setItem(44, glass);
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);

        if (lore.length > 0) {
            List<String> loreList = new ArrayList<>();
            for (String line : lore) {
                loreList.add(line);
            }
            meta.setLore(loreList);
        }

        item.setItemMeta(meta);
        return item;
    }

    private String getPlayerName(java.util.UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            return player.getName();
        }

        org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        String name = offlinePlayer.getName();
        return name != null ? name : "Unknown";
    }
}
