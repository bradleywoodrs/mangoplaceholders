package com.github.bradleywoodrs.mangoplaceholders;

import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.container.Job;
import com.github.bradleywoodrs.mangoplaceholders.db.DatabaseManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.*;

public class JobsExpansion extends PlaceholderExpansion {

    private final JavaPlugin plugin;
    private final List<JobEntry> leaderboardCache = new ArrayList<>();

    public JobsExpansion(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() { return "jobs"; }

    @Override
    public @NotNull String getAuthor() { return "bradleywoodrs"; }

    @Override
    public @NotNull String getVersion() { return plugin.getDescription().getVersion(); }

    @Override
    public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (!(params.startsWith("toplevel_levels_") || params.startsWith("toplevel_name_"))) return null;
        String[] parts = params.split("_");
        if (parts.length != 3) return "";

        int pos;
        try { pos = Integer.parseInt(parts[2]) - 1; }
        catch (NumberFormatException e) { return ""; }

        if (pos < 0 || pos >= leaderboardCache.size()) return "";

        JobEntry entry = leaderboardCache.get(pos);
        return params.startsWith("toplevel_levels_") ? entry.jobName + " " + entry.level : entry.playerName;
    }

    public void updateLeaderboard() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<JobEntry> topJobs = new ArrayList<>();
            String sql = "SELECT u.username, j.job, j.level " +
                    "FROM jobs j " +
                    "JOIN users u ON u.id = j.userid " +
                    "ORDER BY j.level DESC " +
                    "LIMIT 10";

            try (Connection conn = DatabaseManager.getConnection();
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

            boolean changed = leaderboardCache.size() != topJobs.size();
            if (!changed) {
                for (int i = 0; i < topJobs.size(); i++) {
                    if (!leaderboardCache.get(i).equals(topJobs.get(i))) {
                        changed = true;
                        break;
                    }
                }
            }

            if (changed) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    leaderboardCache.clear();
                    leaderboardCache.addAll(topJobs);
                });
            }
        });
    }

    public void clearLeaderboard() { leaderboardCache.clear(); }

    private static class JobEntry {
        final String playerName;
        final String jobName;
        final int level;

        JobEntry(String playerName, String jobName, int level) {
            this.playerName = playerName;
            this.jobName = jobName;
            this.level = level;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof JobEntry)) return false;
            JobEntry other = (JobEntry) obj;
            return playerName.equals(other.playerName) &&
                    jobName.equals(other.jobName) &&
                    level == other.level;
        }

        @Override
        public int hashCode() {
            return Objects.hash(playerName, jobName, level);
        }
    }
}
