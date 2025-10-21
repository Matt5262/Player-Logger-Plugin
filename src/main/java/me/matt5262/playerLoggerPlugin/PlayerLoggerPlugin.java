package me.matt5262.playerLoggerPlugin;

import me.matt5262.playerLoggerPlugin.listeners.PlayerJoinLeaveListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;

public final class PlayerLoggerPlugin extends JavaPlugin {

    private File logFile;

    @Override
    public void onEnable() {
        // Ensure plugin data folder exists
        File dataFolder = getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            // tries to create the folder that contains all the files like config and stuff, if it doesn't exist of course.
            // If creating the folder fail (mkdirs) then log the following message.
            getLogger().severe("Could not create plugin data folder: " + dataFolder.getAbsolutePath());
            // Not returning/disable for now — adjust as needed
        }

        // Create /logs directory and log file using java.nio
        Path logsDir = dataFolder.toPath().resolve("logs");
        // Path is a path or a modern file type, MAYBE. Resolve .resolve("logs") adds "logs" onto that path → /plugins/PlayerLoggerPlugin/logs
        try {
            Files.createDirectories(logsDir);
            // A modern mkdirs?
            // (ChatGPT said: Creates the directory (and parents) if missing — safe and idempotent (you can call it multiple times without errors).
            // If it fails (e.g., permission denied), it throws IOException.)
            Path logPath = logsDir.resolve("join_leave_log.txt");
            // Adds the "join_leave_log.txt" to the path.
            if (!Files.exists(logPath)) {
                Files.createFile(logPath);
            }
            this.logFile = logPath.toFile();
            // Converts the Path back into a File (since Bukkit’s older APIs use File).
            // Stores it in your logFile variable for later access (your listener will use this to append log entries).
        } catch (IOException e) {
            // Catches any file creation error.
            getLogger().log(Level.SEVERE, "Failed to setup logs directory/file", e);
        }

        // Register listener
        getServer().getPluginManager().registerEvents(new PlayerJoinLeaveListener(this), this);
        getLogger().info("PlayerLoggerPlugin enabled! The plugin is still work in progress.");
    }

    public File getLogFile() {
        return logFile;
    }
    // This will be used in the listener so if that the getLogFIle gets called inside the listener
    // it will return logFile variable which is the path to the log file.
}
