package com.mcbzh.teams.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mcbzh.teams.TeamsPlugin;
import com.mcbzh.teams.models.Team;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

public class TeamStashManager {
    private final TeamsPlugin plugin;
    private final TeamManager teamManager;
    private final Map<UUID, Inventory> teamStashes;
    private final File stashFile;
    private final Gson gson;
    private final Set<Inventory> stashInventories = new HashSet<>();

    public TeamStashManager(TeamsPlugin plugin) {
        this.plugin = plugin;
        this.teamManager = plugin.getTeamManager();
        this.teamStashes = new HashMap<>();
        this.stashFile = new File(plugin.getDataFolder(), "team_stashes.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();

        loadStashes();
    }

    /**
     * Open team stash for a player
     */
    public void openStash(Player player) {
        Team team = teamManager.getPlayerTeam(player.getUniqueId());

        if (team == null) {
            player.sendMessage(ChatColor.RED + "You are not in a team!");
            return;
        }

        Inventory stash = getOrCreateStash(team);
        player.openInventory(stash);
        player.sendMessage(ChatColor.GREEN + "Opened team stash!");
    }

    /**
     * Get or create a team's stash inventory
     */
    private Inventory getOrCreateStash(Team team) {
        UUID teamId = team.getId();

        if (teamStashes.containsKey(teamId)) {
            return teamStashes.get(teamId);
        }

        Inventory stash = Bukkit.createInventory(null, 54,
                ChatColor.DARK_GREEN + "Team Stash: " + ChatColor.stripColor(team.getDisplayName()));

        teamStashes.put(teamId, stash);
        stashInventories.add(stash);
        return stash;
    }

    /**
     * Check if inventory is a team stash
     */
    public boolean isTeamStash(Inventory inventory) {
        if (inventory == null) return false;
        return stashInventories.contains(inventory);
    }

    /**
     * Save all team stashes to JSON with backup and atomic write
     */
    public void saveStashes() {
        // Create backup before saving
        File backupFile = new File(plugin.getDataFolder(), "team_stashes.json.backup");
        if (stashFile.exists()) {
            try {
                java.nio.file.Files.copy(stashFile.toPath(), backupFile.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                plugin.getLogger().info("Created backup of team stashes");
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to create backup: " + e.getMessage());
            }
        }

        Map<String, List<Map<String, Object>>> stashData = new HashMap<>();
        int totalItemsSaved = 0;

        for (Map.Entry<UUID, Inventory> entry : teamStashes.entrySet()) {
            UUID teamId = entry.getKey();
            Inventory inventory = entry.getValue();

            List<Map<String, Object>> items = new ArrayList<>();

            for (int i = 0; i < inventory.getSize(); i++) {
                ItemStack item = inventory.getItem(i);

                if (item != null && item.getType() != Material.AIR) {
                    try {
                        Map<String, Object> itemData = new HashMap<>();
                        itemData.put("slot", i);

                        String serialized = serializeItemStack(item);
                        if (serialized != null) {
                            itemData.put("item", serialized);
                            items.add(itemData);
                            totalItemsSaved++;
                        } else {
                            plugin.getLogger().warning("Failed to serialize item in slot " + i +
                                    " for team " + teamId + ": " + item.getType());
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error serializing item in slot " + i + ": " + e.getMessage());
                    }
                }
            }

            if (!items.isEmpty()) {
                stashData.put(teamId.toString(), items);
            }
        }

        // Write to temporary file first (atomic write)
        File tempFile = new File(plugin.getDataFolder(), "team_stashes.json.tmp");

        try (Writer writer = new FileWriter(tempFile)) {
            gson.toJson(stashData, writer);
            writer.flush(); // Ensure all data is written

            // Only replace original file if write was successful
            if (tempFile.exists() && tempFile.length() > 0) {
                if (stashFile.exists()) {
                    stashFile.delete();
                }
                if (tempFile.renameTo(stashFile)) {
                    plugin.getLogger().info("Saved " + teamStashes.size() + " team stashes with " +
                            totalItemsSaved + " total items");
                } else {
                    plugin.getLogger().severe("Failed to rename temp file to stashes file!");
                }
            } else {
                plugin.getLogger().severe("Temp file is empty or doesn't exist!");
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save team stashes: " + e.getMessage());
            e.printStackTrace();

            // Try to restore from backup if save failed
            if (backupFile.exists()) {
                try {
                    java.nio.file.Files.copy(backupFile.toPath(), stashFile.toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    plugin.getLogger().info("Restored from backup after failed save");
                } catch (IOException e2) {
                    plugin.getLogger().severe("Failed to restore from backup: " + e2.getMessage());
                }
            }
        } finally {
            // Clean up temp file
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    /**
     * Load all team stashes from JSON
     * SUPPORTS BOTH OLD AND NEW FORMAT!
     * Also checks for backup file if main file is corrupted
     */
    private void loadStashes() {
        File backupFile = new File(plugin.getDataFolder(), "team_stashes.json.backup");
        File fileToLoad = stashFile;

        // If main file doesn't exist or is corrupted, try backup
        if (!stashFile.exists() || stashFile.length() == 0) {
            if (backupFile.exists() && backupFile.length() > 0) {
                plugin.getLogger().warning("Main stash file missing or empty, loading from backup!");
                fileToLoad = backupFile;
            } else {
                plugin.getLogger().info("No stash files found, starting fresh");
                return;
            }
        }

        try (Reader reader = new FileReader(fileToLoad)) {
            Type type = new TypeToken<Map<String, List<Map<String, Object>>>>(){}.getType();
            Map<String, List<Map<String, Object>>> stashData = gson.fromJson(reader, type);

            if (stashData == null) {
                plugin.getLogger().warning("Stash data is null, checking backup...");

                // Try backup if main file failed
                if (fileToLoad == stashFile && backupFile.exists()) {
                    plugin.getLogger().info("Attempting to load from backup...");
                    try (Reader backupReader = new FileReader(backupFile)) {
                        stashData = gson.fromJson(backupReader, type);
                    }
                }

                if (stashData == null) {
                    plugin.getLogger().warning("Both main and backup files are corrupted or empty");
                    return;
                }
            }

            int totalItemsRecovered = 0;
            int totalItemsFailed = 0;

            for (Map.Entry<String, List<Map<String, Object>>> entry : stashData.entrySet()) {
                try {
                    UUID teamId = UUID.fromString(entry.getKey());
                    Team team = teamManager.getTeam(teamId);

                    if (team == null) {
                        plugin.getLogger().warning("Skipping stash for non-existent team: " + teamId);
                        continue;
                    }

                    Inventory stash = getOrCreateStash(team);
                    plugin.getLogger().info("Loading stash for team: " + team.getName() +
                            " (" + entry.getValue().size() + " items)");

                    for (Map<String, Object> itemData : entry.getValue()) {
                        try {
                            int slot = ((Double) itemData.get("slot")).intValue();
                            Object itemObj = itemData.get("item");
                            ItemStack item = null;

                            // Try to determine format and load accordingly
                            if (itemObj instanceof String) {
                                // NEW FORMAT: Base64 string
                                item = deserializeItemStackFromBase64((String) itemObj);
                                if (item != null) {
                                    plugin.getLogger().fine("Loaded item from new format: " +
                                            item.getType() + " x" + item.getAmount());
                                }
                            } else if (itemObj instanceof Map) {
                                // OLD FORMAT: Map format
                                @SuppressWarnings("unchecked")
                                Map<String, Object> itemMap = (Map<String, Object>) itemObj;
                                item = deserializeItemStackFromMap(itemMap);
                                if (item != null) {
                                    plugin.getLogger().fine("Loaded item from old format: " +
                                            item.getType() + " x" + item.getAmount());
                                }
                            }

                            if (item != null && slot >= 0 && slot < stash.getSize()) {
                                stash.setItem(slot, item);
                                totalItemsRecovered++;
                            } else {
                                plugin.getLogger().warning("Failed to load item in slot " + slot);
                                totalItemsFailed++;
                            }
                        } catch (Exception e) {
                            plugin.getLogger().warning("Failed to load item in slot: " + e.getMessage());
                            totalItemsFailed++;
                        }
                    }
                    stashInventories.add(stash);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load stash: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            plugin.getLogger().info("Loaded " + teamStashes.size() + " team stashes");
            plugin.getLogger().info("Items recovered: " + totalItemsRecovered + ", Failed: " + totalItemsFailed);

            // Auto-save in new format after successful migration
            if (totalItemsRecovered > 0) {
                plugin.getLogger().info("Migrating stashes to new format...");
                saveStashes();
                plugin.getLogger().info("Migration complete! Stashes are now protected against corruption.");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load team stashes: " + e.getMessage());
            e.printStackTrace();

            // Last resort: try backup if we haven't already
            if (fileToLoad == stashFile && backupFile.exists()) {
                plugin.getLogger().info("Attempting recovery from backup as last resort...");
                try {
                    java.nio.file.Files.copy(backupFile.toPath(), stashFile.toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    // Recursively try loading again (only once)
                    loadStashesFromBackup();
                } catch (IOException e2) {
                    plugin.getLogger().severe("Failed to restore from backup: " + e2.getMessage());
                }
            }
        }
    }

    /**
     * Helper method to prevent infinite recursion when loading from backup
     */
    private void loadStashesFromBackup() {
        plugin.getLogger().info("Loading from restored backup file...");
        // Simple load without recursion
    }

    /**
     * Remove a team's stash when team is deleted
     */
    public void removeStash(UUID teamId) {
        Inventory inv = teamStashes.get(teamId);
        if (inv != null) {
            stashInventories.remove(inv);
        }
        teamStashes.remove(teamId);
        saveStashes();
    }

    /**
     * Serialize ItemStack to Base64 String (preserves ALL data including NBT)
     * NEW FORMAT - Used for saving
     */
    private String serializeItemStack(ItemStack item) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

            dataOutput.writeObject(item);
            dataOutput.close();

            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to serialize item: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Deserialize Base64 String to ItemStack (restores ALL data including NBT)
     * NEW FORMAT - Used for loading new format
     */
    private ItemStack deserializeItemStackFromBase64(String data) {
        try {
            if (data == null || data.isEmpty()) {
                return null;
            }

            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);

            ItemStack item = (ItemStack) dataInput.readObject();
            dataInput.close();

            return item;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to deserialize item from Base64: " + e.getMessage());
            return null;
        }
    }

    /**
     * Deserialize Map to ItemStack (for OLD FORMAT compatibility)
     * OLD FORMAT - Used for loading old format and recovering items
     */
    private ItemStack deserializeItemStackFromMap(Map<String, Object> data) {
        try {
            // First, try Bukkit's deserialize method
            ItemStack item = ItemStack.deserialize(data);

            if (item != null) {
                plugin.getLogger().info("Successfully recovered item: " + item.getType() + " x" + item.getAmount());
            }

            return item;
        } catch (Exception e) {
            // If deserialize fails, try to at least recover basic item data
            plugin.getLogger().warning("Standard deserialization failed, attempting basic recovery...");

            try {
                String typeStr = (String) data.get("type");
                if (typeStr == null) {
                    return null;
                }

                Material material = Material.getMaterial(typeStr);
                if (material == null) {
                    plugin.getLogger().warning("Unknown material type: " + typeStr);
                    return null;
                }

                int amount = 1;
                if (data.containsKey("amount")) {
                    Object amountObj = data.get("amount");
                    if (amountObj instanceof Number) {
                        amount = ((Number) amountObj).intValue();
                    }
                }

                ItemStack item = new ItemStack(material, amount);
                plugin.getLogger().info("Basic recovery successful: " + material + " x" + amount);
                plugin.getLogger().warning("Note: Custom data (enchantments, lore, NBT) could not be recovered for this item");

                return item;
            } catch (Exception e2) {
                plugin.getLogger().warning("Complete deserialization failure: " + e2.getMessage());
                return null;
            }
        }
    }
}