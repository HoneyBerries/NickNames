package net.honeyberries.nickNames;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

/**
 * Re-applies a player's saved nickname when they join the server.
 *
 * <p>Without this, the display name and tab-list name reset to the real username every time the
 * player reconnects. The lookup runs on the database thread; the result is applied back on the
 * player's own scheduler so the Bukkit API is only touched from the correct thread.</p>
 */
public final class NicknameListener implements Listener {

    /** Parses MiniMessage tags into Adventure components. */
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    /** The owning plugin, used for scheduling and logging. */
    private final @NotNull Plugin plugin;

    /** Where nicknames are persisted. */
    private final @NotNull NicknameRepository repository;

    /**
     * Creates the listener.
     *
     * @param plugin     the owning plugin
     * @param repository the nickname store to read from
     */
    public NicknameListener(@NotNull Plugin plugin, @NotNull NicknameRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
    }

    /**
     * Looks up the joining player's nickname and, if one is saved, applies it to their
     * display name and tab-list name.
     *
     * @param event the join event
     */
    @EventHandler
    public void onJoin(@NotNull PlayerJoinEvent event) {
        Player player = event.getPlayer();
        repository.find(player.getUniqueId()).whenComplete((nickname, error) -> {
            if (error != null) {
                plugin.getLogger().log(Level.WARNING, "Could not load nickname for " + player.getName(), error);
                return;
            }
            if (nickname.isEmpty()) {
                return;
            }
            Component component = MINI_MESSAGE.deserialize(nickname.get());
            // The retired callback runs only if the player is still online.
            player.getScheduler().run(plugin, task -> {
                player.displayName(component);
                player.playerListName(component);
            }, null);
        });
    }
}
