package net.honeyberries.nickNames;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Brigadier definition of {@code /unnick}.
 *
 * <p>Resets the executing player's display name and tab-list name back to their
 * real username.</p>
 */
public final class UnnickCommand {

    private UnnickCommand() {
    }

    /**
     * Builds the {@code /unnick} command tree.
     *
     * @return the root literal node, ready to be passed to a {@code Commands} registrar
     */
    public static @NotNull LiteralCommandNode<CommandSourceStack> create() {
        return Commands.literal("unnick")
                .executes(UnnickCommand::removeNickname)
                .build();
    }

    /**
     * Executed for {@code /unnick}; clears the player's nickname.
     *
     * @param context the Brigadier command context
     * @return {@link Command#SINGLE_SUCCESS}
     */
    private static int removeNickname(@NotNull CommandContext<CommandSourceStack> context) {
        Player player = requirePlayer(context);
        if (player == null) {
            return Command.SINGLE_SUCCESS;
        }

        Component realName = Component.text(player.getName());
        player.displayName(realName);
        player.playerListName(realName);
        player.sendMessage(Component.text("Your nickname has been removed.", NamedTextColor.YELLOW));
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Resolves the command's executor as a {@link Player}. If the executor is not a
     * player, they are told the command is player-only.
     *
     * @param context the Brigadier command context
     * @return the executing player, or {@code null} if the executor is not a player
     */
    private static @Nullable Player requirePlayer(@NotNull CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        CommandSender executor = source.getExecutor() != null ? source.getExecutor() : source.getSender();
        if (executor instanceof Player player) {
            return player;
        }
        source.getSender().sendMessage(Component.text("This command is for players only."));
        return null;
    }
}
