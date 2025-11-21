package com.mcbzh.teams.listeners;

import com.mcbzh.teams.TeamsPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class StashListener implements Listener {
    private final TeamsPlugin plugin;

    public StashListener(TeamsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // Save stash when closed
        if (plugin.getStashManager().isTeamStash(event.getInventory())) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                plugin.getStashManager().saveStashes();
            });
        }
    }
}