package com.github.bradleywoodrs.mangoplaceholders;

import com.gamingmesh.jobs.Jobs;
import com.github.bradleywoodrs.mangoplaceholders.db.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class Mangoplaceholders extends JavaPlugin{

    private JobsExpansion expansion;

    @Override
    public void onEnable() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            File dbFile = new File(JavaPlugin.getPlugin(Jobs.class).getDataFolder(), "jobs.sqlite.db");
            DatabaseManager.init(dbFile);
            expansion = new JobsExpansion(this);
            expansion.updateLeaderboard();
            expansion.register();
            new ScoreboardExpansion(this).register();
            Bukkit.getScheduler().runTaskTimerAsynchronously(this,() ->
                expansion.updateLeaderboard(),
                0L,
                20L * 120);
        }
    }

    @Override
    public void onDisable() {
        if (expansion != null) {
            expansion.clearLeaderboard();
        }
        DatabaseManager.close();
    }
}
