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
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;

/**
 * Brigadier definition of {@code /nick <nickname>}.
 *
 * <p>Sets the executing player's display name and tab-list name. The nickname is a
 * greedy string argument, so it may contain spaces, and is parsed with
 * {@link MiniMessage} so tags such as {@code <red>} or {@code <gradient:red:blue>}
 * are turned into formatting.</p>
 *
 * <p>The raw MiniMessage text is saved to the {@link NicknameRepository} so the nickname
 * is restored when the player rejoins (see {@link NicknameListener}).</p>
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
     * @param plugin     the owning plugin, used for logging save failures
     * @param repository the store the nickname is persisted to
     * @return the root literal node, ready to be passed to a {@code Commands} registrar
     */
    public static @NotNull LiteralCommandNode<CommandSourceStack> create(
            @NotNull Plugin plugin, @NotNull NicknameRepository repository) {
        return Commands.literal("nick")
                .executes(NickCommand::sendUsage)
                .then(Commands.argument(NICKNAME_ARGUMENT, StringArgumentType.greedyString())
                        .executes(context -> setNickname(context, plugin, repository)))
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
     * Executed for {@code /nick <nickname>}; applies the nickname to the player immediately
     * and saves it in the background so it persists across logins.
     *
     * @param context    the Brigadier command context
     * @param plugin     the owning plugin, used for logging save failures
     * @param repository the store the nickname is persisted to
     * @return {@link Command#SINGLE_SUCCESS}
     */
    private static int setNickname(@NotNull CommandContext<CommandSourceStack> context,
                                   @NotNull Plugin plugin, @NotNull NicknameRepository repository) {
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

        // Persist so the nickname is restored on the next login
        repository.save(player.getUniqueId(), raw).whenComplete((ignored, error) -> {
            if (error != null) {
                plugin.getLogger().log(Level.WARNING, "Could not save nickname for " + player.getName(), error);
                player.sendMessage(Component.text(
                        "Your nickname was set, but could not be saved and will reset when you log out.",
                        NamedTextColor.RED));
            }
        });
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
