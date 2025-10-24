package me.matt5262.playerLoggerPlugin.commands;

import me.matt5262.playerLoggerPlugin.PlayerLoggerPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AvgPlayersCommand implements CommandExecutor {

    private final PlayerLoggerPlugin plugin;

    public AvgPlayersCommand(PlayerLoggerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Connection conn = plugin.getConnection();
        if (conn == null) {
            sender.sendMessage("§c⚠ Database connection is not available right now.");
            plugin.getLogger().severe("AvgPlayersCommand could not obtain a database connection.");
            return true;
        }

        try (PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(DISTINCT uuid) as players, COUNT(DISTINCT date) as days FROM daily_logins"
             );
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                int totalPlayers = rs.getInt("players");
                int totalDays = rs.getInt("days");

                if (totalDays == 0) {
                    sender.sendMessage("§cNo daily data yet — try again later!");
                    return true;
                }

                double avg = (double) totalPlayers / totalDays;
                sender.sendMessage("§a📊 Average unique players per day: §e" + String.format("%.2f", avg));
                sender.sendMessage("§7(" + totalPlayers + " total players over " + totalDays + " days)");
            }

        } catch (SQLException e) {
            sender.sendMessage("§c⚠ Database error: " + e.getMessage());
            plugin.getLogger().severe("Failed to get average players: " + e.getMessage());
        }

        return true;
    }
}
