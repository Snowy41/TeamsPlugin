package com.mcbzh.teams.models;

import org.bukkit.ChatColor;

import java.util.*;
import java.util.stream.Collectors;

public class Team {
    private final UUID id;
    private String name;
    private String displayName;
    private String tag;
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

    // Ally System
    private final Set<UUID> allies; // Team IDs that are allies
    private final Map<UUID, Long> allyInvites; // Pending ally invitations (Team ID -> timestamp)
    private final AllyPermissions allyPermissions; // Permissions for allies

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

        // Initialize ally system
        this.allies = new HashSet<>();
        this.allyInvites = new HashMap<>();
        this.allyPermissions = new AllyPermissions();
    }

    // Ally Management
    public boolean addAlly(UUID teamId) {
        allyInvites.remove(teamId);
        return allies.add(teamId);
    }

    public boolean removeAlly(UUID teamId) {
        return allies.remove(teamId);
    }

    public boolean isAlly(UUID teamId) {
        return allies.contains(teamId);
    }

    public Set<UUID> getAllies() {
        return new HashSet<>(allies);
    }

    public void sendAllyInvite(UUID teamId) {
        allyInvites.put(teamId, System.currentTimeMillis());
    }

    public boolean hasAllyInvite(UUID teamId) {
        if (!allyInvites.containsKey(teamId)) {
            return false;
        }
        // Invitations expire after 5 minutes
        long inviteTime = allyInvites.get(teamId);
        if (System.currentTimeMillis() - inviteTime > 300000) {
            allyInvites.remove(teamId);
            return false;
        }
        return true;
    }

    public void removeAllyInvite(UUID teamId) {
        allyInvites.remove(teamId);
    }

    public AllyPermissions getAllyPermissions() {
        return allyPermissions;
    }

    // Existing getters and setters
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
            return false;
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

        // Serialize ally data
        data.put("allies", allies.stream().map(UUID::toString).collect(Collectors.toList()));
        data.put("allyPermissions", allyPermissions.serialize());

        return data;
    }

    public static Team deserialize(Map<String, Object> data) {
        UUID leader = UUID.fromString((String) data.get("leader"));
        Team team = new Team((String) data.get("name"), leader);

        if (data.containsKey("id")) {
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

        // Deserialize ally data
        if (data.containsKey("allies")) {
            @SuppressWarnings("unchecked")
            List<String> alliesList = (List<String>) data.get("allies");
            alliesList.forEach(uuid -> team.allies.add(UUID.fromString(uuid)));
        }

        if (data.containsKey("allyPermissions")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> permsData = (Map<String, Object>) data.get("allyPermissions");
            team.allyPermissions.deserialize(permsData);
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

    // Inner class for ally permissions
    public static class AllyPermissions {
        private boolean canBreakBlocks;
        private boolean canPlaceBlocks;
        private boolean canUseContainers;
        private boolean canUseDoors;
        private boolean canInteractEntities;
        private boolean canUseBuckets;
        private boolean canUseButtons;

        public AllyPermissions() {
            // Default permissions - more restrictive
            this.canBreakBlocks = false;
            this.canPlaceBlocks = false;
            this.canUseContainers = false;
            this.canUseDoors = true;
            this.canInteractEntities = false;
            this.canUseBuckets = false;
            this.canUseButtons = true;
        }

        // Getters and setters
        public boolean canBreakBlocks() { return canBreakBlocks; }
        public void setCanBreakBlocks(boolean canBreakBlocks) { this.canBreakBlocks = canBreakBlocks; }

        public boolean canPlaceBlocks() { return canPlaceBlocks; }
        public void setCanPlaceBlocks(boolean canPlaceBlocks) { this.canPlaceBlocks = canPlaceBlocks; }

        public boolean canUseContainers() { return canUseContainers; }
        public void setCanUseContainers(boolean canUseContainers) { this.canUseContainers = canUseContainers; }

        public boolean canUseDoors() { return canUseDoors; }
        public void setCanUseDoors(boolean canUseDoors) { this.canUseDoors = canUseDoors; }

        public boolean canInteractEntities() { return canInteractEntities; }
        public void setCanInteractEntities(boolean canInteractEntities) { this.canInteractEntities = canInteractEntities; }

        public boolean canUseBuckets() { return canUseBuckets; }
        public void setCanUseBuckets(boolean canUseBuckets) { this.canUseBuckets = canUseBuckets; }

        public boolean canUseButtons() { return canUseButtons; }
        public void setCanUseButtons(boolean canUseButtons) { this.canUseButtons = canUseButtons; }

        public Map<String, Object> serialize() {
            Map<String, Object> data = new HashMap<>();
            data.put("canBreakBlocks", canBreakBlocks);
            data.put("canPlaceBlocks", canPlaceBlocks);
            data.put("canUseContainers", canUseContainers);
            data.put("canUseDoors", canUseDoors);
            data.put("canInteractEntities", canInteractEntities);
            data.put("canUseBuckets", canUseBuckets);
            data.put("canUseButtons", canUseButtons);
            return data;
        }

        public void deserialize(Map<String, Object> data) {
            this.canBreakBlocks = (Boolean) data.getOrDefault("canBreakBlocks", false);
            this.canPlaceBlocks = (Boolean) data.getOrDefault("canPlaceBlocks", false);
            this.canUseContainers = (Boolean) data.getOrDefault("canUseContainers", false);
            this.canUseDoors = (Boolean) data.getOrDefault("canUseDoors", true);
            this.canInteractEntities = (Boolean) data.getOrDefault("canInteractEntities", false);
            this.canUseBuckets = (Boolean) data.getOrDefault("canUseBuckets", false);
            this.canUseButtons = (Boolean) data.getOrDefault("canUseButtons", true);
        }
    }
}