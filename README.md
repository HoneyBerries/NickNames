# NickNames

Custom, [MiniMessage](https://docs.advntr.dev/minimessage/format.html)-formatted nicknames for [Paper](https://papermc.io) 26.3. Nicknames are stored in SQLite, so they survive logging out and server restarts.

| Command             | Description                                       |
|---------------------|---------------------------------------------------|
| `/nick <nickname>`  | Set your nickname (spaces and MiniMessage allowed). |
| `/unnick`           | Remove your nickname and go back to your real name. |

Both commands are player-only and currently available to everyone (there are no permission nodes yet).

## Formatting

The nickname is parsed as MiniMessage and shown in chat and the tab list:

```
/nick <red>Berry
/nick <gradient:red:blue>Honey Berries</gradient>
/nick <rainbow>Pepmon
/nick <#ff8800>Orange
```

## Storage

Nicknames live in `plugins/NickNames/nicknames.db`, keyed by player UUID, so they keep working if a player changes their Minecraft username. The raw MiniMessage text is saved and re-applied when the player joins. All database work runs on its own thread, so it never blocks the server. The SQLite driver is bundled in the jar.

## Build

Requires JDK 25 and Maven:

```
mvn package
```

The jar is written to `target/NickNames-<version>.jar`.

## Authors

HoneyBerries and Pepmon270 - [honeyberries.net](https://honeyberries.net)
