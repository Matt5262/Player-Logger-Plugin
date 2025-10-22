package me.matt5262.playerLoggerPlugin.listeners;

import me.matt5262.playerLoggerPlugin.PlayerLoggerPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

public class PlayerJoinLeaveListener implements Listener {

    private final PlayerLoggerPlugin plugin;
    private final Map<UUID, Long> sessionStartTimes = new HashMap<>();

    public PlayerJoinLeaveListener(PlayerLoggerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        sessionStartTimes.put(uuid, System.currentTimeMillis());

        // Get player IP safely
        String ip = player.getAddress() != null
                ? player.getAddress().getAddress().getHostAddress()
                : "Unknown IP";

        long totalPlaytime = getTotalPlaytime(uuid);
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        // Format: [JOIN] Name | UUID | IP | Total playtime
        logToFile(String.format("[%s] [JOIN] %s | %s | %s | Total: %s",
                time, player.getName(), uuid, ip, formatPlaytime(totalPlaytime)));

        // Record daily login
        recordDailyLogin(uuid);
    }

    private void recordDailyLogin(UUID uuid) {
        String today = java.time.LocalDate.now().toString();
        Connection conn = plugin.getConnection(); // DO NOT close this connection!
        if (conn == null) {
            plugin.getLogger().severe("Database connection is null, cannot record daily login.");
            return;
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR IGNORE INTO daily_logins (date, uuid) VALUES (?, ?)"
        )) {
            ps.setString(1, today);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to record daily login for " + uuid + ": " + e.getMessage());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (plugin.isShuttingDown) return; // skip normal quit logging
        handlePlayerLeave(event.getPlayer(), "LEFT");
    }

    public void handleServerShutdown(Player player) {
        handlePlayerLeave(player, "LEFT (SHUTDOWN)");
    }

    private void handlePlayerLeave(Player player, String eventLabel) {
        UUID uuid = player.getUniqueId();
        long joinTime = sessionStartTimes.getOrDefault(uuid, System.currentTimeMillis());
        long sessionDuration = System.currentTimeMillis() - joinTime;

        // Update total playtime in the database
        updateTotalPlaytime(uuid, sessionDuration);

        long totalPlaytime = getTotalPlaytime(uuid);
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String ip = player.getAddress() != null
                ? player.getAddress().getAddress().getHostAddress()
                : "Unknown IP";

        logToFile(String.format("[%s] [%s] %s | %s | %s | Total: %s | Session: %s",
                time, eventLabel, player.getName(), uuid, ip,
                formatPlaytime(totalPlaytime),
                formatPlaytime(sessionDuration)));
    }

    private void updateTotalPlaytime(UUID uuid, long sessionDuration) {
        Connection conn = plugin.getConnection(); // DO NOT close this connection!
        if (conn == null) {
            plugin.getLogger().severe("Database connection is null, cannot update playtime for " + uuid);
            return;
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO playtime (uuid, total_ms) VALUES (?, ?) " +
                        "ON CONFLICT(uuid) DO UPDATE SET total_ms = total_ms + ?"
        )) {
            ps.setString(1, uuid.toString());
            ps.setLong(2, sessionDuration);
            ps.setLong(3, sessionDuration);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to update total playtime for " + uuid + ": " + e.getMessage());
        }
    }

    private long getTotalPlaytime(UUID uuid) {
        Connection conn = plugin.getConnection();
        if (conn == null) {
            plugin.getLogger().severe("Database connection is null, cannot get total playtime for " + uuid);
            return 0L;
        }

        try (PreparedStatement ps = conn.prepareStatement("SELECT total_ms FROM playtime WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("total_ms");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get total playtime for " + uuid + ": " + e.getMessage());
        }
        return 0L;
    }

    private void logToFile(String message) {
        try (FileWriter writer = new FileWriter(plugin.getLogFile(), true)) {
            writer.write(message + System.lineSeparator());
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to write to log file: " + e.getMessage());
        }
    }

    private String formatPlaytime(long ms) {
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        seconds %= 60;
        minutes %= 60;

        if (hours > 0)
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        else if (minutes > 0)
            return String.format("%dm %ds", minutes, seconds);
        else
            return String.format("%ds", seconds);
    }
}
