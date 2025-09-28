package com.github.bradleywoodrs.mangoplaceholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ScoreboardExpansion extends PlaceholderExpansion {

    private final JavaPlugin plugin;

    public ScoreboardExpansion(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "mango";
    }

    @Override
    public String getAuthor() {
        return "bradleywoodrs";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (identifier.equalsIgnoreCase("world_days")) {
            World world = Bukkit.getWorld("world");
            if (world == null) return "0";
            long worldDays = world.getFullTime() / 24000L;
            return String.valueOf(worldDays);
        }

        if (identifier.equalsIgnoreCase("player_days")) {
            if (player == null) return "0";
            long daysPlayed = player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 24000L;
            return String.valueOf(daysPlayed);
        }

        return null;
    }
}
