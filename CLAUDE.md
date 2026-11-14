# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build

Paper plugin (Paper API 26.3, JDK 25, Maven). No tests or linter are configured.

```
mvn package        # shaded jar -> target/NickNames-<version>.jar
```

The SQLite JDBC driver is bundled into the jar via maven-shade-plugin. `plugin.yml` uses `${version}` from `pom.xml` (resource filtering). CI is in `.github/workflows/` (`build.yml`, `publish.yml`).

## Architecture

Small plugin in `net.honeyberries.nickNames`; the pieces only make sense together:

- **`NickNames`** (main class) opens `NicknameRepository` (`<datafolder>/nicknames.db`), registers `NicknameListener`, and registers `/nick` and `/unnick` via Paper's Brigadier API in the `LifecycleEvents.COMMANDS` handler. Commands are deliberately **not** declared in `plugin.yml`. If the DB can't be opened, the plugin disables itself.
- **`NickCommand` / `UnnickCommand`** build Brigadier `LiteralCommandNode`s. `NickCommand.requirePlayer` is shared by both. The nickname is a `greedyString`, parsed as MiniMessage, applied immediately to `displayName` and `playerListName`, then persisted asynchronously. The raw MiniMessage string is what's stored, not the rendered component.
- **`NicknameRepository`** is SQLite storage (table `nicknames(uuid, nickname)`, keyed by UUID). All DB work runs on a single dedicated daemon thread (`NickNames-DB`) that owns the only connection. Every method returns a `CompletableFuture`; `SQLException`s surface as `IllegalStateException` wrapped in a failed future. Callers must handle failures in `whenComplete` and must not block the server thread on these futures.
- **`NicknameListener`** re-applies the saved nickname on `PlayerJoinEvent`. The DB lookup completes off-thread, so the result is applied via `player.getScheduler().run(...)` (Folia-style entity scheduler) rather than touching the Bukkit API from the DB thread.

Both commands are player-only and have no permission nodes yet.
