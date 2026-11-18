package net.honeyberries.nickNames;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

/**
 * Brigadier definition of {@code /unnick}.
 *
 * <p>Resets the executing player's display name and tab-list name back to their
 * real username and deletes the saved nickname from the {@link NicknameRepository},
 * so it is not restored on the next login.</p>
 */
public final class UnnickCommand {

    private UnnickCommand() {
    }

    /**
     * Builds the {@code /unnick} command tree.
     *
     * @param plugin     the owning plugin, used for logging delete failures
     * @param repository the store the nickname is deleted from
     * @return the root literal node, ready to be passed to a {@code Commands} registrar
     */
    public static @NotNull LiteralCommandNode<CommandSourceStack> create(
            @NotNull Plugin plugin, @NotNull NicknameRepository repository) {
        return Commands.literal("unnick")
                .executes(context -> removeNickname(context, plugin, repository))
                .build();
    }

    /**
     * Executed for {@code /unnick}; clears the player's nickname and removes it from storage.
     *
     * @param context    the Brigadier command context
     * @param plugin     the owning plugin, used for logging delete failures
     * @param repository the store the nickname is deleted from
     * @return {@link Command#SINGLE_SUCCESS}
     */
    private static int removeNickname(@NotNull CommandContext<CommandSourceStack> context,
                                      @NotNull Plugin plugin, @NotNull NicknameRepository repository) {
        Player player = NickCommand.requirePlayer(context);
        if (player == null) {
            return Command.SINGLE_SUCCESS;
        }

        Component realName = Component.text(player.getName());
        player.displayName(realName);
        player.playerListName(realName);
        player.sendMessage(Component.text("Your nickname has been removed.", NamedTextColor.YELLOW));

        repository.delete(player.getUniqueId()).whenComplete((ignored, error) -> {
            if (error != null) {
                plugin.getLogger().log(Level.WARNING, "Could not delete nickname for " + player.getName(), error);
                // The future completes on the DB thread; messaging must happen on the player's region.
                player.getScheduler().run(plugin, task -> player.sendMessage(Component.text(
                        "Your nickname was removed, but the saved copy could not be deleted and may return when you log in.",
                        NamedTextColor.RED)), null);
            }
        });
        return Command.SINGLE_SUCCESS;
    }
}
