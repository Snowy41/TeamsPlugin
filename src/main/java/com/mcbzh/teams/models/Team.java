package com.mcbzh.teams.models;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;

import java.util.*;
import java.util.stream.Collectors;

public class Team {
    private final UUID id;
    private String name;
    private String displayName;
    private String tag; // New: Team tag (e.g., "[RED]")
    private ChatColor color;
    private UUID leader;
    private final Set<UUID> members;
    private final Set<UUID> moderators;
    private final Map<UUID, Long> invitations;
    private String description;
    private boolean friendlyFire;
    private boolean allowAlliances;
    private final long createdAt;
    private int maxMembers;

    // Statistics
    private int totalKills;
    private int totalDeaths;

    public Team(String name, UUID leader) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.displayName = name;
        this.leader = leader;
        this.members = new HashSet<>();
        this.moderators = new HashSet<>();
        this.invitations = new HashMap<>();
        this.members.add(leader);
        this.color = ChatColor.WHITE;
        this.description = "A new team";
        this.friendlyFire = false;
        this.allowAlliances = true;
        this.createdAt = System.currentTimeMillis();
        this.maxMembers = 10;
        this.totalKills = 0;
        this.totalDeaths = 0;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }

    public ChatColor getColor() { return color; }
    public void setColor(ChatColor color) { this.color = color; }

    public String getColoredName() {
        return color + displayName + ChatColor.RESET;
    }

    public String getFormattedPrefix() {
        return color + "[" + displayName + "]" + ChatColor.RESET + " ";
    }

    public String getColoredTag() {
        if (tag != null && !tag.isEmpty()) {
            return color + tag + ChatColor.RESET;
        }
        return color + "[" + displayName + "]" + ChatColor.RESET;
    }

    public UUID getLeader() { return leader; }
    public void setLeader(UUID leader) {
        this.leader = leader;
        if (!members.contains(leader)) {
            members.add(leader);
        }
    }

    public Set<UUID> getMembers() { return new HashSet<>(members); }
    public Set<UUID> getModerators() { return new HashSet<>(moderators); }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isFriendlyFire() { return friendlyFire; }
    public void setFriendlyFire(boolean friendlyFire) { this.friendlyFire = friendlyFire; }

    public boolean isAllowAlliances() { return allowAlliances; }
    public void setAllowAlliances(boolean allowAlliances) { this.allowAlliances = allowAlliances; }

    public long getCreatedAt() { return createdAt; }

    public int getMaxMembers() { return maxMembers; }
    public void setMaxMembers(int maxMembers) { this.maxMembers = maxMembers; }

    public int getTotalKills() { return totalKills; }
    public void addKill() { this.totalKills++; }

    public int getTotalDeaths() { return totalDeaths; }
    public void addDeath() { this.totalDeaths++; }

    // Member Management
    public boolean addMember(UUID player) {
        if (members.size() >= maxMembers) {
            return false;
        }
        invitations.remove(player);
        return members.add(player);
    }

    public boolean removeMember(UUID player) {
        if (player.equals(leader)) {
            return false; // Cannot remove leader
        }
        moderators.remove(player);
        return members.remove(player);
    }

    public boolean isMember(UUID player) {
        return members.contains(player);
    }

    public boolean isLeader(UUID player) {
        return leader.equals(player);
    }

    public boolean isModerator(UUID player) {
        return moderators.contains(player) || isLeader(player);
    }

    public boolean addModerator(UUID player) {
        if (!isMember(player) || isLeader(player)) {
            return false;
        }
        return moderators.add(player);
    }

    public boolean removeModerator(UUID player) {
        return moderators.remove(player);
    }

    // Invitation Management
    public void addInvitation(UUID player) {
        invitations.put(player, System.currentTimeMillis());
    }

    public boolean hasInvitation(UUID player) {
        if (!invitations.containsKey(player)) {
            return false;
        }
        // Invitations expire after 5 minutes
        long inviteTime = invitations.get(player);
        if (System.currentTimeMillis() - inviteTime > 300000) {
            invitations.remove(player);
            return false;
        }
        return true;
    }

    public void removeInvitation(UUID player) {
        invitations.remove(player);
    }

    public int getMemberCount() {
        return members.size();
    }

    public boolean isFull() {
        return members.size() >= maxMembers;
    }

    public double getKDRatio() {
        if (totalDeaths == 0) {
            return totalKills;
        }
        return (double) totalKills / totalDeaths;
    }

    // Serialization helpers
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("id", id.toString());
        data.put("name", name);
        data.put("displayName", displayName);
        data.put("tag", tag);
        data.put("color", color.name());
        data.put("leader", leader.toString());
        data.put("members", members.stream().map(UUID::toString).collect(Collectors.toList()));
        data.put("moderators", moderators.stream().map(UUID::toString).collect(Collectors.toList()));
        data.put("description", description);
        data.put("friendlyFire", friendlyFire);
        data.put("allowAlliances", allowAlliances);
        data.put("createdAt", createdAt);
        data.put("maxMembers", maxMembers);
        data.put("totalKills", totalKills);
        data.put("totalDeaths", totalDeaths);
        return data;
    }

    public static Team deserialize(Map<String, Object> data) {
        UUID leader = UUID.fromString((String) data.get("leader"));
        Team team = new Team((String) data.get("name"), leader);

        if (data.containsKey("id")) {
            // Use reflection to set the final id field
            try {
                java.lang.reflect.Field idField = Team.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(team, UUID.fromString((String) data.get("id")));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        team.setDisplayName((String) data.get("displayName"));
        if (data.containsKey("tag")) {
            team.setTag((String) data.get("tag"));
        }
        team.setColor(ChatColor.valueOf((String) data.get("color")));
        team.setDescription((String) data.getOrDefault("description", "A new team"));
        team.setFriendlyFire((Boolean) data.getOrDefault("friendlyFire", false));
        team.setAllowAlliances((Boolean) data.getOrDefault("allowAlliances", true));
        team.setMaxMembers((Integer) data.getOrDefault("maxMembers", 10));

        if (data.containsKey("totalKills")) {
            team.totalKills = (Integer) data.get("totalKills");
        }
        if (data.containsKey("totalDeaths")) {
            team.totalDeaths = (Integer) data.get("totalDeaths");
        }

        @SuppressWarnings("unchecked")
        List<String> membersList = (List<String>) data.get("members");
        team.members.clear();
        membersList.forEach(uuid -> team.members.add(UUID.fromString(uuid)));

        if (data.containsKey("moderators")) {
            @SuppressWarnings("unchecked")
            List<String> modsList = (List<String>) data.get("moderators");
            modsList.forEach(uuid -> team.moderators.add(UUID.fromString(uuid)));
        }

        return team;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Team team = (Team) o;
        return id.equals(team.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
