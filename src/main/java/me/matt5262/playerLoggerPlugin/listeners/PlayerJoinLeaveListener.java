package me.matt5262.playerLoggerPlugin.listeners;

import me.matt5262.playerLoggerPlugin.PlayerLoggerPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PlayerJoinLeaveListener implements Listener {

    private final PlayerLoggerPlugin plugin;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public PlayerJoinLeaveListener(PlayerLoggerPlugin plugin) {
        this.plugin = plugin;
    }

    private void log(String message) {
        try (FileWriter writer = new FileWriter(plugin.getLogFile(), true)) {
            writer.write(message + System.lineSeparator());
            // normally you would writer.close()
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to write to log file: " + e.getMessage());
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String name = event.getPlayer().getName();
        String uuid = event.getPlayer().getUniqueId().toString();
        String ip = event.getPlayer().getAddress() != null
                ? event.getPlayer().getAddress().getAddress().getHostAddress()
                // ? means if true
                // : means if false
                : "Unknown IP";
        String time = dateFormat.format(new Date());

        log("[JOIN] " + name + " | UUID: " + uuid + " | IP: " + ip + " | Time: " + time);

    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        String name = event.getPlayer().getName();
        String uuid = event.getPlayer().getUniqueId().toString();
        String ip = event.getPlayer().getAddress() != null
                ? event.getPlayer().getAddress().getAddress().getHostAddress()
                : "Unknown IP";
        String time = dateFormat.format(new Date());

        log("[LEAVE] " + name + " | UUID: " + uuid + " | IP: " + ip + " | Time: " + time);
    }
}
