package net.honeyberries.nickNames;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * NickNames main plugin class.
 *
 * <p>Registers the {@code /nick} and {@code /unnick} commands through Paper's
 * Brigadier command API during the {@link LifecycleEvents#COMMANDS} lifecycle event.
 * Because the commands are registered programmatically they do not need to be
 * declared in {@code plugin.yml}.</p>
 *
 * @author HoneyBerries
 * @author Pepmon270
 */
public final class NickNames extends JavaPlugin {

    /**
     * Registers the Brigadier commands and logs that the plugin is up.
     */
    @Override
    public void onEnable() {
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register(
                    NickCommand.create(),
                    "Set your nickname (supports MiniMessage formatting)"
            );
            event.registrar().register(
                    UnnickCommand.create(),
                    "Remove your nickname"
            );
        });

        getLogger().info("NickNames enabled!");
    }

    /**
     * Logs that the plugin has been disabled.
     */
    @Override
    public void onDisable() {
        getLogger().info("NickNames disabled!");
    }
}
