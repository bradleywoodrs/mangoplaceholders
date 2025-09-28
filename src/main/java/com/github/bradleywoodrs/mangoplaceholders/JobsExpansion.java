package com.github.bradleywoodrs.mangoplaceholders;

import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.api.JobsJoinEvent;
import com.gamingmesh.jobs.api.JobsLeaveEvent;
import com.gamingmesh.jobs.api.JobsLevelUpEvent;
import com.gamingmesh.jobs.container.Job;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JobsExpansion extends PlaceholderExpansion implements Listener {

    private final JavaPlugin plugin;
    private final List<JobEntry> leaderboardCache = new ArrayList<>();
    private boolean leaderboardUpdateScheduled = false;
    private boolean leaderboardUpdatePending = false;

    public JobsExpansion(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "jobs";
    }

    @Override
    public @NotNull String getAuthor() {
        return "bradleywoodrs";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (!(params.startsWith("toplevel_levels_") || params.startsWith("toplevel_name_"))) return null;

        String[] parts = params.split("_");
        if (parts.length != 3) return "";

        int pos;
        try {
            pos = Integer.parseInt(parts[2]) - 1;
        } catch (NumberFormatException e) {
            return "";
        }

        if (pos < 0 || pos >= leaderboardCache.size()) return "";

        JobEntry entry = leaderboardCache.get(pos);

        if (params.startsWith("toplevel_levels_")) {
            return entry.jobName + " " + entry.level;
        } else {
            return entry.playerName;
        }
    }

    private void scheduleLeaderboardUpdate() {
        if (leaderboardUpdateScheduled) {
            leaderboardUpdatePending = true;
            return;
        }

        leaderboardUpdateScheduled = true;

        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            updateLeaderboard();

            if (leaderboardUpdatePending) {
                leaderboardUpdatePending = false;
                scheduleLeaderboardUpdate();
            } else {
                leaderboardUpdateScheduled = false;
            }
        }, 20L);
    }


    public void updateLeaderboard() {
        File dbFile = new File(JavaPlugin.getPlugin(Jobs.class).getDataFolder(), "jobs.sqlite.db");
        if (!dbFile.exists()) return;

        List<JobEntry> topJobs = new ArrayList<>();
        String sql = "SELECT u.username, j.job, j.level " +
                "FROM jobs j " +
                "JOIN users u ON u.id = j.userid " +
                "ORDER BY j.level DESC " +
                "LIMIT 10";

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String playerName = rs.getString("username");
                String jobName = rs.getString("job");
                int level = rs.getInt("level");

                Job job = Jobs.getJob(jobName);
                String coloredJob = job != null ? job.getChatColor() + job.getDisplayName() : jobName;
                String coloredPlayer = job != null ? job.getChatColor() + playerName : playerName;

                topJobs.add(new JobEntry(coloredPlayer, coloredJob, level));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            leaderboardCache.clear();
            leaderboardCache.addAll(topJobs);
        });
    }

    public void clearLeaderboard() {
        leaderboardCache.clear();
    }

    @EventHandler
    public void onJobLevelUp(JobsLevelUpEvent event) {
        scheduleLeaderboardUpdate();
    }

    @EventHandler
    public void onJobJoin(JobsJoinEvent event) {
        scheduleLeaderboardUpdate();
    }

    @EventHandler
    public void onJobLeave(JobsLeaveEvent event) {
        scheduleLeaderboardUpdate();
    }

    private static class JobEntry {
        final String playerName;
        final String jobName;
        final int level;

        JobEntry(String playerName, String jobName, int level) {
            this.playerName = playerName;
            this.jobName = jobName;
            this.level = level;
        }
    }
}