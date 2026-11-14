package net.honeyberries.nickNames;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Brigadier definition of {@code /nick <nickname>}.
 *
 * <p>Sets the executing player's display name and tab-list name. The nickname is a
 * greedy string argument, so it may contain spaces, and is parsed with
 * {@link MiniMessage} so tags such as {@code <red>} or {@code <gradient:red:blue>}
 * are turned into formatting.</p>
 *
 * <p>Running {@code /nick} with no argument prints the usage message.</p>
 */
public final class NickCommand {

    /** Name of the greedy string argument holding the nickname. */
    private static final String NICKNAME_ARGUMENT = "nickname";

    /** Parses MiniMessage tags into Adventure components. */
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private NickCommand() {
    }

    /**
     * Builds the {@code /nick} command tree.
     *
     * @return the root literal node, ready to be passed to a {@code Commands} registrar
     */
    public static @NotNull LiteralCommandNode<CommandSourceStack> create() {
        return Commands.literal("nick")
                .executes(NickCommand::sendUsage)
                .then(Commands.argument(NICKNAME_ARGUMENT, StringArgumentType.greedyString())
                        .executes(NickCommand::setNickname))
                .build();
    }

    /**
     * Executed for a bare {@code /nick}; tells the player how to use the command.
     *
     * @param context the Brigadier command context
     * @return {@link Command#SINGLE_SUCCESS}
     */
    private static int sendUsage(@NotNull CommandContext<CommandSourceStack> context) {
        Player player = requirePlayer(context);
        if (player != null) {
            player.sendMessage(Component.text("Usage: /nick <nickname>", NamedTextColor.RED));
        }
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Executed for {@code /nick <nickname>}; applies the nickname to the player.
     *
     * @param context the Brigadier command context
     * @return {@link Command#SINGLE_SUCCESS}
     */
    private static int setNickname(@NotNull CommandContext<CommandSourceStack> context) {
        Player player = NickCommand.requirePlayer(context);
        if (player == null) {
            return Command.SINGLE_SUCCESS;
        }

        String raw = StringArgumentType.getString(context, NICKNAME_ARGUMENT).trim();
        Component nickname = MINI_MESSAGE.deserialize(raw);

        // Change the player's nickname
        player.displayName(nickname);
        player.playerListName(nickname);

        player.sendMessage(Component.text("Your nickname has been set to ", NamedTextColor.GREEN)
                .append(nickname));
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Resolves the command's executor as a {@link Player}. If the executor is not a
     * player, they are told the command is player-only.
     *
     * @param context the Brigadier command context
     * @return the executing player, or {@code null} if the executor is not a player
     */
    public static @Nullable Player requirePlayer(@NotNull CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        CommandSender executor = source.getExecutor() != null ? source.getExecutor() : source.getSender();
        if (executor instanceof Player player) {
            return player;
        }
        source.getSender().sendMessage(Component.text("This command is for players only."));
        return null;
    }
}
