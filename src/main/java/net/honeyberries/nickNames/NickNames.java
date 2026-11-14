package net.honeyberries.nickNames;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * NickNames main plugin class.
 *
 * <p>Opens the SQLite nickname database, registers the {@code /nick} and {@code /unnick}
 * commands through Paper's Brigadier command API during the {@link LifecycleEvents#COMMANDS}
 * lifecycle event, and restores saved nicknames when players join. Because the commands are
 * registered programmatically they do not need to be declared in {@code plugin.yml}.</p>
 *
 * <p>Disables itself if the database cannot be opened.</p>
 *
 * @author HoneyBerries
 * @author Pepmon270
 */
public final class NickNames extends JavaPlugin {

    /**
     * The SQLite-backed nickname store, opened in {@link #onEnable()} and closed in
     * {@link #onDisable()}. {@code null} until the database has been successfully opened.
     */
    @Nullable
    private NicknameRepository repository;

    /**
     * Opens the nickname database ({@code nicknames.db} in the plugin's data folder), registers
     * the Brigadier commands and the join listener, and logs that the plugin is up. If the
     * database cannot be opened, the error is logged and this plugin is disabled.
     */
    @Override
    public void onEnable() {
        NicknameRepository repo;
        try {
            Files.createDirectories(getDataFolder().toPath());
            repo = new NicknameRepository(getDataFolder().toPath().resolve("nicknames.db"));
        } catch (IOException | SQLException e) {
            getLogger().log(Level.SEVERE, "Could not open the nickname database; disabling.", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        repository = repo;

        getServer().getPluginManager().registerEvents(new NicknameListener(this, repo), this);

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register(
                    NickCommand.create(this, repo),
                    "Set your nickname (supports MiniMessage formatting)"
            );
            event.registrar().register(
                    UnnickCommand.create(this, repo),
                    "Remove your nickname"
            );
        });

        getLogger().info("NickNames enabled!");
    }

    /**
     * Closes the nickname database if it was opened, then logs that the plugin has been disabled.
     */
    @Override
    public void onDisable() {
        if (repository != null) {
            repository.close();
        }
        getLogger().info("NickNames disabled!");
    }
}
