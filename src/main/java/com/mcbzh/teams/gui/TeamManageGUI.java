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
import java.util.UUID;

public class TeamManageGUI {
    private final TeamsPlugin plugin;
    private final TeamManager teamManager;

    public TeamManageGUI(TeamsPlugin plugin) {
        this.plugin = plugin;
        this.teamManager = plugin.getTeamManager();
    }

    public void openTeamManageMenu(Player player) {
        Team team = teamManager.getPlayerTeam(player.getUniqueId());
        if (team == null) {
            player.sendMessage(ChatColor.RED + "You are not in a team!");
            return;
        }

        boolean isLeader = team.isLeader(player.getUniqueId());
        boolean isModerator = team.isModerator(player.getUniqueId());

        Inventory inv = Bukkit.createInventory(null, 54,
                ChatColor.DARK_GREEN + "Manage: " + team.getColoredName());

        addBorderGlass(inv);

        // Team info
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName(ChatColor.GOLD + "Team Information");
        List<String> infoLore = new ArrayList<>();
        infoLore.add(ChatColor.GRAY + "━━━━━━━━━━━━━━━━━━━");
        infoLore.add(ChatColor.YELLOW + "Name: " + team.getColoredName());
        infoLore.add(ChatColor.YELLOW + "Tag: " + team.getColoredTag());
        infoLore.add(ChatColor.YELLOW + "Members: " + ChatColor.WHITE +
                team.getMemberCount() + "/" + team.getMaxMembers());
        infoLore.add(ChatColor.YELLOW + "Leader: " + ChatColor.WHITE +
                getPlayerName(team.getLeader()));
        infoLore.add("");
        infoLore.add(ChatColor.AQUA + "Statistics:");
        infoLore.add(ChatColor.WHITE + "  ⚔ Kills: " + ChatColor.GREEN + team.getTotalKills());
        infoLore.add(ChatColor.WHITE + "  ☠ Deaths: " + ChatColor.RED + team.getTotalDeaths());
        infoLore.add(ChatColor.WHITE + "  📊 K/D: " + ChatColor.GOLD +
                String.format("%.2f", team.getKDRatio()));
        infoLore.add(ChatColor.GRAY + "━━━━━━━━━━━━━━━━━━━");
        infoMeta.setLore(infoLore);
        info.setItemMeta(infoMeta);
        inv.setItem(4, info);

        // View members
        ItemStack members = createItem(Material.PLAYER_HEAD,
                ChatColor.GREEN + "View Members",
                ChatColor.GRAY + "Click to see all team members",
                ChatColor.YELLOW + "Total: " + team.getMemberCount());
        inv.setItem(19, members);

        // Invite player (moderators+)
        if (isModerator && !team.isFull()) {
            ItemStack invite = createItem(Material.WRITABLE_BOOK,
                    ChatColor.AQUA + "Invite Player",
                    ChatColor.GRAY + "Click to invite a player",
                    ChatColor.YELLOW + "Type their name in chat");
            inv.setItem(21, invite);
        }

        // Team chat
        boolean chatEnabled = plugin.getChatManager().isTeamChatEnabled(player.getUniqueId());
        ItemStack chat = createItem(Material.PAPER,
                ChatColor.YELLOW + "Team Chat",
                ChatColor.GRAY + "Toggle team chat mode",
                "",
                ChatColor.GRAY + "Status: " + (chatEnabled ?
                        ChatColor.GREEN + "✔ Enabled" : ChatColor.RED + "✖ Disabled"),
                "",
                ChatColor.WHITE + "Click to toggle");
        inv.setItem(23, chat);

        // Team stash
        ItemStack stash = createItem(Material.CHEST,
                ChatColor.GOLD + "Team Stash",
                ChatColor.GRAY + "Access team storage",
                "",
                ChatColor.WHITE + "Click to open");
        inv.setItem(25, stash);

        // Team settings (leader only)
        if (isLeader) {
            ItemStack settings = createItem(Material.COMPARATOR,
                    ChatColor.LIGHT_PURPLE + "Team Settings",
                    ChatColor.GRAY + "Configure team options",
                    "",
                    ChatColor.YELLOW + "Leader only");
            inv.setItem(31, settings);
        }

        // Leave team
        if (!isLeader) {
            ItemStack leave = createItem(Material.RED_BED,
                    ChatColor.RED + "Leave Team",
                    ChatColor.GRAY + "Exit from this team",
                    "",
                    ChatColor.DARK_RED + "⚠ This action cannot be undone!");
            inv.setItem(48, leave);
        }

        // Disband team (leader only)
        if (isLeader) {
            ItemStack disband = createItem(Material.TNT,
                    ChatColor.DARK_RED + "Disband Team",
                    ChatColor.GRAY + "Permanently delete this team",
                    "",
                    ChatColor.DARK_RED + "⚠ WARNING: This cannot be undone!",
                    ChatColor.RED + "All members will be removed!");
            inv.setItem(49, disband);
        }

        // Close
        ItemStack close = createItem(Material.BARRIER,
                ChatColor.RED + "Close",
                ChatColor.GRAY + "Close this menu");
        inv.setItem(45, close);

        player.openInventory(inv);
    }

    public void openMembersMenu(Player player, Team team) {
        Inventory inv = Bukkit.createInventory(null, 54,
                ChatColor.DARK_GREEN + "Team Members");

        addBorderGlass(inv);

        boolean isLeader = team.isLeader(player.getUniqueId());
        boolean isModerator = team.isModerator(player.getUniqueId());

        List<UUID> members = new ArrayList<>(team.getMembers());
        int slot = 10;

        for (UUID memberId : members) {
            if (slot >= 44) break;

            ItemStack memberItem = createMemberItem(memberId, team, player, isLeader, isModerator);
            inv.setItem(slot, memberItem);

            slot++;
            if (slot == 17) slot = 19;
            if (slot == 26) slot = 28;
            if (slot == 35) slot = 37;
        }

        // Back button
        ItemStack back = createItem(Material.ARROW,
                ChatColor.YELLOW + "Back",
                ChatColor.GRAY + "Return to team management");
        inv.setItem(45, back);

        player.openInventory(inv);
    }

    public void openSettingsMenu(Player player, Team team) {
        if (!team.isLeader(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Only the team leader can access settings!");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54,
                ChatColor.DARK_GREEN + "Team Settings");

        addBorderGlass(inv);

        // Friendly fire toggle
        ItemStack ff = createItem(
                team.isFriendlyFire() ? Material.RED_CONCRETE : Material.GREEN_CONCRETE,
                ChatColor.YELLOW + "Friendly Fire",
                ChatColor.GRAY + "Current: " +
                        (team.isFriendlyFire() ? ChatColor.RED + "Enabled" : ChatColor.GREEN + "Disabled"),
                "",
                ChatColor.WHITE + "Click to toggle");
        inv.setItem(20, ff);

        // Change team color
        ItemStack color = createItem(Material.INK_SAC,
                ChatColor.AQUA + "Team Color",
                ChatColor.GRAY + "Current: " + team.getColor() + team.getColor().name(),
                "",
                ChatColor.WHITE + "Click to change");
        inv.setItem(22, color);

        // Change description
        ItemStack desc = createItem(Material.WRITABLE_BOOK,
                ChatColor.LIGHT_PURPLE + "Team Description",
                ChatColor.GRAY + "Current:",
                ChatColor.WHITE + team.getDescription(),
                "",
                ChatColor.WHITE + "Click to change");
        inv.setItem(24, desc);

        // Max members
        ItemStack maxMembers = createItem(Material.PLAYER_HEAD,
                ChatColor.GOLD + "Maximum Members",
                ChatColor.GRAY + "Current: " + ChatColor.WHITE + team.getMaxMembers(),
                "",
                ChatColor.GREEN + "Left click: +1",
                ChatColor.RED + "Right click: -1");
        inv.setItem(31, maxMembers);

        // Transfer leadership
        ItemStack transfer = createItem(Material.GOLDEN_HELMET,
                ChatColor.GOLD + "Transfer Leadership",
                ChatColor.GRAY + "Give leadership to another member",
                "",
                ChatColor.YELLOW + "⚠ Use with caution!");
        inv.setItem(40, transfer);

        // Back button
        ItemStack back = createItem(Material.ARROW,
                ChatColor.YELLOW + "Back",
                ChatColor.GRAY + "Return to team management");
        inv.setItem(45, back);

        player.openInventory(inv);
    }

    private ItemStack createMemberItem(UUID memberId, Team team, Player viewer,
                                       boolean viewerIsLeader, boolean viewerIsMod) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();

        Player member = Bukkit.getPlayer(memberId);
        String memberName = member != null ? member.getName() :
                Bukkit.getOfflinePlayer(memberId).getName();
        if (memberName == null) memberName = "Unknown";

        if (member != null) {
            meta.setOwningPlayer(member);
        }

        boolean isLeader = team.isLeader(memberId);
        boolean isMod = team.isModerator(memberId);

        ChatColor nameColor = isLeader ? ChatColor.GOLD :
                isMod ? ChatColor.AQUA : ChatColor.WHITE;
        meta.setDisplayName(nameColor + memberName);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "━━━━━━━━━━━━━━━━━━━");

        if (isLeader) {
            lore.add(ChatColor.GOLD + "★ Team Leader");
        } else if (isMod) {
            lore.add(ChatColor.AQUA + "◆ Moderator");
        } else {
            lore.add(ChatColor.GREEN + "● Member");
        }

        lore.add("");
        lore.add(ChatColor.YELLOW + "Status: " +
                (member != null && member.isOnline() ?
                        ChatColor.GREEN + "Online" : ChatColor.RED + "Offline"));

        if (!memberId.equals(viewer.getUniqueId())) {
            if (viewerIsLeader && !isLeader) {
                lore.add("");
                if (isMod) {
                    lore.add(ChatColor.RED + "Click to demote");
                } else {
                    lore.add(ChatColor.GREEN + "Click to promote");
                }
                lore.add(ChatColor.DARK_RED + "Shift-click to kick");
            } else if (viewerIsMod && !isMod && !isLeader) {
                lore.add("");
                lore.add(ChatColor.DARK_RED + "Shift-click to kick");
            }
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

        for (int i = 0; i < 9; i++) {
            inv.setItem(i, glass);
            inv.setItem(i + 45, glass);
        }

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

    private String getPlayerName(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            return player.getName();
        }

        String name = Bukkit.getOfflinePlayer(uuid).getName();
        return name != null ? name : "Unknown";
    }
}
