package net.honeyberries.nickNames;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * SQLite-backed nickname storage.
 *
 * <p>Each player has at most one nickname, stored as the raw MiniMessage string the player typed
 * (for example {@code <red>Berry}), keyed by their UUID so it survives name changes.</p>
 *
 * <p>All database work runs on a single dedicated thread that owns the only connection, so every
 * operation is atomic with respect to the others and the server thread is never blocked. Every
 * method returns a future that completes on that thread; if the underlying SQL fails, the future
 * completes exceptionally with an {@link IllegalStateException} wrapping the {@link SQLException}.</p>
 */
public final class NicknameRepository implements AutoCloseable {

    /** Single daemon thread that owns the database connection and runs every database operation. */
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "NickNames-DB");
        thread.setDaemon(true);
        return thread;
    });

    /** The single SQLite connection; only touched from the database thread ({@link #executor}). */
    private final Connection connection;

    /**
     * Opens (creating if needed) the database and its schema.
     *
     * @param file path of the SQLite file
     * @throws SQLException if the database cannot be opened or the schema cannot be created
     */
    public NicknameRepository(@NotNull Path file) throws SQLException {
        try {
            this.connection = CompletableFuture.supplyAsync(() -> {
                try {
                    Connection c = DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath());
                    try (Statement s = c.createStatement()) {
                        s.execute("""
                                CREATE TABLE IF NOT EXISTS nicknames (
                                    uuid     TEXT NOT NULL PRIMARY KEY,
                                    nickname TEXT NOT NULL
                                ) WITHOUT ROWID""");
                    }
                    return c;
                } catch (SQLException e) {
                    throw new IllegalStateException(e);
                }
            }, executor).join();
        } catch (RuntimeException e) {
            executor.shutdownNow();
            throw e.getCause() instanceof SQLException sql ? sql : new SQLException(e);
        }
    }

    /**
     * Looks up a player's saved nickname.
     *
     * @param owner the player's UUID
     * @return future of the raw MiniMessage nickname, or empty if the player has none
     */
    public @NotNull CompletableFuture<@NotNull Optional<@NotNull String>> find(@NotNull UUID owner) {
        return run(() -> {
            try (PreparedStatement s = connection.prepareStatement("SELECT nickname FROM nicknames WHERE uuid = ?")) {
                s.setString(1, owner.toString());
                try (ResultSet rs = s.executeQuery()) {
                    return rs.next() ? Optional.of(rs.getString("nickname")) : Optional.empty();
                }
            }
        });
    }

    /**
     * Saves a player's nickname, replacing any existing one.
     *
     * @param owner    the player's UUID
     * @param nickname the raw MiniMessage nickname to store
     * @return future that completes once the nickname has been written
     */
    public @NotNull CompletableFuture<Void> save(@NotNull UUID owner, @NotNull String nickname) {
        return run(() -> {
            try (PreparedStatement s = connection.prepareStatement(
                    "INSERT OR REPLACE INTO nicknames (uuid, nickname) VALUES (?, ?)")) {
                s.setString(1, owner.toString());
                s.setString(2, nickname);
                s.executeUpdate();
            }
            return null;
        });
    }

    /**
     * Removes a player's saved nickname.
     *
     * @param owner the player's UUID
     * @return future of {@code true} if a nickname was deleted, {@code false} if the player had none
     */
    public @NotNull CompletableFuture<@NotNull Boolean> delete(@NotNull UUID owner) {
        return run(() -> {
            try (PreparedStatement s = connection.prepareStatement("DELETE FROM nicknames WHERE uuid = ?")) {
                s.setString(1, owner.toString());
                return s.executeUpdate() > 0;
            }
        });
    }

    /** Finishes queued operations, then closes the connection. Blocks for at most a few seconds. */
    @Override
    public void close() {
        executor.execute(() -> {
            try {
                connection.close();
            } catch (SQLException ignored) {
                // Shutting down; nothing useful to do.
            }
        });
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * A database task that may throw {@link SQLException}.
     *
     * @param <T> the result type of the task
     */
    @FunctionalInterface
    private interface Task<T> {
        /**
         * Performs the database work. Runs on the database thread.
         *
         * @return the task's result
         * @throws SQLException if the database operation fails
         */
        T run() throws SQLException;
    }

    /**
     * Runs a database task on the dedicated database thread. A {@link SQLException} thrown by the task
     * is wrapped in an {@link IllegalStateException}, so the returned future completes exceptionally.
     *
     * @param task the task to run
     * @param <T>  the result type of the task
     * @return future of the task's result
     */
    private <T> @NotNull CompletableFuture<T> run(@NotNull Task<T> task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.run();
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        }, executor);
    }
}
