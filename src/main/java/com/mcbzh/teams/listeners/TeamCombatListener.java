package com.mcbzh.teams.listeners;

import com.mcbzh.teams.TeamsPlugin;
import com.mcbzh.teams.managers.TeamManager;
import com.mcbzh.teams.models.Team;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class TeamCombatListener implements Listener {
    private final TeamsPlugin plugin;
    private final TeamManager teamManager;

    public TeamCombatListener(TeamsPlugin plugin) {
        this.plugin = plugin;
        this.teamManager = plugin.getTeamManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Update player's nametag after a short delay
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (plugin.getNametagManager() != null) {
                plugin.getNametagManager().updatePlayer(player);
            }
        }, 10L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clean up player's scoreboard entry
        // This happens automatically, but we can do cleanup if needed
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Only handle player vs player damage
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player victim = (Player) event.getEntity();
        Player attacker = null;

        // Direct attack
        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        }
        // Projectile attack
        else if (event.getDamager() instanceof Projectile) {
            Projectile projectile = (Projectile) event.getDamager();
            if (projectile.getShooter() instanceof Player) {
                attacker = (Player) projectile.getShooter();
            }
        }

        if (attacker == null) {
            return;
        }

        // Check if they're teammates
        if (teamManager.areTeammates(attacker.getUniqueId(), victim.getUniqueId())) {
            Team team = teamManager.getPlayerTeam(attacker.getUniqueId());

            if (team != null && !team.isFriendlyFire()) {
                // Cancel the damage
                event.setCancelled(true);

                // Optional: notify attacker
                if (plugin.getConfig().getBoolean("friendly-fire.show-message", true)) {
                    attacker.sendMessage(ChatColor.RED + "You cannot hurt your teammate!");
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!plugin.getConfig().getBoolean("statistics.enabled", true)) {
            return;
        }

        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        // Track death for victim's team
        Team victimTeam = teamManager.getPlayerTeam(victim.getUniqueId());
        if (victimTeam != null) {
            victimTeam.addDeath();
        }

        // Track kill for killer's team (if PvP)
        if (killer != null) {
            Team killerTeam = teamManager.getPlayerTeam(killer.getUniqueId());

            // Only count if not same team or friendly fire is enabled
            if (killerTeam != null &&
                    (victimTeam == null || !killerTeam.equals(victimTeam) || killerTeam.isFriendlyFire())) {

                // Only count PvP kills if configured
                if (!plugin.getConfig().getBoolean("statistics.pvp-only", true) || victim != null) {
                    killerTeam.addKill();
                }
            }
        }

        // Save changes
        teamManager.saveTeams();
    }
}
