package me.matt5262.playerLoggerPlugin;

import me.matt5262.playerLoggerPlugin.commands.AvgPlayersCommand;
import me.matt5262.playerLoggerPlugin.listeners.PlayerJoinLeaveListener;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

public final class PlayerLoggerPlugin extends JavaPlugin {

    private File logFile;
    private Connection connection;
    public boolean isShuttingDown = false;
    private PlayerJoinLeaveListener listenerInstance;

    @Override
    public void onEnable() {
        // Initialize database first
        setupDatabase();

        // Create /logs folder and log file
        setupLogFile();

        // Register listener
        listenerInstance = new PlayerJoinLeaveListener(this);
        getServer().getPluginManager().registerEvents(listenerInstance, this);

        // Register command
        if (getCommand("avgplayers") != null) {
            getCommand("avgplayers").setExecutor(new AvgPlayersCommand(this));
        } else {
            getLogger().warning("Command /avgplayers is not defined in plugin.yml!");
        }

        getLogger().info("PlayerLoggerPlugin enabled!");
    }

    @Override
    public void onDisable() {
        isShuttingDown = true;
        getLogger().info("Saving any remaining players before shutdown...");

        // Log all players that are still online before shutdown
        for (Player player : getServer().getOnlinePlayers()) {
            if (listenerInstance != null) {
                listenerInstance.handleServerShutdown(player);
            }
        }

        // Close the database connection safely
        if (connection != null) {
            try {
                connection.close();
                getLogger().info("Database connection closed.");
            } catch (SQLException e) {
                getLogger().log(Level.SEVERE, "Failed to close database connection", e);
            }
        }

        getLogger().info("PlayerLoggerPlugin disabled!");
    }

    public File getLogFile() {
        return logFile;
    }

    public Connection getConnection() {
        return connection;
    }

    private void setupDatabase() {
        try {
            File dbFile = new File(getDataFolder(), "playtime.db");
            if (!dbFile.getParentFile().exists() && !dbFile.getParentFile().mkdirs()) {
                getLogger().severe("Failed to create plugin folder for database: " + dbFile.getAbsolutePath());
            }

            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

            // Create tables if not exist
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS playtime (" +
                                "uuid TEXT PRIMARY KEY," +
                                "total_ms INTEGER NOT NULL" +
                                ");"
                );
                stmt.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS daily_logins (" +
                                "date TEXT NOT NULL," +
                                "uuid TEXT NOT NULL," +
                                "PRIMARY KEY (date, uuid)" +
                                ");"
                );
            }

        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Failed to setup database", e);
        }
    }

    private void setupLogFile() {
        File dataFolder = getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            getLogger().severe("Could not create plugin data folder: " + dataFolder.getAbsolutePath());
        }

        Path logsDir = dataFolder.toPath().resolve("logs");
        try {
            Files.createDirectories(logsDir);
            Path logPath = logsDir.resolve("join_leave_log.txt");
            if (!Files.exists(logPath)) {
                Files.createFile(logPath);
            }
            this.logFile = logPath.toFile();
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Failed to setup logs directory/file", e);
        }
    }
}
