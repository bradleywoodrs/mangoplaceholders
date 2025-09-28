package com.github.bradleywoodrs.mangoplaceholders;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class Mangoplaceholders extends JavaPlugin {

    private JobsExpansion expansion;

    @Override
    public void onEnable() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            expansion = new JobsExpansion(this);
            expansion.updateLeaderboard();
            expansion.register();
            Bukkit.getPluginManager().registerEvents(expansion, this);
            new ScoreboardExpansion(this).register();
        }
    }

    @Override
    public void onDisable() {
        if (expansion != null) {
            expansion.clearLeaderboard();
        }
    }
}
